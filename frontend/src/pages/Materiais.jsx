import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import '../components/form/BaseField/BaseField.scss'

const initialForm = { nome: '', descricao: '', quantidadeEstoque: '', categoriaMaterialId: '' }

export default function Materiais() {
  const [items, setItems] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const { can } = useAuth()
  const canManage = can('GESTAO_DOACOES')

  const loadCategorias = async () => {
    try {
      const data = await get('/api/categorias-materiais')
      setCategorias(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const load = async (nome, categoriaId) => {
    try {
      let path = '/api/materiais'
      const params = []
      if (nome) params.push('nome=' + encodeURIComponent(nome))
      if (categoriaId) params.push('categoriaId=' + categoriaId)
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
      load(filtroNome, filtroCategoria || null)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroNome, filtroCategoria])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFieldErrors({})
    setSuccess('')
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
      descricao: item.descricao || '',
      quantidadeEstoque: String(item.quantidadeEstoque ?? ''),
      categoriaMaterialId: String(item.categoriaMaterialId ?? ''),
    })
    setEditing(item.id)
    setErro('')
    setFieldErrors({})
    setSuccess('')
    setFormOpen(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.nome.trim()) erros.nome = 'Nome do material é obrigatório'
    if (!form.descricao.trim()) erros.descricao = 'Descrição do material é obrigatória'
    if (form.quantidadeEstoque === '') {
      erros.quantidadeEstoque = 'Quantidade em estoque é obrigatória'
    } else if (parseInt(form.quantidadeEstoque, 10) < 0) {
      erros.quantidadeEstoque = 'Quantidade em estoque não pode ser negativa'
    }
    if (!form.categoriaMaterialId) erros.categoriaMaterialId = 'Categoria é obrigatória'
    return erros
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
        const quantidade = parseInt(form.quantidadeEstoque, 10)

        const payload = {
          nome: form.nome,
          descricao: form.descricao,
          quantidadeEstoque: quantidade,
          categoriaMaterialId: form.categoriaMaterialId ? parseInt(form.categoriaMaterialId, 10) : null,
        }

        if (editing) {
          await put('/api/materiais/' + editing, payload)
          setSuccess('Material alterado com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/materiais', payload)
          setSuccess('Material cadastrado com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroNome, filtroCategoria || null)
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
    const confirmed = window.confirm('Excluir "' + item.nome + '"?')

    if (confirmed) {
      try {
        await del('/api/materiais/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        setSuccess('Material excluído com sucesso.')
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
      <PageHeader title="Materiais" subtitle="Controle materiais e recursos fisicos da igreja" actionLabel={canManage ? 'Adicionar Material' : ''} onAction={canManage ? openNew : null} />

      <section className="filter-bar">
        <input
          placeholder="Filtrar por nome..."
          value={filtroNome}
          onChange={e => setFiltroNome(e.target.value)}
        />
        <select
          value={filtroCategoria}
          onChange={e => setFiltroCategoria(e.target.value)}
        >
          <option value="">Todas as categorias</option>
          {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
      </section>

      {success && <div className="message success">{success}</div>}

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Material' : 'Novo Material'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>
            <div className="field-container">
              <label className="field-label"><span>Nome <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.nome ? ' is-invalid' : '')}
                     value={form.nome} onChange={e => handleFieldChange('nome', e.target.value)} placeholder="Nome do material" />
              {fieldErrors.nome && <span className="field-error">{fieldErrors.nome}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Descrição <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.descricao ? ' is-invalid' : '')}
                     value={form.descricao} onChange={e => handleFieldChange('descricao', e.target.value)} placeholder="Descricao do material" />
              {fieldErrors.descricao && <span className="field-error">{fieldErrors.descricao}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Quantidade em Estoque <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.quantidadeEstoque ? ' is-invalid' : '')}
                     value={form.quantidadeEstoque} onChange={e => handleFieldChange('quantidadeEstoque', e.target.value)} placeholder="0" type="number" />
              {fieldErrors.quantidadeEstoque && <span className="field-error">{fieldErrors.quantidadeEstoque}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Categoria <span className="required-star">*</span></span></label>
              <select className={'field-control' + (fieldErrors.categoriaMaterialId ? ' is-invalid' : '')}
                      value={form.categoriaMaterialId} onChange={e => handleFieldChange('categoriaMaterialId', e.target.value)}>
                <option value="">Selecione...</option>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
              {fieldErrors.categoriaMaterialId && <span className="field-error">{fieldErrors.categoriaMaterialId}</span>}
            </div>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Material'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map((item, index) => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                {item.descricao && <span><Icon name="box" size={14} /> <strong>Descrição:</strong> {item.descricao}</span>}
                <span><strong>Estoque:</strong> {item.quantidadeEstoque}</span>
                {item.categoriaMaterialId && <span><strong>Categoria:</strong> {categorias.find(c => c.id === item.categoriaMaterialId)?.nome || item.categoriaMaterialId}</span>}
              </div>
            </div>
            <div className="card-actions">
              {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
        {items.length === 0 && (
          <p className="empty-state">Nenhum material encontrado.</p>
        )}
      </div>
    </>
  )
}
