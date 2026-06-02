import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { toast } from 'react-toastify'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { useAuth } from '../state/AuthContext'
import Modal from '../components/Modal'
import '../components/Modal/Modal.scss'

const initialForm = { nome: '' }

export default function CategoriasDespesa() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [filtroNome, setFiltroNome] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)
  const { can } = useAuth()
  const podeGerenciar = can('GESTAO_DESPESAS')

  const load = async (nome) => {
    try {
      let path = '/api/categorias-despesa'
      if (nome) path += '?nome=' + encodeURIComponent(nome)
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      toast.error(error.message)
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
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
    })
    setEditing(item.id)
    setFormOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()

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
      toast.error(error.message)
    }
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/categorias-despesa/' + confirmDelete.id)
      setItems(prev => prev.filter(current => current.id !== confirmDelete.id))
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

  return (
    <>
      <PageHeader title="Categorias de Despesa" subtitle="Gerencie as categorias de despesa da igreja" actionLabel={podeGerenciar ? 'Adicionar Categoria' : ''} onAction={openNew} />

      <section className="filter-bar">
        <input placeholder="Filtrar por nome..." value={filtroNome} onChange={e => setFiltroNome(e.target.value)} />
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Categoria de Despesa' : 'Nova Categoria de Despesa'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
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
        <p>Excluir categoria "{confirmDelete?.nome}"?</p>
      </Modal>

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
            </div>
            <div className="card-actions">
              {podeGerenciar && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {podeGerenciar && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
