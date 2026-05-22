import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { dataParaBackend, moeda, valorParaNumero } from '../utils/format'
import CurrencyField from '../components/form/CurrencyField'
import DateField from '../components/form/DateField'
import { useAuth } from '../state/AuthContext'

const initialForm = { investimentoId: '', valorAporte: '0,00', dataAporte: '' }

export default function Aportes() {
  const [investimentos, setInvestimentos] = useState([])
  const [aportes, setAportes] = useState([])
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(initialForm)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroColaborador, setFiltroColaborador] = useState('')
  const { user, can } = useAuth()
  const canManage = can(['LANCAR_APORTE', 'REGISTRAR_INVESTIMENTO'])

  const filtered = aportes.filter(a => {
    const matchNome = !filtroNome || (a.investimentoNome || '').toLowerCase().includes(filtroNome.toLowerCase())
    const matchColab = !filtroColaborador || (a.colaboradorNome || '').toLowerCase().includes(filtroColaborador.toLowerCase())
    return matchNome && matchColab
  })

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
      if (!error.fieldErrors) setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFieldErrors({})
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
    setFieldErrors({})
    setFormOpen(true)
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.investimentoId) erros.investimentoId = 'Selecione um investimento'
    if (!form.valorAporte || !valorParaNumero(form.valorAporte)) erros.valorAporte = 'Valor do aporte deve ser maior que zero'
    if (!form.dataAporte.trim()) erros.dataAporte = 'Data do aporte é obrigatória'
    return erros
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')
    setFieldErrors({})

    const campos = validarCampos()
    const temErros = Object.keys(campos).length > 0
    if (temErros) {
      setFieldErrors(campos)
    }

    if (!temErros) {
      try {
        const value = valorParaNumero(form.valorAporte)

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
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
      }
    }
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
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

      <section className="filter-bar">
        <input placeholder="Filtrar por investimento..." value={filtroNome}
               onChange={e => setFiltroNome(e.target.value)} />
        <input placeholder="Filtrar por quem lançou..." value={filtroColaborador}
               onChange={e => setFiltroColaborador(e.target.value)} />
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Aporte' : 'Novo Aporte'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <div className="field-container">
              <label className="field-label">Investimento <span className="required-star">*</span></label>
              <select className={'field-control' + (fieldErrors.investimentoId ? ' is-invalid' : '')}
                      value={form.investimentoId} onChange={e => handleFieldChange('investimentoId', e.target.value)}>
                <option value="">Selecione o investimento</option>
                {investimentos.map(item => (
                  <option key={item.id} value={item.id} disabled={item.status === 'ENCERRADO'}>
                    {item.nome}{item.status === 'ENCERRADO' ? ' (Encerrado)' : ''}
                  </option>
                ))}
              </select>
              {fieldErrors.investimentoId && <span className="field-error">{fieldErrors.investimentoId}</span>}
            </div>
            <CurrencyField label="Valor (R$)" value={form.valorAporte}
              setValue={v => handleFieldChange('valorAporte', v)} error={fieldErrors.valorAporte} required />
            <DateField label="Data" value={form.dataAporte} setValue={v => handleFieldChange('dataAporte', v)} error={fieldErrors.dataAporte} required />
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Aporte'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {filtered.map(item => (
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
