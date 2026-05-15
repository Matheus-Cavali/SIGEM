import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { dataParaBackend, formatarData, moeda, valorParaNumero } from '../utils/format'
import { useAuth } from '../state/AuthContext'

const initialForm = { nome: '', valorMeta: '', dataAbertura: '', status: 'ABERTO' }

export default function Investimentos() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const { user, can } = useAuth()
  const canManage = can('REGISTRAR_INVESTIMENTO')

  const load = async () => {
    try {
      const data = await get('/api/investimentos')
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFormOpen(true)
  }

  const openEdit = async (item) => {
    setErro('')
    try {
      const data = await get('/api/investimentos/' + item.id)
      setForm({
        nome: data.nome || '',
        valorMeta: String(data.valorMeta || '').replace('.', ','),
        dataAbertura: data.dataAbertura || '',
        status: data.status || 'ABERTO',
      })
      setEditing(item.id)
      setFormOpen(true)
    } catch (error) {
      setErro(error.message)
    }
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      const value = valorParaNumero(form.valorMeta)

      if (!form.nome || !value) {
        throw new Error('Nome e valor sao obrigatorios')
      }

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
      setErro(error.message)
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir "' + item.nome + '"?')

    if (confirmed) {
      try {
        await del('/api/investimentos/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader title="Investimentos" subtitle="Gerencie os investimentos e metas financeiras da igreja" actionLabel={canManage ? 'Adicionar Investimento' : ''} onAction={openNew} />

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Investimento' : 'Novo Investimento'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Nome</span>
              <input value={form.nome} onChange={e => setForm(prev => ({ ...prev, nome: e.target.value }))} placeholder="Nome do investimento" />
            </label>
            <label>
              <span>Meta de Valor (R$)</span>
              <input value={form.valorMeta} onChange={e => setForm(prev => ({ ...prev, valorMeta: e.target.value }))} placeholder="0,00" />
            </label>
            {!editing && (
              <label>
                <span>Data Meta</span>
                <input value={form.dataAbertura} onChange={e => setForm(prev => ({ ...prev, dataAbertura: formatarData(e.target.value) }))} placeholder="dd/mm/aaaa" />
              </label>
            )}
            {editing && (
              <label>
                <span>Status</span>
                <select value={form.status} onChange={e => setForm(prev => ({ ...prev, status: e.target.value }))}>
                  <option value="ABERTO">Em andamento</option>
                  <option value="ENCERRADO">Encerrado</option>
                </select>
              </label>
            )}
            <div className="form-submit">
              <button className="danger-action">{editing ? 'Salvar Alteracoes' : 'Salvar Investimento'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map((item, index) => (
          <article className={'finance-card ' + (index % 2 ? 'accent-red' : '')} key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                <span><Icon name="target" size={15} /> {moeda(item.valorMeta)}</span>
                <span><Icon name="calendar" size={15} /> {item.dataAbertura}</span>
                <span style={{ fontSize: 11, color: '#888' }}>por {item.colaboradorNome || '---'}</span>
              </div>
            </div>
            <div className="card-actions">
              <span className={'status ' + (item.status === 'ENCERRADO' ? 'closed' : '')}>{item.status === 'ENCERRADO' ? 'Encerrado' : 'Em andamento'}</span>
              {canManage && <button className="icon-button" onClick={() => openEdit(item)}><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button" onClick={() => remove(item)}><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
