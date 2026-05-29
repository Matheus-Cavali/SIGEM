import { useEffect, useState } from 'react'
import { del, get, post, postForm, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import DateField from '../components/form/DateField'
import BaseField from '../components/form/BaseField'
import TextField from '../components/form/TextField'
import '../components/form/BaseField/BaseField.scss'

const initialForm = {
  titulo: '',
  arquivo: null,
  caminhoArquivo: '',
  dataDocumento: '',
  categoriaDocumentoId: '',
}

function arquivoUrl(caminho) {
  if (!caminho) return ''
  if (caminho.startsWith('http')) return caminho
  return 'http://localhost:8080' + caminho
}

export default function Documentos() {
  const [items, setItems] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroTitulo, setFiltroTitulo] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const { can } = useAuth()
  const canManage = can('GESTAO_DOACOES')

  const loadCategorias = async () => {
    try {
      const data = await get('/api/categorias-documentos')
      setCategorias(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const load = async (titulo, categoriaId) => {
    try {
      let path = '/api/documentos'
      const params = []
      if (titulo) params.push('titulo=' + encodeURIComponent(titulo))
      if (categoriaId) params.push('categoriaDocumentoId=' + categoriaId)
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load(); loadCategorias() }, [])

  useEffect(() => {
    const timer = setTimeout(() => {
      load(filtroTitulo, filtroCategoria || null)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroTitulo, filtroCategoria])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setSuccess('')
    setFieldErrors({})
    setFormOpen(true)
  }

  const openEdit = async (item) => {
    setErro('')
    setSuccess('')
    setFieldErrors({})

    try {
      const data = await get('/api/documentos/' + item.id)
      setForm({
        titulo: data.titulo || '',
        arquivo: null,
        caminhoArquivo: data.caminhoArquivo || '',
        dataDocumento: data.dataDocumento || '',
        categoriaDocumentoId: String(data.categoriaDocumentoId || ''),
      })
      setEditing(item.id)
      setFormOpen(true)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) {
      setErro(error.message)
    }
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.titulo.trim()) erros.titulo = 'Título do documento é obrigatório'
    if (!form.caminhoArquivo && !form.arquivo) erros.arquivo = 'Arquivo do documento é obrigatório'
    if (!form.dataDocumento.trim()) erros.dataDocumento = 'Data do documento é obrigatória'
    if (!form.categoriaDocumentoId) erros.categoriaDocumentoId = 'Categoria do documento é obrigatória'
    return erros
  }

  const uploadArquivo = async () => {
    if (!form.arquivo) return form.caminhoArquivo

    const formData = new FormData()
    formData.append('arquivo', form.arquivo)
    const data = await postForm('/api/documentos/upload', formData)
    return data && data.caminhoArquivo ? data.caminhoArquivo : form.caminhoArquivo
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')
    setSuccess('')
    setFieldErrors({})

    const campos = validarCampos()
    const temErros = Object.keys(campos).length > 0
    if (temErros) {
      setFieldErrors(campos)
    }

    if (!temErros) {
      try {
        const caminhoArquivo = await uploadArquivo()
        const payload = {
          titulo: form.titulo,
          caminhoArquivo,
          dataDocumento: form.dataDocumento,
          categoriaDocumentoId: form.categoriaDocumentoId ? parseInt(form.categoriaDocumentoId, 10) : null,
        }

        if (editing) {
          await put('/api/documentos/' + editing, payload)
          setSuccess('Documento alterado com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/documentos', payload)
          setSuccess('Documento cadastrado com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroTitulo, filtroCategoria || null)
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
      }
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir documento "' + item.titulo + '"?')

    if (confirmed) {
      try {
        await del('/api/documentos/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        setSuccess('Documento excluído com sucesso.')
      } catch (error) {
        alert(error.message)
      }
    }
  }

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(timer)
  }, [success])

  return (
    <>
      <PageHeader title="Documentos" subtitle="Cadastre e acompanhe documentos da igreja" actionLabel={canManage ? 'Adicionar Documento' : ''} onAction={canManage ? openNew : null} />

      <section className="filter-bar">
        <TextField
          placeholder="Filtrar por título..."
          value={filtroTitulo}
          setValue={setFiltroTitulo}
        />
        <BaseField>
          <select className="field-control" value={filtroCategoria} onChange={e => setFiltroCategoria(e.target.value)}>
            <option value="">Todas as categorias</option>
            {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </BaseField>
      </section>

      {success && <div className="message success">{success}</div>}

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Documento' : 'Novo Documento'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>
            <TextField
              label="Título"
              required
              value={form.titulo}
              setValue={value => handleFieldChange('titulo', value)}
              placeholder="Título do documento"
              error={fieldErrors.titulo}
            />

            <BaseField label="Arquivo" required error={fieldErrors.arquivo}>
              <input
                className="field-control"
                type="file"
                onChange={e => handleFieldChange('arquivo', e.target.files[0] || null)}
              />
              {form.caminhoArquivo && !form.arquivo && <span className="field-help">{form.caminhoArquivo}</span>}
            </BaseField>

            <DateField label="Data do Documento" value={form.dataDocumento} setValue={v => handleFieldChange('dataDocumento', v)} required error={fieldErrors.dataDocumento} />

            <BaseField label="Categoria" required error={fieldErrors.categoriaDocumentoId}>
              <select className="field-control" value={form.categoriaDocumentoId} onChange={e => handleFieldChange('categoriaDocumentoId', e.target.value)}>
                <option value="">Selecione...</option>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
            </BaseField>

            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Documento'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.titulo}</h3>
              <div className="meta-row">
                <span><Icon name="calendar" size={15} /> {item.dataDocumento}</span>
                {item.categoriaDocumentoNome && <span className="tag">{item.categoriaDocumentoNome}</span>}
                {item.colaboradorLancouNome && <span>Lançado por: {item.colaboradorLancouNome}</span>}
                {item.caminhoArquivo && <a href={arquivoUrl(item.caminhoArquivo)} target="_blank" rel="noreferrer">Abrir arquivo</a>}
              </div>
            </div>
            <div className="card-actions">
              {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
        {!items.length && <p className="empty-state">Nenhum documento encontrado.</p>}
      </div>
    </>
  )
}
