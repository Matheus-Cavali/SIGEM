import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { dataParaBackend, formatarData, moeda, valorParaNumero } from '../utils/format'
import { useAuth } from '../state/AuthContext'

const initialForm = { investimentoId: '', valorAporte: '', dataAporte: '' }

export default function Aportes() {
  const [investimentos, setInvestimentos] = useState([])
  const [aportes, setAportes] = useState([])
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(initialForm)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const { user, can } = useAuth()
  const canManage = can(['LANCAR_APORTE', 'REGISTRAR_INVESTIMENTO'])

  const load = async () => {
    try {
      const invs = await get('/api/investimentos')
      const safeInvs = Array.isArray(invs) ? invs : []
      const lists = await Promise.all(safeInvs.map(inv => get('/api/investimentos/' + inv.id + '/aporte').catch(() => [])))
      const flat = []

      safeInvs.forEach((inv, index) => {
        const current = Array.isArray(lists[index]) ? lists[index] : []
        current.forEach(aporte => {
          flat.push({ ...aporte, investimentoId: inv.id, investimentoNome: inv.nome })
        })
      })

      setInvestimentos(safeInvs)
      setAportes(flat)
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

  const openEdit = (item) => {
    setForm({
      investimentoId: String(item.investimentoId),
      valorAporte: String(item.valorAporte || '').replace('.', ','),
      dataAporte: item.dataAporte || '',
    })
    setEditing(item)
    setErro('')
    setFormOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      const value = valorParaNumero(form.valorAporte)

      if (!form.investimentoId || !value || !form.dataAporte) {
        throw new Error('Investimento, valor e data sao obrigatorios')
      }

      if (editing) {
        await put('/api/investimentos/' + form.investimentoId + '/aportes/' + editing.id, { valorAporte: value, dataAporte: dataParaBackend(form.dataAporte) })
      } else {
        await post('/api/investimentos/' + form.investimentoId + '/aporte', { valorAporte: value, dataAporte: dataParaBackend(form.dataAporte), colaboradorId: user.id })
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
    const confirmed = window.confirm('Excluir aporte de ' + moeda(item.valorAporte) + '?')

    if (confirmed) {
      try {
        await del('/api/investimentos/' + item.investimentoId + '/aportes/' + item.id)
        setAportes(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader title="Aportes" subtitle="Registre os aportes realizados para cada investimento" actionLabel={canManage ? 'Adicionar Aporte' : ''} onAction={openNew} />

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Aporte' : 'Novo Aporte'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Investimento</span>
              <select value={form.investimentoId} onChange={e => setForm(prev => ({ ...prev, investimentoId: e.target.value }))}>
                <option value="">Selecione o investimento</option>
                {investimentos.map(item => <option key={item.id} value={item.id}>{item.nome}</option>)}
              </select>
            </label>
            <label>
              <span>Valor (R$)</span>
              <input value={form.valorAporte} onChange={e => setForm(prev => ({ ...prev, valorAporte: e.target.value }))} placeholder="0,00" />
            </label>
            <label>
              <span>Data</span>
              <input value={form.dataAporte} onChange={e => setForm(prev => ({ ...prev, dataAporte: formatarData(e.target.value) }))} placeholder="dd/mm/aaaa" />
            </label>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Aporte'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {aportes.map(item => (
          <article className="finance-card" key={item.investimentoId + '-' + item.id}>
            <div>
              <h3><Icon name="trend" size={15} /> {item.investimentoNome}</h3>
              <div className="meta-row">
                <span><Icon name="dollar" size={15} /> {moeda(item.valorAporte)}</span>
                <span><Icon name="calendar" size={15} /> {item.dataAporte}</span>
                {item.colaboradorNome && <span><Icon name="users" size={15} /> {item.colaboradorNome}</span>}
              </div>
            </div>
            <div className="card-actions">
              <strong className="amount">{moeda(item.valorAporte)}</strong>
              {canManage && <button className="icon-button" onClick={() => openEdit(item)}><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)}><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
