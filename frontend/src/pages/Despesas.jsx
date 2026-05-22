import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { moeda, valorParaNumero } from '../utils/format'
import DateField from '../components/form/DateField'
import '../components/form/BaseField/BaseField.scss'

const initialForm = { descricao: '', valor: '', dataVencimento: '', categoriaDespesaId: '' }

export default function Despesas() {
  const [items, setItems] = useState([])
  const [tipos, setTipos] = useState([])
  const [saldo, setSaldo] = useState(null)
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroDescricao, setFiltroDescricao] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const { can } = useAuth()
  const canLancar = can('LANCAR_DESPESA')
  const canManage = can('GERENCIAR_DESPESA')

  const loadTipos = async () => {
    try {
      const data = await get('/api/categorias-despesa')
      setTipos(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const load = async (descricao, categoriaId) => {
    try {
      let path = '/api/despesas'
      const params = []
      if (descricao) params.push('descricao=' + encodeURIComponent(descricao))
      if (categoriaId) params.push('categoriaDespesaId=' + categoriaId)
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  const loadSaldo = async () => {
    try {
      const data = await get('/api/despesas/saldo')
      setSaldo(data?.saldo ?? null)
    } catch (_) {}
  }

  useEffect(() => { load(); loadTipos(); loadSaldo() }, [])

  useEffect(() => {
    const timer = setTimeout(() => {
      load(filtroDescricao, filtroCategoria || null)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroDescricao, filtroCategoria])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFieldErrors({})
    setSuccess('')
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      descricao: item.descricao || '',
      valor: String(item.valor || '').replace('.', ','),
      dataVencimento: item.dataVencimento || '',
      categoriaDespesaId: String(item.categoriaDespesaId ?? ''),
    })
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
    if (!form.descricao.trim()) erros.descricao = 'Descrição da despesa é obrigatória'
    const valor = valorParaNumero(form.valor)
    if (!form.valor || !valor || Number(valor) <= 0) erros.valor = 'Valor da despesa é obrigatório e deve ser maior que zero'
    if (!form.dataVencimento) erros.dataVencimento = 'Data de vencimento é obrigatória'
    if (!form.categoriaDespesaId) erros.categoriaDespesaId = 'Categoria é obrigatória'
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
      return
    }

    try {
      const value = valorParaNumero(form.valor)

      const payload = {
        descricao: form.descricao,
        valor: value,
        categoriaDespesaId: Number(form.categoriaDespesaId),
        dataVencimento: form.dataVencimento,
      }

      if (editing) {
        await put('/api/despesas/' + editing, payload)
        setSuccess('Despesa alterada com sucesso.')
        setFormOpen(false)
      } else {
        await post('/api/despesas', payload)
        setSuccess('Despesa cadastrada com sucesso.')
      }

      setEditing(null)
      setForm(initialForm)
      load(filtroDescricao, filtroCategoria || null)
      loadSaldo()
    } catch (error) {
      if (error.fieldErrors) {
        setFieldErrors(error.fieldErrors)
      } else {
        setErro(error.message)
      }
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir despesa "' + item.descricao + '"?')

    if (confirmed) {
      try {
        await del('/api/despesas/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        setSuccess('Despesa excluída com sucesso.')
        loadSaldo()
      } catch (error) {
        alert(error.message)
      }
    }
  }

  const quitar = async (item) => {
    const confirmed = window.confirm('Confirmar pagamento da despesa "' + item.descricao + '"?\nValor: ' + moeda(item.valor))
    if (!confirmed) return

    try {
      await post('/api/despesas/' + item.id + '/quitar', {})
      setSuccess('Despesa quitada com sucesso.')
      load(filtroDescricao, filtroCategoria || null)
      loadSaldo()
    } catch (error) {
      alert(error.message)
    }
  }

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(timer)
  }, [success])

  const pendentes = items.filter(i => !i.dataPagamento)
  const pagas = items.filter(i => i.dataPagamento)

  return (
    <>
      <PageHeader
        title="Despesas"
        subtitle="Gerencie as despesas da igreja"
        actionLabel={canLancar || canManage ? 'Adicionar Despesa' : ''}
        onAction={canLancar || canManage ? openNew : null}
      />

      {saldo !== null && (
        <section className="editor-card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Icon name="dollar" size={18} />
            <span style={{ fontWeight: 600 }}>Saldo disponível:</span>
            <span style={{ fontWeight: 700, color: Number(saldo) >= 0 ? 'var(--color-success, #16a34a)' : 'var(--color-danger, #dc2626)', fontSize: '1.1rem' }}>
              {moeda(saldo)}
            </span>
          </div>
        </section>
      )}

      <section className="filter-bar">
        <input
          placeholder="Filtrar por descrição..."
          value={filtroDescricao}
          onChange={e => setFiltroDescricao(e.target.value)}
        />
        <select
          value={filtroCategoria}
          onChange={e => setFiltroCategoria(e.target.value)}
        >
          <option value="">Todas as categorias</option>
          {tipos.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}
        </select>
      </section>

      {success && <div className="message success">{success}</div>}

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Despesa' : 'Nova Despesa'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>
            <div className="field-container">
              <label className="field-label"><span>Descrição <span className="required-star">*</span></span></label>
              <input
                className={'field-control' + (fieldErrors.descricao ? ' is-invalid' : '')}
                value={form.descricao}
                onChange={e => handleFieldChange('descricao', e.target.value)}
                placeholder="Descrição da despesa"
              />
              {fieldErrors.descricao && <span className="field-error">{fieldErrors.descricao}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Valor (R$) <span className="required-star">*</span></span></label>
              <input
                className={'field-control' + (fieldErrors.valor ? ' is-invalid' : '')}
                value={form.valor}
                onChange={e => handleFieldChange('valor', e.target.value)}
                placeholder="0,00"
              />
              {fieldErrors.valor && <span className="field-error">{fieldErrors.valor}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Categoria de Despesa <span className="required-star">*</span></span></label>
              <select
                className={'field-control' + (fieldErrors.categoriaDespesaId ? ' is-invalid' : '')}
                value={form.categoriaDespesaId}
                onChange={e => handleFieldChange('categoriaDespesaId', e.target.value)}
              >
                <option value="">Selecione...</option>
                {tipos.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}
              </select>
              {fieldErrors.categoriaDespesaId && <span className="field-error">{fieldErrors.categoriaDespesaId}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Data de Vencimento <span className="required-star">*</span></span></label>
              <DateField
                value={form.dataVencimento}
                setValue={v => handleFieldChange('dataVencimento', v)}
                className={'field-control' + (fieldErrors.dataVencimento ? ' is-invalid' : '')}
              />
              {fieldErrors.dataVencimento && <span className="field-error">{fieldErrors.dataVencimento}</span>}
            </div>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alterações' : 'Salvar Despesa'}</button>
            </div>
          </form>
        </section>
      )}

      {pendentes.length > 0 && (
        <>
          <h3 style={{ margin: '1rem 0 0.5rem', fontSize: '0.85rem', color: 'var(--color-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Pendentes ({pendentes.length})
          </h3>
          <div className="cards-list">
            {pendentes.map(item => (
              <article className="finance-card" key={item.id}>
                <div>
                  <h3>{item.descricao}</h3>
                  <div className="meta-row">
                    <span><Icon name="dollar" size={15} /> {moeda(item.valor)}</span>
                    <span><Icon name="calendar" size={15} /> Vence: {item.dataVencimento}</span>
                    <span className="tag">{item.categoriaDespesaNome || tipos.find(t => t.id === item.categoriaDespesaId)?.nome}</span>
                  </div>
                </div>
                <div className="card-actions">
                  {(canLancar || canManage) && (
                    <button className="primary-action" style={{ fontSize: '0.78rem', padding: '0.3rem 0.8rem' }} onClick={() => quitar(item)} title="Quitar despesa">
                      Quitar
                    </button>
                  )}
                  {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
                  {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      {pagas.length > 0 && (
        <>
          <h3 style={{ margin: '1.5rem 0 0.5rem', fontSize: '0.85rem', color: 'var(--color-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Pagas ({pagas.length})
          </h3>
          <div className="cards-list">
            {pagas.map(item => (
              <article className="finance-card" key={item.id} style={{ opacity: 0.65 }}>
                <div>
                  <h3>{item.descricao}</h3>
                  <div className="meta-row">
                    <span><Icon name="dollar" size={15} /> {moeda(item.valor)}</span>
                    <span><Icon name="calendar" size={15} /> Pago em: {item.dataPagamento}</span>
                    <span className="tag">{item.categoriaDespesaNome || tipos.find(t => t.id === item.categoriaDespesaId)?.nome}</span>
                  </div>
                </div>
                <div className="card-actions">
                  <span className="status closed">Quitada</span>
                  {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      {items.length === 0 && (
        <div className="cards-list">
          <p style={{ color: 'var(--color-muted)', textAlign: 'center', padding: '2rem' }}>Nenhuma despesa cadastrada.</p>
        </div>
      )}
    </>
  )
}
