import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'

const initialForm = { nome: '' }

export default function CategoriasEventos() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [filtroNome, setFiltroNome] = useState('')

  const load = async (nome) => {
    try {
      let path = '/api/categorias-eventos'
      const params = []

      if (nome) {
        params.push('nome=' + encodeURIComponent(nome))
      }

      if (params.length) {
        path += '?' + params.join('&')
      }

      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => {
    load()
  }, [])

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
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || ''
    })
    setEditing(item.id)
    setErro('')
    setFormOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      if (!form.nome) {
        throw new Error('Nome é obrigatório')
      }

      const payload = {
        nome: form.nome
      }

      if (editing) {
        await put('/api/categorias-eventos/' + editing, payload)
      } else {
        await post('/api/categorias-eventos', payload)
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
        await del('/api/categorias-eventos/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader
        title="Categorias de Eventos"
        subtitle="Gerencie as categorias dos eventos da igreja"
        actionLabel="Nova Categoria"
        onAction={openNew}
      />

      <section className="filter-bar">
        <input
          placeholder="Filtrar por nome..."
          value={filtroNome}
          onChange={e => setFiltroNome(e.target.value)}
        />
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Categoria' : 'Nova Categoria De Evento'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}>
              <Icon name="close" size={16} />
            </button>
          </div>

          {erro && <div className="message inline">{erro}</div>}

          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Nome</span>
              <input
                value={form.nome}
                onChange={e =>
                  setForm(prev => ({ ...prev, nome: e.target.value }))
                }
                placeholder="Nome da categoria"
              />
            </label>

            <div className="form-submit">
              <button className="danger-action">
                {editing ? 'Salvar Alterações' : 'Salvar Categoria'}
              </button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map((item, index) => (
          <article
            className={'finance-card ' + (index % 2 ? 'accent-red' : '')}
            key={item.id}
          >
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                <span>ID: {item.id}</span>
              </div>
            </div>

            <div className="card-actions">
              <button
                className="icon-button"
                onClick={() => openEdit(item)}
                title="Editar"
              >
                <Icon name="edit" size={16} />
              </button>

              <button
                className="icon-button icon-button--danger"
                onClick={() => remove(item)}
                title="Excluir"
              >
                <Icon name="trash" size={16} />
              </button>
            </div>
          </article>
        ))}
      </div>
    </>
  )
}