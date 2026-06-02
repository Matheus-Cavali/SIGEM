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

const initialForm = { investimentoId: '', valorAporte: '0,00', dataAporte: '' }

export default function Aportes() {
  const [investimentos, setInvestimentos] = useState([])
  const [aportes, setAportes] = useState([])
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(initialForm)
  const [editing, setEditing] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [usuariosPerm, setUsuariosPerm] = useState([])
  const [filtroInvestimento, setFiltroInvestimento] = useState('')
  const [filtroColaborador, setFiltroColaborador] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [confirmAntecipar, setConfirmAntecipar] = useState(null)
  const [saving, setSaving] = useState(false)
  const { user, can } = useAuth()
  const canManage = can(['LANCAR_APORTE', 'REGISTRAR_INVESTIMENTO'])

  const filtered = aportes.filter(a => {
    const matchInv = !filtroInvestimento || a.investimentoId === Number(filtroInvestimento)
    const matchColab = !filtroColaborador || a.colaboradorId === Number(filtroColaborador)
    return matchInv && matchColab
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

      const usuarios = await get('/api/recurso/usuarios?permissao=LANCAR_APORTE').catch(() => [])
      setUsuariosPerm(Array.isArray(usuarios) ? usuarios : [])
    } catch (error) {
      if (!error.fieldErrors) toast.error(error.message)
    }
  }

  useEffect(() => { load() }, [])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setFieldErrors({})
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      investimentoId: String(item.investimentoId),
      valorAporte: String(Math.round(Number(item.valorAporte || 0) * 100)),
      dataAporte: item.dataAporte || '',
    })
    setEditing(item)
    setFieldErrors({})
    setFormOpen(true)
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.investimentoId) erros.investimentoId = 'Selecione um investimento'
    if (!form.valorAporte || !valorParaNumero(form.valorAporte)) erros.valorAporte = 'Valor do aporte deve ser maior que zero'
    if (!form.dataAporte.trim()) erros.dataAporte = 'Data do aporte é obrigatória'
    if (form.dataAporte.trim()) {
      const partes = form.dataAporte.split('/')
      if (partes.length === 3) {
        const dia = parseInt(partes[0], 10)
        const mes = parseInt(partes[1], 10) - 1
        const ano = parseInt(partes[2], 10)
        const data = new Date(ano, mes, dia)
        const hoje = new Date()
        hoje.setHours(0, 0, 0, 0)
        if (data < hoje) erros.dataAporte = 'Não é permitido lançar aporte com data anterior à data atual'
      }
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
      const value = valorParaNumero(form.valorAporte)
      const apiPath = '/api/investimentos/' + form.investimentoId

      if (editing) {
        setSaving(true)
        try {
          await put(apiPath + '/aportes/' + editing.id, { valorAporte: value, dataAporte: dataParaBackend(form.dataAporte) })
          setFormOpen(false); setEditing(null); setForm(initialForm); load()
        } catch (error) {
          if (error.fieldErrors) setFieldErrors(error.fieldErrors)
          else toast.error(error.message)
        } finally { setSaving(false) }
        return
      }

      const inv = investimentos.find(i => i.id === Number(form.investimentoId))
      if (inv && inv.dataAbertura) {
        const partes = form.dataAporte.split('/')
        const aporteDate = new Date(parseInt(partes[2]), parseInt(partes[1]) - 1, parseInt(partes[0]))
        const invPartes = inv.dataAbertura.split('/')
        const invDate = new Date(parseInt(invPartes[2]), parseInt(invPartes[1]) - 1, parseInt(invPartes[0]))
        aporteDate.setHours(0, 0, 0, 0)
        invDate.setHours(0, 0, 0, 0)
        if (aporteDate < invDate) {
          setConfirmAntecipar({ invNome: inv.nome, dataAporte: form.dataAporte, value, apiPath })
          return
        }
      }

      enviarAporte(value, apiPath)
    }
  }

  const enviarAporte = async (value, apiPath, anteciparData) => {
    setSaving(true)
    try {
      const body = { valorAporte: value, dataAporte: dataParaBackend(form.dataAporte), colaboradorId: user.id }
      if (anteciparData) body.anteciparData = true
      const resp = await post(apiPath + '/aporte', body)
      if (resp && resp.aviso) toast.warning(resp.aviso)
      setFormOpen(false); setEditing(null); setForm(initialForm); load()
    } catch (error) {
      if (error.fieldErrors) setFieldErrors(error.fieldErrors)
      else toast.error(error.message)
    } finally { setSaving(false) }
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const handleConfirmAntecipar = () => {
    if (!confirmAntecipar) return
    enviarAporte(confirmAntecipar.value, confirmAntecipar.apiPath, true)
    setConfirmAntecipar(null)
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/investimentos/' + confirmDelete.investimentoId + '/aportes/' + confirmDelete.id)
      setAportes(prev => prev.filter(current => current.id !== confirmDelete.id))
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

  return (
    <>
      <PageHeader title="Aportes" subtitle="Registre os aportes realizados para cada investimento" actionLabel={canManage ? 'Adicionar Aporte' : ''} onAction={openNew} />

      <section className="filter-bar">
        <div className="field-container">
          <select className="field-control" value={filtroInvestimento} onChange={e => setFiltroInvestimento(e.target.value)}>
            <option value="">Todos os investimentos</option>
            {investimentos.map(inv => (
              <option key={inv.id} value={inv.id}>{inv.nome}</option>
            ))}
          </select>
        </div>
        <div className="field-container">
          <select className="field-control" value={filtroColaborador} onChange={e => setFiltroColaborador(e.target.value)}>
            <option value="">Todos os lançadores</option>
            {usuariosPerm.map(u => (
              <option key={u.id} value={u.id}>{u.nome}</option>
            ))}
          </select>
        </div>
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Aporte' : 'Novo Aporte'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
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
            <DateField label="Data" value={form.dataAporte} setValue={v => handleFieldChange('dataAporte', v)} error={fieldErrors.dataAporte} required minDate={hojeISO()} maxDate="2099-12-31" />
            <div className="form-submit">
              <button className="primary-action" disabled={saving}>{saving ? 'Salvando...' : (editing ? 'Salvar Alteracoes' : 'Salvar Aporte')}</button>
            </div>
          </form>
        </section>
      )}

      <Modal
        open={!!confirmAntecipar}
        title="Antecipar data do investimento"
        variant="info"
        hideCloseButton
        onClose={() => setConfirmAntecipar(null)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setConfirmAntecipar(null)}>Cancelar</button>
            <button className="primary-action" onClick={handleConfirmAntecipar}>Sim, antecipar</button>
          </>
        }
      >
        <p>A data do aporte ({confirmAntecipar?.dataAporte}) é anterior à data de abertura do investimento "{confirmAntecipar?.invNome}".</p>
        <p>Deseja antecipar a data de abertura do investimento para {confirmAntecipar?.dataAporte}? Caso contrário, o aporte não será registrado.</p>
      </Modal>

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
        <p>Excluir aporte de {confirmDelete && moeda(confirmDelete.valorAporte)}?</p>
      </Modal>

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
