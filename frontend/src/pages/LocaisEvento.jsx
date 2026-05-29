import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { toast } from 'react-toastify'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import '../components/Modal/Modal.scss'

const initialForm = {
  nome: '',
  endereco: ''
}

export default function LocaisEventos() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)

  const { can } = useAuth()
  const canManage = can('GESTAO_EVENTOS')

  const load = async (nome) => {
    try {
      let path = '/api/locais-evento'
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
      toast.error(error.message)
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
    setFieldErrors({})
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
      endereco: item.endereco || ''
    })

    setEditing(item.id)
    setFieldErrors({})
    setFormOpen(true)

    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const validarCampos = () => {
    const erros = {}

    if (!form.nome.trim()) {
      erros.nome = 'Nome do local é obrigatório'
    }

    return erros
  }

  const save = async (event) => {
    event.preventDefault()

    setFieldErrors({})

    const campos = validarCampos()
    const temErros = Object.keys(campos).length > 0

    if (temErros) {
      setFieldErrors(campos)
    }

    if (!temErros) {
      try {
        const payload = {
          nome: form.nome,
          endereco: form.endereco || null
        }

        if (editing) {
          await put('/api/locais-evento/' + editing, payload)
          toast.success('Local alterado com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/locais-evento', payload)
          toast.success('Local cadastrado com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroNome)
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          toast.error(error.message)
        }
      }
    }
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/locais-evento/' + confirmDelete.id)
      setItems(prev => prev.filter(current => current.id !== confirmDelete.id))
      toast.success('Local excluído com sucesso.')
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

  return (
    <>
      <PageHeader
        title="Locais de Eventos"
        subtitle="Gerencie os locais dos eventos da igreja"
        actionLabel={canManage ? 'Novo Local' : ''}
        onAction={canManage ? openNew : null}
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
            <h2>
              {editing ? 'Editar Local' : 'Novo Local de Evento'}
            </h2>

            <button
              className="ghost-icon"
              onClick={() => setFormOpen(false)}
            >
              <Icon name="close" size={16} />
            </button>
          </div>

          <form
            className="inline-form"
            onSubmit={save}
            noValidate
          >
            <div className="field-container">
              <label className="field-label">
                <span>
                  Nome <span className="required-star">*</span>
                </span>
              </label>

              <input
                className={
                  'field-control' +
                  (fieldErrors.nome ? ' is-invalid' : '')
                }
                value={form.nome}
                onChange={e =>
                  handleFieldChange('nome', e.target.value)
                }
                placeholder="Nome do local"
              />

              {fieldErrors.nome && (
                <span className="field-error">
                  {fieldErrors.nome}
                </span>
              )}
            </div>

            <div className="field-container">
              <label className="field-label">
                Endereço
              </label>

              <input
                className="field-control"
                value={form.endereco}
                onChange={e =>
                  handleFieldChange('endereco', e.target.value)
                }
                placeholder="Endereço do local"
              />
            </div>

            <div className="form-submit">
              <button className="primary-action">
                {editing
                  ? 'Salvar Alterações'
                  : 'Salvar Local'}
              </button>
            </div>
          </form>
        </section>
      )}

      <Modal
        open={!!confirmDelete}
        title="Confirmar exclusão"
        variant="error"
        hideCloseButton
        onClose={() => setConfirmDelete(null)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setConfirmDelete(null)}>Cancelar</button>
            <button className="danger-action" onClick={handleConfirmDelete}>Excluir</button>
          </>
        }
      >
        <p>Excluir local "{confirmDelete?.nome}"?</p>
      </Modal>

      <div className="cards-list">
        {items.map((item, index) => (
          <article
            className={
              'finance-card ' +
              (index % 2 ? 'accent-red' : '')
            }
            key={item.id}
          >
            <div>
              <h3>{item.nome}</h3>

              <div className="meta-row">
                <span>{item.endereco || '-'}</span>
              </div>
            </div>

            <div className="card-actions">
              {canManage && (
                <button
                  className="icon-button"
                  onClick={() => openEdit(item)}
                  title="Editar"
                >
                  <Icon name="edit" size={16} />
                </button>
              )}

              {canManage && (
                <button
                  className="icon-button icon-button--danger"
                  onClick={() => remove(item)}
                  title="Excluir"
                >
                  <Icon name="trash" size={16} />
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}