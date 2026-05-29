import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import TextField from '../components/form/TextField'
import '../components/form/BaseField/BaseField.scss'

const initialForm = { nome: '' }

export default function CategoriasDocumentos() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const { can } = useAuth()
  const canManage = can('GESTAO_DOCUMENTOS')

  const load = async (nome) => {
    try {
      let path = '/api/categorias-documentos'
      const params = []
      if (nome) params.push('nome=' + encodeURIComponent(nome))
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const timer = setTimeout(() => {
      load(filtroNome)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroNome])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFieldErrors({})
    setSuccess('')
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({ nome: item.nome || '' })
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
    if (!form.nome.trim()) erros.nome = 'Nome da categoria é obrigatório'
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
        const payload = { nome: form.nome }

        if (editing) {
          await put('/api/categorias-documentos/' + editing, payload)
          setSuccess('Categoria alterada com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/categorias-documentos', payload)
          setSuccess('Categoria cadastrada com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroNome)
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
        await del('/api/categorias-documentos/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        setSuccess('Categoria excluída com sucesso.')
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
      <PageHeader title="Categorias de Documentos" subtitle="Gerencie as categorias dos documentos da igreja" actionLabel={canManage ? 'Adicionar Categoria' : ''} onAction={canManage ? openNew : null} />

      <section className="filter-bar">
        <TextField
          placeholder="Filtrar por nome..."
          value={filtroNome}
          setValue={setFiltroNome}
        />
      </section>

      {success && <div className="message success">{success}</div>}

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Categoria' : 'Nova Categoria'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>
            <TextField
              label="Nome"
              required
              value={form.nome}
              setValue={value => handleFieldChange('nome', value)}
              placeholder="Nome da categoria"
              error={fieldErrors.nome}
            />
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Categoria'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
            </div>
            <div className="card-actions">
              {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
