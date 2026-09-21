import { useEffect, useState } from 'react'
import { del, get, patch, post, put } from '../api/http'
import { toast } from 'react-toastify'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import { DateTimeField, CurrencyField } from '../components/form'
import { moeda, valorParaNumero } from '../utils/format'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import '../components/form/BaseField/BaseField.scss'
import '../components/Modal/Modal.scss'

function toISO(br) {
  if (!br || br.length < 16) return ''
  const [date, time] = br.split(' ')
  const [dd, mm, yyyy] = date.split('/')
  return `${yyyy}-${mm}-${dd}T${time}`
}

function toBr(iso) {
  if (!iso) return ''
  const parts = iso.split(/[- :.T]/)
  if (parts.length >= 5) {
    return `${parts[2]}/${parts[1]}/${parts[0]} ${parts[3]}:${parts[4]}`
  }
  try {
    const d = new Date(iso)
    if (!isNaN(d.getTime())) {
      const pad = n => String(n).padStart(2, '0')
      return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  } catch {}
  return ''
}

const initialForm = {
  nome: '',
  descricao: '',
  dataInicio: '',
  dataFim: '',
  categoriaEventoId: '',
  localId: '',
  coordenadorId: ''
}

const initialEncerramento = {
  observacoesHistorico: '',
  resultadoFinanceiro: ''
}

export default function Eventos() {

  const [items, setItems] = useState([])

  const [categorias, setCategorias] = useState([])
  const [locais, setLocais] = useState([])
  const [coordenadores, setCoordenadores] = useState([])

  const getCategoriaNome = (id) =>
  categorias.find(c => c.id === id)?.nome || '-'

  const getLocalNome = (id) =>
  locais.find(l => l.id === id)?.nome || '-'

  const getCoordenadorNome = (id) =>
  coordenadores.find(u => u.id === id)?.nome || '-'

  const [form, setForm] = useState(initialForm)
  const [encerramento, setEncerramento] = useState(initialEncerramento)

  const [formOpen, setFormOpen] = useState(false)
  const [encerrarOpen, setEncerrarOpen] = useState(false)

  const [editing, setEditing] = useState(null)
  const [encerrandoId, setEncerrandoId] = useState(null)

  const [confirmAction, setConfirmAction] = useState(null)



  const [fieldErrors, setFieldErrors] = useState({})

  const [encerramentoFieldErrors, setEncerramentoFieldErrors] = useState({})

  const [filtroNome, setFiltroNome] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')

  const [expanded, setExpanded] = useState([])

  const { can } = useAuth()

  const canManage = can('GESTAO_EVENTOS')

  const load = async (nome, status) => {

    try {

      let path = '/api/eventos'

      const params = []

      if (nome)
        params.push('nome=' + encodeURIComponent(nome))

      if (status)
        params.push('status=' + encodeURIComponent(status))

      if (params.length)
        path += '?' + params.join('&')

      const data = await get(path)

      setItems(Array.isArray(data) ? data : [])
    }
    catch (error) {
      toast.error(error.message)
    }
  }

  const loadAuxiliares = async () => {

    try {

      const categoriasData = await get('/api/categorias-eventos')
      setCategorias(Array.isArray(categoriasData) ? categoriasData : [])

      try {
        const locaisData = await get('/api/locais-evento')
        setLocais(Array.isArray(locaisData) ? locaisData : [])
        }
        catch {
        setLocais([])
        }

      try {

        const usuarios = await get('/api/usuarios')

        setCoordenadores(
          Array.isArray(usuarios) ? usuarios : []
        )
      }
      catch {
        setCoordenadores([])
      }
    }
    catch (error) {
      console.error(error)
    }
  }

  useEffect(() => {

    load()
    loadAuxiliares()

  }, [])

  useEffect(() => {

    const timer = setTimeout(() => {

      load(filtroNome, filtroStatus)

    }, 300)

    return () => clearTimeout(timer)

  }, [filtroNome, filtroStatus])

  const openNew = () => {

    setForm(initialForm)

    setEditing(null)

    setFieldErrors({})

    setFormOpen(true)
  }

  const openEdit = (item) => {

    setForm({
      nome: item.nome || '',
      descricao: item.descricao || '',
      dataInicio: toBr(item.dataInicio),
      dataFim: toBr(item.dataFim),
      categoriaEventoId: item.categoriaEventoId || '',
      localId: item.localId || '',
      coordenadorId: item.coordenadorId || ''
    })

    setEditing(item.id)

    setFieldErrors({})

    setFormOpen(true)

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }

  const handleFieldChange = (field, value) => {

    setForm(prev => ({
      ...prev,
      [field]: value
    }))

    setFieldErrors(prev => ({
      ...prev,
      [field]: ''
    }))
  }

  const validarCampos = () => {

    const erros = {}

    if (!form.nome.trim())
      erros.nome = 'Nome é obrigatório'

    if (!form.dataInicio) {
      erros.dataInicio = 'Data de início é obrigatória'
    } else if (!/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/.test(form.dataInicio)) {
      erros.dataInicio = 'Data de início inválida'
    } else {
      const dataInicioISO = toISO(form.dataInicio)
      if (isNaN(new Date(dataInicioISO).getTime())) {
        erros.dataInicio = 'Data de início inválida'
      } else {
        const hoje = new Date()
        hoje.setHours(0, 0, 0, 0)
        if (new Date(dataInicioISO) < hoje)
          erros.dataInicio = 'Data de início não pode ser no passado'
      }
    }

    if (!form.dataFim) {
      erros.dataFim = 'Data de fim é obrigatória'
    } else if (!/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/.test(form.dataFim)) {
      erros.dataFim = 'Data de fim inválida'
    } else {
      const dataFimISO = toISO(form.dataFim)
      if (isNaN(new Date(dataFimISO).getTime()))
        erros.dataFim = 'Data de fim inválida'
    }

    return erros
  }

  const save = async (event) => {

    event.preventDefault()

    setFieldErrors({})

    const erros = validarCampos()

    const temErros = Object.keys(erros).length > 0

    if (temErros)
      setFieldErrors(erros)

    if (!temErros) {

      try {

        const payload = {
          nome: form.nome,
          descricao: form.descricao || null,
          dataInicio: toISO(form.dataInicio),
          dataFim: toISO(form.dataFim),
          categoriaEventoId: form.categoriaEventoId || null,
          localId: form.localId || null,
          coordenadorId: form.coordenadorId || null
        }

        if (editing) {

          await put('/api/eventos/' + editing, payload)

          toast.success('Evento alterado com sucesso.')

          setFormOpen(false)
        }
        else {

          await post('/api/eventos', payload)

          toast.success('Evento criado com sucesso.')
        }

        setEditing(null)

        setForm(initialForm)

        load(filtroNome, filtroStatus)
      }
      catch (error) {

        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        }
        else {
          toast.error(error.message)
        }
      }
    }
  }

  const abrirEvento = (item) => {
    setConfirmAction({ item, type: 'abrir' })
  }

  const cancelarEvento = (item) => {
    setConfirmAction({ item, type: 'cancelar' })
  }

  const openEncerrar = (item) => {

    setEncerrandoId(item.id)

    setEncerramento(initialEncerramento)

    setEncerramentoFieldErrors({})

    setEncerrarOpen(true)
  }

  const encerrarEvento = () => {

    if (!encerramento.resultadoFinanceiro) {
      setEncerramentoFieldErrors({ resultadoFinanceiro: 'Resultado financeiro é obrigatório' })
      return
    }

    setConfirmAction({
      item: { id: encerrandoId },
      type: 'encerrar',
      payload: {
        observacoesHistorico: encerramento.observacoesHistorico || null,
        resultadoFinanceiro: valorParaNumero(encerramento.resultadoFinanceiro)
      }
    })
  }

  const remove = (item) => {
    setConfirmAction({ item, type: 'delete' })
  }

  const handleConfirmAction = async () => {
    if (!confirmAction) return
    const { item, type, payload } = confirmAction

    try {
      if (type === 'delete') {
        await del('/api/eventos/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        toast.success('Evento excluído com sucesso.')
      } else if (type === 'abrir') {
        await patch('/api/eventos/' + item.id + '/abrir')
        toast.success('Evento aberto com sucesso.')
        load(filtroNome, filtroStatus)
      } else if (type === 'cancelar') {
        await patch('/api/eventos/' + item.id + '/cancelar')
        toast.success('Evento cancelado com sucesso.')
        load(filtroNome, filtroStatus)
      } else if (type === 'encerrar') {
        const resposta = await patch('/api/eventos/' + item.id + '/encerrar', payload)
        setEncerrarOpen(false)
        toast.success('Evento encerrado com sucesso.')
        if (resposta?.avisos && resposta.avisos.length > 0) {
          resposta.avisos.forEach(aviso => toast.warning(aviso))
        }
        load(filtroNome, filtroStatus)
      }
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmAction(null)
    }
  }

  const toggleExpand = (id) => {

    setExpanded(prev => {

      if (prev.includes(id))
        return prev.filter(current => current !== id)

      return [...prev, id]
    })
  }

  const getStatusClass = (status) => {

    if (status === 'AGENDADO')
      return 'status'

    if (status === 'ABERTO')
      return 'status-ativo'

    if (status === 'ENCERRADO')
      return 'status closed'

    if (status === 'CANCELADO')
      return 'status-inativo'

    return 'status'
  }

  return (
    <>
      <PageHeader
        title="Eventos"
        subtitle="Gerencie os eventos da igreja"
        actionLabel={canManage ? 'Criar Novo Evento' : ''}
        onAction={canManage ? openNew : null}
      />

      <section className="filter-bar">

        <input
          placeholder="Filtrar por nome..."
          value={filtroNome}
          onChange={e => setFiltroNome(e.target.value)}
        />

        <select
          value={filtroStatus}
          onChange={e => setFiltroStatus(e.target.value)}
        >
          <option value="">Todos os status</option>
          <option value="AGENDADO">Agendado</option>
          <option value="ABERTO">Aberto</option>
          <option value="ENCERRADO">Encerrado</option>
          <option value="CANCELADO">Cancelado</option>
        </select>
      </section>

      {formOpen && (
        <section className="editor-card">

          <div className="editor-title">

            <h2>
              {editing
                ? 'Editar Evento'
                : 'Novo Evento'}
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
                  Nome
                  <span className="required-star">*</span>
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
                placeholder="Nome do evento"
              />

              {fieldErrors.nome &&
                <span className="field-error">
                  {fieldErrors.nome}
                </span>
              }
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Descrição</span>
              </label>

              <input
                value={form.descricao}
                onChange={e =>
                  handleFieldChange('descricao', e.target.value)
                }
                placeholder="Descrição"
              />
            </div>

            <DateTimeField
              label="Data Início"
              required
              value={form.dataInicio}
              setValue={v => handleFieldChange('dataInicio', v)}
              error={fieldErrors.dataInicio}
            />

            <DateTimeField
              label="Data Fim"
              required
              value={form.dataFim}
              setValue={v => handleFieldChange('dataFim', v)}
              error={fieldErrors.dataFim}
            />

            <div className="field-container">
              <label className="field-label">
                <span>Categoria</span>
              </label>

              <select
                value={form.categoriaEventoId}
                onChange={e =>
                handleFieldChange(
                    'categoriaEventoId',
                    e.target.value ? Number(e.target.value) : null
                )
                }
              >
                <option value="">Selecione</option>

                {categorias.map(item => (
                  <option
                    key={item.id}
                    value={item.id}
                  >
                    {item.nome}
                  </option>
                ))}
              </select>
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Local</span>
              </label>

              <select
                value={form.localId}
                onChange={e =>
                handleFieldChange(
                    'localId',
                    e.target.value ? Number(e.target.value) : null
                )
                }
              >
                <option value="">Selecione</option>

                {locais.map(item => (
                  <option
                    key={item.id}
                    value={item.id}
                  >
                    {item.nome}
                  </option>
                ))}
              </select>
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>Coordenador</span>
              </label>

              <select
                value={form.coordenadorId}
                onChange={e =>
                handleFieldChange(
                    'coordenadorId',
                    e.target.value ? Number(e.target.value) : null
                )
                }
              >
                <option value="">Selecione</option>

                {coordenadores.map(item => (
                  <option
                    key={item.id}
                    value={item.id}
                  >
                    {item.nome}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-submit">
              <button className="primary-action">
                {editing
                  ? 'Salvar Alterações'
                  : 'Salvar Evento'}
              </button>
            </div>
          </form>
        </section>
      )}

      {encerrarOpen && (
        <section className="editor-card">

          <div className="editor-title">

            <h2>Encerrar Evento</h2>

            <button
              className="ghost-icon"
              onClick={() => setEncerrarOpen(false)}
            >
              <Icon name="close" size={16} />
            </button>
          </div>

          <div className="inline-form">

            <CurrencyField
              label="Resultado Financeiro"
              required
              value={encerramento.resultadoFinanceiro}
              setValue={v => {
                setEncerramento(prev => ({ ...prev, resultadoFinanceiro: v }))
                setEncerramentoFieldErrors(prev => ({ ...prev, resultadoFinanceiro: '' }))
              }}
              error={encerramentoFieldErrors.resultadoFinanceiro}
            />

            <div className="field-container">
              <label className="field-label">
                <span>Observações</span>
              </label>

              <input
                value={encerramento.observacoesHistorico}
                onChange={e =>
                  setEncerramento(prev => ({
                    ...prev,
                    observacoesHistorico: e.target.value
                  }))
                }
              />
            </div>

            <div className="form-submit">
              <button
                className="primary-action"
                onClick={encerrarEvento}
              >
                Encerrar Evento
              </button>
            </div>
          </div>
        </section>
      )}

      <Modal
        open={!!confirmAction}
        title={
          confirmAction?.type === 'delete' ? 'Confirmar exclusão'
          : confirmAction?.type === 'encerrar' ? 'Confirmar encerramento'
          : 'Confirmar ação'
        }
        variant={confirmAction?.type === 'delete' ? 'error' : 'info'}
        hideCloseButton
        onClose={() => setConfirmAction(null)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setConfirmAction(null)}>Cancelar</button>
            <button className="danger-action" onClick={handleConfirmAction}>
              {confirmAction?.type === 'delete' ? 'Excluir'
                : confirmAction?.type === 'encerrar' ? 'Encerrar'
                : confirmAction?.type === 'abrir' ? 'Abrir'
                : 'Confirmar'
              }
            </button>
          </>
        }
      >
        {confirmAction?.type === 'delete' && (
          <p>Excluir evento "{confirmAction.item.nome}"?</p>
        )}
        {confirmAction?.type === 'abrir' && (
          <p>Tem certeza que deseja abrir este evento?</p>
        )}
        {confirmAction?.type === 'cancelar' && (
          <p>Tem certeza que deseja cancelar este evento?</p>
        )}
        {confirmAction?.type === 'encerrar' && (
          <p>Tem certeza que deseja encerrar este evento? As informações de encerramento não poderão ser alteradas depois.</p>
        )}
      </Modal>

      <div className="cards-list">

        {items.map(item => {

          const isExpanded = expanded.includes(item.id)

          return (
            <article
              className="finance-card"
              key={item.id}
            >

              <div>

                <h3>
                  {item.nome}

                  <span className={getStatusClass(item.status)}>
                    {item.status}
                  </span>
                </h3>

                <div className="meta-row">

                    <span>
                      <strong>Início:</strong>
                      {' '}
                      {toBr(item.dataInicio)}
                    </span>

                    <span>
                      <strong>Fim:</strong>
                      {' '}
                      {toBr(item.dataFim)}
                    </span>

                  <span>
                    <strong>Categoria:</strong>
                    {' '}
                    {getCategoriaNome(item.categoriaEventoId) || '-'}
                  </span>

                  <span>
                    <strong>Local:</strong>
                    {' '}
                    {getLocalNome(item.localId) || '-'}
                  </span>
                </div>

                {isExpanded && (
                  <div
                    className="meta-row"
                    style={{ marginTop: '10px' }}
                  >

                    <span>
                      <strong>Descrição:</strong>
                      {' '}
                      {item.descricao || '-'}
                    </span>

                    <span>
                      <strong>Coordenador:</strong>
                      {' '}
                      {getCoordenadorNome(item.coordenadorId) || '-'}
                    </span>

                    {item.status === 'ENCERRADO' && (
                      <>
                      <span>
                        <strong>Resultado:</strong>
                        {' '}
                        {moeda(item.resultadoFinanceiro)}
                      </span>

                      <span>
                        <strong>Observações:</strong>
                        {' '}
                        {item.observacoesHistorico || '-'}
                      </span>
                      </>
                    )}
                  </div>
                )}
              </div>

              <div className="card-actions">

                <button
                  className="icon-button"
                  onClick={() => toggleExpand(item.id)}
                  title="Expandir"
                >
                  <Icon
                    name={isExpanded ? 'minus' : 'plus'}
                    size={16}
                  />
                </button>

                {canManage &&
                (item.status === 'AGENDADO' || item.status === 'CANCELADO') && (
                    <>
                    {item.status === 'AGENDADO' && (
                        <>
                        <button
                            className="icon-button icon-button--success"
                            onClick={() => abrirEvento(item)}
                            title="Abrir Evento"
                        >
                            <Icon name="play" size={16} />
                        </button>

                        <button
                            className="icon-button"
                            onClick={() => openEdit(item)}
                            title="Editar"
                        >
                            <Icon name="edit" size={16} />
                        </button>

                        <button
                            className="icon-button icon-button--warning"
                            onClick={() => cancelarEvento(item)}
                            title="Cancelar"
                        >
                            <Icon name="x" size={16} />
                        </button>
                        </>
                    )}

                    <button
                        className="icon-button icon-button--danger"
                        onClick={() => remove(item)}
                        title="Excluir"
                    >
                        <Icon name="trash" size={16} />
                    </button>
                    </>
                )}

                {canManage &&
                  item.status === 'ABERTO' && (
                    <button
                      className="icon-button icon-button--danger"
                      onClick={() => openEncerrar(item)}
                      title="Encerrar Evento"
                    >
                      <Icon name="check" size={16} />
                    </button>
                  )
                }
              </div>
            </article>
          )
        })}
      </div>
    </>
  )
}