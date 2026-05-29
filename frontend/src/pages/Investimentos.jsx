import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { toast } from 'react-toastify'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { dataParaBackend, moeda, valorParaNumero } from '../utils/format'
import Modal from '../components/Modal'
import '../components/Modal/Modal.scss'
import CurrencyField from '../components/form/CurrencyField'
import DateField from '../components/form/DateField'
import { useAuth } from '../state/AuthContext'

function hojeISO() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const initialForm = { nome: '', valorMeta: '0,00', dataAbertura: '', status: 'ABERTO' }

export default function Investimentos() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)
  const { user, can } = useAuth()
  const canManage = can('REGISTRAR_INVESTIMENTO')

  const load = async (nome, status) => {
    try {
      let path = '/api/investimentos'
      const params = []
      if (nome) params.push('nome=' + encodeURIComponent(nome))
      if (status) params.push('status=' + status)
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      if (!error.fieldErrors) toast.error(error.message)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const timer = setTimeout(() => load(filtroNome, filtroStatus || null), 300)
    return () => clearTimeout(timer)
  }, [filtroNome, filtroStatus])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setFieldErrors({})
    setFormOpen(true)
  }

  const openEdit = async (item) => {
    setFieldErrors({})
    try {
      const data = await get('/api/investimentos/' + item.id)
      setForm({
        nome: data.nome || '',
        valorMeta: String(Math.round(Number(data.valorMeta || 0) * 100)),
        dataAbertura: data.dataAbertura || '',
        status: data.status || 'ABERTO',
      })
      setEditing(item.id)
      setFormOpen(true)
    } catch (error) {
      if (!error.fieldErrors) toast.error(error.message)
    }
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.nome.trim()) erros.nome = 'Nome do investimento é obrigatório'
    if (!form.valorMeta || !valorParaNumero(form.valorMeta)) erros.valorMeta = 'Valor meta deve ser maior que zero'
    if (!editing && !form.dataAbertura.trim()) erros.dataAbertura = 'Data de abertura é obrigatória'
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
        const value = valorParaNumero(form.valorMeta)

        if (editing) {
          await put('/api/investimentos/' + editing, { nome: form.nome, valorMeta: value, status: form.status })
        } else {
          await post('/api/investimentos', { nome: form.nome, valorMeta: value, dataAbertura: dataParaBackend(form.dataAbertura), colaboradorId: user.id })
        }

        setFormOpen(false)
        setEditing(null)
        setForm(initialForm)
        load()
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          toast.error(error.message)
        }
      }
    }
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/investimentos/' + confirmDelete.id)
      setItems(prev => prev.filter(current => current.id !== confirmDelete.id))
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

  return (
    <>
      <PageHeader title="Investimentos" subtitle="Gerencie os investimentos e metas financeiras da igreja" actionLabel={canManage ? 'Adicionar Investimento' : ''} onAction={openNew} />

      <section className="filter-bar">
        <input placeholder="Filtrar por nome..." value={filtroNome}
               onChange={e => setFiltroNome(e.target.value)} />
        <select value={filtroStatus} onChange={e => setFiltroStatus(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="ABERTO">Em andamento</option>
          <option value="ENCERRADO">Encerrado</option>
        </select>
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Investimento' : 'Novo Investimento'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          <form className="inline-form" onSubmit={save}>
            <div className="field-container">
              <label className="field-label">Nome <span className="required-star">*</span></label>
              <input className={'field-control' + (fieldErrors.nome ? ' is-invalid' : '')}
                     value={form.nome} onChange={e => handleFieldChange('nome', e.target.value)} placeholder="Nome do investimento" />
              {fieldErrors.nome && <span className="field-error">{fieldErrors.nome}</span>}
            </div>
            <CurrencyField label="Meta de Valor (R$)" value={form.valorMeta}
              setValue={v => handleFieldChange('valorMeta', v)} error={fieldErrors.valorMeta} required />
            {!editing && (
              <DateField label="Data Meta" value={form.dataAbertura} setValue={v => handleFieldChange('dataAbertura', v)} error={fieldErrors.dataAbertura} required minDate={hojeISO()} maxDate="2099-12-31" />
            )}
            {editing && (
              <div className="field-container">
                <label className="field-label">Status</label>
                <select className="field-control" value={form.status} onChange={e => handleFieldChange('status', e.target.value)}>
                  <option value="ABERTO">Em andamento</option>
                  <option value="ENCERRADO">Encerrado</option>
                </select>
              </div>
            )}
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Investimento'}</button>
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
        <p>Excluir investimento "{confirmDelete?.nome}"?</p>
      </Modal>

      <div className="cards-list">
        {items.map((item, index) => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                <span><Icon name="target" size={15} /> {moeda(item.valorMeta)}</span>
                <span><Icon name="calendar" size={15} /> {item.dataAbertura}</span>
                {item.colaboradorNome && <span><Icon name="users" size={15} /> {item.colaboradorNome}</span>}
              </div>
            </div>
            <div className="card-actions">
              <span className={'status ' + (item.status === 'ENCERRADO' ? 'closed' : '')}>{item.status === 'ENCERRADO' ? 'Encerrado' : 'Em andamento'}</span>
              {canManage && <button className="icon-button" onClick={() => openEdit(item)}><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)}><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
