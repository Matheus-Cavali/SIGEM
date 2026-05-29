import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { useAuth } from '../state/AuthContext'

const initialForm = { nome: '' }

export default function CategoriasDespesa() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [filtroNome, setFiltroNome] = useState('')
  const { can } = useAuth()
  const canManage = can('GERENCIAR_DESPESA')

  const load = async (nome) => {
    try {
      let path = '/api/categorias-despesa'
      if (nome) path += '?nome=' + encodeURIComponent(nome)
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const timer = setTimeout(() => { load(filtroNome) }, 300)
    return () => clearTimeout(timer)
  }, [filtroNome])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
    })
    setEditing(item.id)
    setErro('')
    setFormOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      if (!form.nome) throw new Error('Nome é obrigatorio')

      const payload = { nome: form.nome }

      if (editing) {
        await put('/api/categorias-despesa/' + editing, payload)
      } else {
        await post('/api/categorias-despesa', payload)
      }

      setFormOpen(false)
      setEditing(null)
      setForm(initialForm)
      load(filtroNome)
    } catch (error) {
      setErro(error.message)
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir "' + item.nome + '"?')

    if (confirmed) {
      try {
        await del('/api/categorias-despesa/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader title="Categorias de Despesa" subtitle="Gerencie as categorias de despesa da igreja" actionLabel={canManage ? 'Adicionar Categoria' : ''} onAction={openNew} />

      <section className="filter-bar">
        <input placeholder="Filtrar por nome..." value={filtroNome} onChange={e => setFiltroNome(e.target.value)} />
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Categoria de Despesa' : 'Nova Categoria de Despesa'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Nome</span>
              <input value={form.nome} onChange={e => setForm(prev => ({ ...prev, nome: e.target.value }))} placeholder="Nome da categoria" />
            </label>
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
