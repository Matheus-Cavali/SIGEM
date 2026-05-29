import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { moeda, valorParaNumero } from '../utils/format'
import DateField from '../components/form/DateField'
import '../components/form/BaseField/BaseField.scss'

// Retorna a data de hoje no formato dd/mm/aaaa
function hoje() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

// Retorna hoje no formato yyyy-mm-dd (para minDate do DateField)
function hojeISO() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// Converte dd/mm/aaaa para objeto Date (para comparação)
function parseDMY(str) {
  if (!str) return null
  const [dd, mm, yyyy] = str.split('/')
  if (!dd || !mm || !yyyy) return null
  return new Date(Number(yyyy), Number(mm) - 1, Number(dd))
}

// Verifica se uma data dd/mm/aaaa está no passado
function isDataPassada(str) {
  const d = parseDMY(str)
  if (!d) return false
  const t = new Date()
  t.setHours(0, 0, 0, 0)
  return d < t
}

const initialForm = {
  descricao: '',
  valor: '',
  dataVencimento: '',
  dataPrazo: '',
  categoriaDespesaId: ''
}

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

  // Modal de quitar
  const [quitarModal, setQuitarModal] = useState(null) // item sendo quitado
  const [quitarData, setQuitarData] = useState('')
  const [quitarErro, setQuitarErro] = useState('')

  // Ajuste de saldo
  const [ajusteOpen, setAjusteOpen] = useState(false)
  const [ajusteValor, setAjusteValor] = useState('')
  const [ajusteErro, setAjusteErro] = useState('')

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
      dataPrazo: item.dataPrazo || '',
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
    if (!form.valor || !valor || Number(valor) <= 0) erros.valor = 'Valor deve ser maior que zero'
    if (!form.dataVencimento) {
      erros.dataVencimento = 'Data de vencimento é obrigatória'
    } else if (isDataPassada(form.dataVencimento)) {
      erros.dataVencimento = 'A data de vencimento não pode ser no passado'
    }
    if (form.dataPrazo && isDataPassada(form.dataPrazo)) {
      erros.dataPrazo = 'O prazo de pagamento não pode ser no passado'
    }
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
        dataPrazo: form.dataPrazo || null,
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

  const abrirQuitar = (item) => {
    setQuitarModal(item)
    setQuitarData(hoje())
    setQuitarErro('')
  }

  const confirmarQuitar = async () => {
    if (!quitarData) { setQuitarErro('Informe a data de pagamento'); return }
    setQuitarErro('')
    try {
      const res = await post('/api/despesas/' + quitarModal.id + '/quitar', { dataPagamento: quitarData })
      if (res?.comJuros) {
        setSuccess(`Despesa quitada com juros de 2%. Valor pago: ${moeda(res.valorPago)}`)
      } else {
        setSuccess('Despesa quitada com sucesso.')
      }
      setQuitarModal(null)
      load(filtroDescricao, filtroCategoria || null)
      loadSaldo()
    } catch (error) {
      setQuitarErro(error.message)
    }
  }

  const estornar = async (item) => {
    const confirmed = window.confirm(
      `Estornar pagamento de "${item.descricao}"?\nO valor será devolvido ao caixa e a despesa voltará como pendente.`
    )
    if (!confirmed) return

    try {
      await post('/api/despesas/' + item.id + '/estornar', {})
      setSuccess('Pagamento estornado com sucesso.')
      load(filtroDescricao, filtroCategoria || null)
      loadSaldo()
    } catch (error) {
      alert(error.message)
    }
  }

  const salvarAjusteSaldo = async () => {
    setAjusteErro('')
    const valor = valorParaNumero(ajusteValor)
    if (!ajusteValor || valor <= 0) {
      setAjusteErro('Informe um valor maior que zero')
      return
    }
    try {
      await post('/api/despesas/ajustar-saldo', { valor })
      setSuccess('Saldo do caixa ajustado com sucesso.')
      setAjusteOpen(false)
      setAjusteValor('')
      loadSaldo()
    } catch (error) {
      setAjusteErro(error.message)
    }
  }

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(timer)
  }, [success])

  const pendentes = items.filter(i => !i.dataPagamento)
  const pagas = items.filter(i => i.dataPagamento)

  // Separa pendentes em: em atraso e no prazo
  const emAtraso = pendentes.filter(i => i.emAtraso)
  const noPrazo = pendentes.filter(i => !i.emAtraso)

  return (
    <>
      <PageHeader
        title="Despesas"
        subtitle="Gerencie as despesas da igreja"
        actionLabel={canLancar || canManage ? 'Adicionar Despesa' : ''}
        onAction={canLancar || canManage ? openNew : null}
      />

      {/* Saldo + Botão de ajuste */}
      {saldo !== null && (
        <section className="editor-card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <Icon name="dollar" size={18} />
            <span style={{ fontWeight: 600 }}>Saldo disponível:</span>
            <span style={{
              fontWeight: 700,
              color: Number(saldo) >= 0 ? 'var(--color-success, #16a34a)' : 'var(--color-danger, #dc2626)',
              fontSize: '1.1rem'
            }}>
              {moeda(saldo)}
            </span>
            {canManage && (
              <button
                className="icon-button"
                style={{ marginLeft: 'auto', fontSize: '0.78rem', padding: '0.3rem 0.8rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
                onClick={() => { setAjusteOpen(o => !o); setAjusteErro(''); setAjusteValor('') }}
                title="Editar valor do caixa para testes"
              >
                <Icon name="edit" size={14} /> Editar caixa
              </button>
            )}
          </div>

          {ajusteOpen && (
            <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'flex-start', gap: '0.5rem', flexWrap: 'wrap' }}>
              <div style={{ flex: 1, minWidth: 180 }}>
                <input
                  className="field-control"
                  placeholder="Novo valor do caixa (R$)"
                  value={ajusteValor}
                  onChange={e => { setAjusteValor(e.target.value); setAjusteErro('') }}
                  style={{ width: '100%' }}
                />
                {ajusteErro && <span className="field-error">{ajusteErro}</span>}
              </div>
              <button className="primary-action" style={{ fontSize: '0.82rem', padding: '0.4rem 1rem' }} onClick={salvarAjusteSaldo}>
                Salvar
              </button>
              <button className="ghost-icon" onClick={() => { setAjusteOpen(false); setAjusteErro('') }}>
                <Icon name="close" size={16} />
              </button>
            </div>
          )}
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
            <button className="ghost-icon" onClick={() => setFormOpen(false)}>
              <Icon name="close" size={16} />
            </button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>

            <div className="field-container">
              <label className="field-label">
                <span>Descrição <span className="required-star">*</span></span>
              </label>
              <input
                className={'field-control' + (fieldErrors.descricao ? ' is-invalid' : '')}
                value={form.descricao}
                onChange={e => handleFieldChange('descricao', e.target.value)}
                placeholder="Descrição da despesa"
              />
              {fieldErrors.descricao && <span className="field-error">{fieldErrors.descricao}</span>}
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Valor (R$) <span className="required-star">*</span></span>
              </label>
              <input
                className={'field-control' + (fieldErrors.valor ? ' is-invalid' : '')}
                value={form.valor}
                onChange={e => handleFieldChange('valor', e.target.value)}
                placeholder="0,00"
              />
              {fieldErrors.valor && <span className="field-error">{fieldErrors.valor}</span>}
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Categoria de Despesa <span className="required-star">*</span></span>
              </label>
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
              <label className="field-label">
                <span>Data de Vencimento <span className="required-star">*</span></span>
              </label>
              <DateField
                value={form.dataVencimento}
                setValue={v => handleFieldChange('dataVencimento', v)}
                error={fieldErrors.dataVencimento}
                minDate={hojeISO()}
                maxDate="2099-12-31"
              />
              {fieldErrors.dataVencimento && <span className="field-error">{fieldErrors.dataVencimento}</span>}
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Prazo para Pagamento</span>
              </label>
              <DateField
                value={form.dataPrazo}
                setValue={v => handleFieldChange('dataPrazo', v)}
                error={fieldErrors.dataPrazo}
                minDate={hojeISO()}
                maxDate="2099-12-31"
              />
              {fieldErrors.dataPrazo && <span className="field-error">{fieldErrors.dataPrazo}</span>}
              <span style={{ fontSize: '0.75rem', color: 'var(--color-muted)', marginTop: '0.2rem', display: 'block' }}>
                Se pago após o prazo, incide juros de 2% sobre o valor.
              </span>
            </div>

            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alterações' : 'Salvar Despesa'}</button>
            </div>
          </form>
        </section>
      )}

      {/* Modal de Quitar */}
      {quitarModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <section className="editor-card" style={{ width: '100%', maxWidth: 420, margin: '1rem' }}>
            <div className="editor-title">
              <h2>Confirmar Pagamento</h2>
              <button className="ghost-icon" onClick={() => setQuitarModal(null)}>
                <Icon name="close" size={16} />
              </button>
            </div>

            <p style={{ marginBottom: '0.5rem' }}>
              <strong>{quitarModal.descricao}</strong>
            </p>

            {quitarModal.dataPrazo && (
              <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', marginBottom: '0.5rem' }}>
                Prazo sem juros: <strong>{quitarModal.dataPrazo}</strong>
              </p>
            )}

            <div className="field-container" style={{ marginBottom: '0.75rem' }}>
              <label className="field-label">
                <span>Data de pagamento <span className="required-star">*</span></span>
              </label>
              <DateField
                value={quitarData}
                setValue={setQuitarData}
                minDate="2000-01-01"
                maxDate="2099-12-31"
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--color-muted)', marginTop: '0.2rem', display: 'block' }}>
                Use uma data após o prazo para simular juros de 2%.
              </span>
            </div>

            {(() => {
              const prazo = quitarModal.dataPrazo
              const venc = quitarModal.dataVencimento
              const limite = prazo || venc
              const paga = parseDMY(quitarData)
              const lim = parseDMY(limite)
              const comJuros = paga && lim && paga > lim
              const valorBase = Number(quitarModal.valor)
              let valorFinal = valorBase
              if (comJuros && paga && lim) {
                const diasAtraso = Math.round((paga - lim) / (1000 * 60 * 60 * 24))
                valorFinal = +(valorBase * Math.pow(1.02, diasAtraso)).toFixed(2)
              }
              return (
                <div style={{
                  background: comJuros ? '#fef2f2' : '#f0fdf4',
                  border: `1px solid ${comJuros ? '#fca5a5' : '#86efac'}`,
                  borderRadius: '0.5rem',
                  padding: '0.75rem',
                  marginBottom: '1rem'
                }}>
                  {comJuros ? (
                    <>
                      {(() => {
                        const diasAtraso = (paga && lim) ? Math.round((paga - lim) / (1000 * 60 * 60 * 24)) : 0
                        return (
                          <>
                            <p style={{ color: '#dc2626', fontWeight: 600, marginBottom: '0.3rem' }}>
                              ⚠️ {diasAtraso} dia{diasAtraso !== 1 ? 's' : ''} em atraso — juros compostos de 2%/dia
                            </p>
                            <p style={{ fontSize: '0.85rem' }}>Valor original: <strong>{moeda(valorBase)}</strong></p>
                            <p style={{ fontSize: '0.85rem' }}>Valor com juros: <strong style={{ color: '#dc2626' }}>{moeda(valorFinal)}</strong></p>
                          </>
                        )
                      })()}
                    </>
                  ) : (
                    <p style={{ color: '#16a34a', fontWeight: 600 }}>
                      ✓ Dentro do prazo — Valor: {moeda(valorBase)}
                    </p>
                  )}
                </div>
              )
            })()}

            {quitarErro && <div className="message inline" style={{ marginBottom: '0.75rem' }}>{quitarErro}</div>}

            <div className="form-submit" style={{ display: 'flex', gap: '0.5rem' }}>
              <button className="primary-action" onClick={confirmarQuitar}>Confirmar Pagamento</button>
              <button className="icon-button" onClick={() => setQuitarModal(null)}>Cancelar</button>
            </div>
          </section>
        </div>
      )}

      {/* Em atraso */}
      {emAtraso.length > 0 && (
        <>
          <h3 style={{ margin: '1rem 0 0.5rem', fontSize: '0.85rem', color: '#dc2626', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Icon name="x" size={15} /> Em Atraso ({emAtraso.length})
          </h3>
          <div className="cards-list">
            {emAtraso.map(item => (
              <article
                className="finance-card"
                key={item.id}
                style={{ borderLeft: '4px solid #dc2626' }}
              >
                <div>
                  <h3>
                    {item.descricao}
                    <span className="status-inativo" style={{ marginLeft: '0.5rem' }}>EM ATRASO</span>
                  </h3>
                  <div className="meta-row">
                    <span>
                      <Icon name="dollar" size={15} />
                      {' '}Valor original: {moeda(item.valor)}
                    </span>
                    <span style={{ color: '#dc2626', fontWeight: 600 }}>
                      <Icon name="dollar" size={15} />
                      {' '}Com juros (2%): {moeda(item.valorComJuros)}
                    </span>
                    <span>
                      <Icon name="calendar" size={15} />
                      {' '}Venceu: {item.dataVencimento}
                    </span>
                    {item.dataPrazo && (
                      <span style={{ color: '#dc2626' }}>
                        <Icon name="calendar" size={15} />
                        {' '}Prazo: {item.dataPrazo}
                      </span>
                    )}
                    <span className="tag">{item.categoriaDespesaNome || tipos.find(t => t.id === item.categoriaDespesaId)?.nome}</span>
                  </div>
                </div>
                <div className="card-actions">
                  {(canLancar || canManage) && (
                    <button
                      className="primary-action"
                      style={{ fontSize: '0.78rem', padding: '0.3rem 0.8rem', background: '#dc2626' }}
                      onClick={() => abrirQuitar(item)}
                      title="Quitar com juros"
                    >
                      Quitar c/ Juros
                    </button>
                  )}
                  {canManage && (
                    <button className="icon-button" onClick={() => openEdit(item)} title="Editar">
                      <Icon name="edit" size={16} />
                    </button>
                  )}
                  {canManage && (
                    <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir">
                      <Icon name="trash" size={16} />
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      {/* Pendentes no prazo */}
      {noPrazo.length > 0 && (
        <>
          <h3 style={{ margin: '1rem 0 0.5rem', fontSize: '0.85rem', color: 'var(--color-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Pendentes ({noPrazo.length})
          </h3>
          <div className="cards-list">
            {noPrazo.map(item => (
              <article className="finance-card" key={item.id}>
                <div>
                  <h3>{item.descricao}</h3>
                  <div className="meta-row">
                    <span><Icon name="dollar" size={15} /> {moeda(item.valor)}</span>
                    <span><Icon name="calendar" size={15} /> Vence: {item.dataVencimento}</span>
                    {item.dataPrazo && (
                      <span>
                        <Icon name="calendar" size={15} />
                        {' '}Prazo: {item.dataPrazo}
                      </span>
                    )}
                    <span className="tag">{item.categoriaDespesaNome || tipos.find(t => t.id === item.categoriaDespesaId)?.nome}</span>
                  </div>
                </div>
                <div className="card-actions">
                  {(canLancar || canManage) && (
                    <button
                      className="primary-action"
                      style={{ fontSize: '0.78rem', padding: '0.3rem 0.8rem' }}
                      onClick={() => abrirQuitar(item)}
                      title="Quitar despesa"
                    >
                      Quitar
                    </button>
                  )}
                  {canManage && (
                    <button className="icon-button" onClick={() => openEdit(item)} title="Editar">
                      <Icon name="edit" size={16} />
                    </button>
                  )}
                  {canManage && (
                    <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir">
                      <Icon name="trash" size={16} />
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      {/* Pagas */}
      {pagas.length > 0 && (
        <>
          <h3 style={{ margin: '1.5rem 0 0.5rem', fontSize: '0.85rem', color: 'var(--color-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Pagas ({pagas.length})
          </h3>
          <div className="cards-list">
            {pagas.map(item => (
              <article className="finance-card" key={item.id} style={{ opacity: 0.7 }}>
                <div>
                  <h3>{item.descricao}</h3>
                  <div className="meta-row">
                    <span><Icon name="dollar" size={15} /> {moeda(item.valor)}</span>
                    <span><Icon name="calendar" size={15} /> Pago em: {item.dataPagamento}</span>
                    {item.dataVencimento && (
                      <span><Icon name="calendar" size={15} /> Vencimento: {item.dataVencimento}</span>
                    )}
                    <span className="tag">{item.categoriaDespesaNome || tipos.find(t => t.id === item.categoriaDespesaId)?.nome}</span>
                  </div>
                </div>
                <div className="card-actions">
                  <span className="status closed">Quitada</span>
                  {canManage && (
                    <button
                      className="icon-button icon-button--warning"
                      onClick={() => estornar(item)}
                      title="Estornar pagamento"
                    >
                      <Icon name="x" size={16} />
                    </button>
                  )}
                  {canManage && (
                    <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir">
                      <Icon name="trash" size={16} />
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      {items.length === 0 && (
        <div className="cards-list">
          <p style={{ color: 'var(--color-muted)', textAlign: 'center', padding: '2rem' }}>
            Nenhuma despesa cadastrada.
          </p>
        </div>
      )}
    </>
  )
}
