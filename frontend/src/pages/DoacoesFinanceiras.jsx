import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import DateField from '../components/form/DateField'
import Modal from '../components/Modal'
import { dmyToISO } from '../utils/date'
import '../components/form/BaseField/BaseField.scss'
import '../components/Modal/Modal.scss'

const initialForm = { valor: '', categoriaFinanceiraId: '' }

export default function DoacoesFinanceiras() {
  const [items, setItems] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const [erroData, setErroData] = useState('')
  const [editing, setEditing] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [errorModal, setErrorModal] = useState({ open: false, message: '' })

  const [caixaAberto, setCaixaAberto] = useState(null)
  const [caixaLoading, setCaixaLoading] = useState(true)
  const [confirmAbrirCaixa, setConfirmAbrirCaixa] = useState(false)
  const [fecharCaixaModal, setFecharCaixaModal] = useState(false)

  const { user, can } = useAuth()
  const canManage = can('GESTAO_DOACOES')

  const loadCategorias = async () => {
    try {
      const data = await get('/api/categorias-financeiras')
      setCategorias(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const loadCaixaAberto = async () => {
    try {
      const data = await get('/api/caixas/aberto')
      setCaixaAberto(data)
    } catch (_) {}
    finally { setCaixaLoading(false) }
  }

  const load = async (categoriaId, dataInicio, dataFim) => {
    try {
      let path = '/api/doacoes-financeiras'
      const params = []
      if (categoriaId) params.push('categoriaId=' + categoriaId)
      if (dataInicio) params.push('dataInicio=' + encodeURIComponent(dataInicio))
      if (dataFim) params.push('dataFim=' + encodeURIComponent(dataFim))
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load(); loadCategorias(); loadCaixaAberto() }, [])

  useEffect(() => {
    let dateError = ''
    if (filtroDataInicio && filtroDataFim) {
      const isoInicio = dmyToISO(filtroDataInicio)
      const isoFim = dmyToISO(filtroDataFim)
      if (isoFim < isoInicio) {
        dateError = 'Data fim não pode ser menor que data início'
      }
    }
    setErroData(dateError)

    const timer = setTimeout(() => {
      if (!dateError) {
        load(filtroCategoria || null, filtroDataInicio, filtroDataFim)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroCategoria, filtroDataInicio, filtroDataFim])

  const openNew = () => {
    setForm({ ...initialForm })
    setEditing(null)
    setErro('')
    setFieldErrors({})
    setSuccess('')
    setFormOpen(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const openEdit = (item) => {
    setForm({
      valor: String(item.valor),
      categoriaFinanceiraId: String(item.categoriaFinanceiraId),
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
    if (!form.categoriaFinanceiraId) erros.categoriaFinanceiraId = 'Categoria é obrigatória'
    if (form.valor === '') {
      erros.valor = 'Valor é obrigatório'
    } else if (parseFloat(form.valor) <= 0) {
      erros.valor = 'Valor deve ser maior que zero'
    }
    if (!caixaAberto) {
      erros.caixa = 'Caixa fechado. Abra o caixa antes de registrar doação.'
    }
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
    }

    if (!temErros) {
      try {
        const payload = {
          valor: parseFloat(form.valor),
          categoriaFinanceiraId: parseInt(form.categoriaFinanceiraId, 10),
        }

        if (editing) {
          await put('/api/doacoes-financeiras/' + editing, payload)
          setSuccess('Doação alterada com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/doacoes-financeiras', payload)
          setSuccess('Doação cadastrada com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroCategoria || null, filtroDataInicio, filtroDataFim)
        loadCaixaAberto()
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
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
      await del('/api/doacoes-financeiras/' + confirmDelete.id)
      setItems(prev => prev.filter(current => current.id !== confirmDelete.id))
      setSuccess('Doação financeira excluída com sucesso.')
      loadCaixaAberto()
    } catch (error) {
      setErrorModal({ open: true, message: error.message })
    } finally {
      setConfirmDelete(null)
    }
  }

  const handleAbrirCaixa = async () => {
    try {
      await post('/api/caixas', {})
      await loadCaixaAberto()
      setSuccess('Caixa aberto com sucesso.')
    } catch (error) {
      setErro(error.message)
    } finally {
      setConfirmAbrirCaixa(false)
    }
  }

  const handleFecharCaixa = async () => {
    try {
      await put('/api/caixas/' + caixaAberto.id + '/fechar', {
        valorFechamento: parseFloat(caixaAberto.saldo)
      })
      await loadCaixaAberto()
      setSuccess('Caixa fechado com sucesso.')
    } catch (error) {
      setErro(error.message)
    } finally {
      setFecharCaixaModal(false)
    }
  }

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(timer)
  }, [success])

  return (
    <>
      <PageHeader title="Doações Financeiras" subtitle="Registre doações em dinheiro para a igreja" actionLabel={canManage ? 'Nova Doação' : ''} onAction={canManage ? openNew : null} />

      <section className="caixa-status-bar">
        {caixaLoading ? (
          <span>Carregando...</span>
        ) : caixaAberto ? (
          <div className="caixa-status">
            <span className="caixa-indicador verde" />
            <span>Caixa: <strong className="caixa-aberto-texto">Aberto</strong> (Saldo: R$ {parseFloat(caixaAberto.saldo).toFixed(2)})</span>
              {canManage && (
              <button className="icon-button" onClick={() => setFecharCaixaModal(true)} title="Fechar Caixa">
                <Icon name="minus" size={18} />
              </button>
            )}
          </div>
        ) : (
          <div className="caixa-status">
            <span className="caixa-indicador vermelho" />
            <span>Caixa: <strong className="caixa-fechado-texto">Fechado</strong></span>
            {canManage && (
              <button className="icon-button" onClick={() => setConfirmAbrirCaixa(true)} title="Abrir Caixa">
                <Icon name="plus" size={18} />
              </button>
            )}
          </div>
        )}
      </section>

      <section className="filter-bar">
        <select
          value={filtroCategoria}
          onChange={e => setFiltroCategoria(e.target.value)}
        >
          <option value="">Todas as categorias</option>
          {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
        <DateField
          placeholder="Data início"
          value={filtroDataInicio}
          setValue={setFiltroDataInicio}
        />
        <DateField
          placeholder="Data fim"
          value={filtroDataFim}
          setValue={setFiltroDataFim}
          error={erroData}
          minDate={filtroDataInicio ? dmyToISO(filtroDataInicio) : undefined}
        />
      </section>

      {success && <div className="message success">{success}</div>}
      {erro && <div className="message inline">{erro}</div>}

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
        {confirmDelete && (
          <p>Excluir doação de R$ {parseFloat(confirmDelete.valor).toFixed(2)}?</p>
        )}
      </Modal>

      <Modal
        open={errorModal.open}
        title="Erro"
        variant="error"
        onClose={() => setErrorModal({ open: false, message: '' })}
        footer={
          <button className="primary-action" onClick={() => setErrorModal({ open: false, message: '' })}>Fechar</button>
        }
      >
        <p>{errorModal.message}</p>
      </Modal>

      <Modal
        open={confirmAbrirCaixa}
        title="Abrir Caixa"
        variant="info"
        hideCloseButton
        onClose={() => setConfirmAbrirCaixa(false)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setConfirmAbrirCaixa(false)}>Cancelar</button>
            <button className="primary-action" onClick={handleAbrirCaixa}>Confirmar Abertura</button>
          </>
        }
      >
        <p>Deseja realmente abrir o caixa?</p>
      </Modal>

      <Modal
        open={fecharCaixaModal}
        title="Fechar Caixa"
        variant="info"
        hideCloseButton
        onClose={() => setFecharCaixaModal(false)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setFecharCaixaModal(false)}>Cancelar</button>
            <button className="primary-action" onClick={handleFecharCaixa}>Confirmar Fechamento</button>
          </>
        }
      >
        <p>Deseja realmente fechar o caixa?</p>
        <p style={{ marginTop: '0.5rem', color: 'var(--muted)' }}>Valor de fechamento: R$ {caixaAberto ? parseFloat(caixaAberto.saldo).toFixed(2) : '0,00'}</p>
      </Modal>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Doação Financeira' : 'Nova Doação Financeira'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          <form className="inline-form" onSubmit={save} noValidate>
            <div className="field-container">
              <label className="field-label"><span>Valor <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.valor ? ' is-invalid' : '')}
                     value={form.valor} onChange={e => handleFieldChange('valor', e.target.value)}
                     placeholder="0,00" type="number" step="0.01" min="0.01" />
              {fieldErrors.valor && <span className="field-error">{fieldErrors.valor}</span>}
            </div>

            <div className="field-container">
              <label className="field-label"><span>Categoria Financeira <span className="required-star">*</span></span></label>
              <select className={'field-control' + (fieldErrors.categoriaFinanceiraId ? ' is-invalid' : '')}
                      value={form.categoriaFinanceiraId}
                      onChange={e => handleFieldChange('categoriaFinanceiraId', e.target.value)}>
                <option value="">Selecione...</option>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
              {fieldErrors.categoriaFinanceiraId && <span className="field-error">{fieldErrors.categoriaFinanceiraId}</span>}
            </div>

            {fieldErrors.caixa && <div className="message inline">{fieldErrors.caixa}</div>}

            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alterações' : 'Salvar Doação'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.categoriaFinanceiraNome}</h3>
              <div className="meta-row">
                <span><Icon name="dollar" size={14} /> <strong>Valor:</strong> R$ {parseFloat(item.valor).toFixed(2)}</span>
                <span><strong>Data do registro:</strong> {item.dataFormatada}</span>
                <span><strong>Registrado por:</strong> {item.colaboradorNome}</span>
              </div>
            </div>
            <div className="card-actions">
              {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
        {items.length === 0 && (
          <p className="empty-state">Nenhuma doação encontrada.</p>
        )}
      </div>
    </>
  )
}
