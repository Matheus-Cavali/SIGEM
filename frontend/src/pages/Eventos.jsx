import { useEffect, useState } from 'react'
import { del, get, patch, post, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import '../components/form/BaseField/BaseField.scss'

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

  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')

  const [fieldErrors, setFieldErrors] = useState({})

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
      setErro(error.message)
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

    setErro('')
    setSuccess('')

    setFieldErrors({})

    setFormOpen(true)
  }

  const openEdit = (item) => {

    setForm({
      nome: item.nome || '',
      descricao: item.descricao || '',
      dataInicio: item.dataInicio
      ? new Date(item.dataInicio).toISOString().slice(0, 16)
      : '',
      dataFim: item.dataFim
      ? new Date(item.dataFim).toISOString().slice(0, 16)
      : '',
      categoriaEventoId: item.categoriaEventoId || '',
      localId: item.localId || '',
      coordenadorId: item.coordenadorId || ''
    })

    setEditing(item.id)

    setErro('')
    setSuccess('')

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

    if (!form.dataInicio)
      erros.dataInicio = 'Data de início é obrigatória'

    if (!form.dataFim)
      erros.dataFim = 'Data de fim é obrigatória'

    return erros
  }

  const save = async (event) => {

    event.preventDefault()

    setErro('')
    setSuccess('')

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
          dataInicio: form.dataInicio,
          dataFim: form.dataFim,
          categoriaEventoId: form.categoriaEventoId || null,
          localId: form.localId || null,
          coordenadorId: form.coordenadorId || null
        }

        if (editing) {

          await put('/api/eventos/' + editing, payload)

          setSuccess('Evento alterado com sucesso.')

          setFormOpen(false)
        }
        else {

          await post('/api/eventos', payload)

          setSuccess('Evento criado com sucesso.')
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
          setErro(error.message)
        }
      }
    }
  }

  const abrirEvento = async (item) => {

    const confirmed = window.confirm(
      'Tem certeza que deseja abrir este evento?'
    )

    if (confirmed) {

      try {

        await patch('/api/eventos/' + item.id + '/abrir')

        setSuccess('Evento aberto com sucesso.')

        load(filtroNome, filtroStatus)
      }
      catch (error) {
        alert(error.message)
      }
    }
  }

  const cancelarEvento = async (item) => {

    const confirmed = window.confirm(
      'Tem certeza que deseja cancelar este evento?'
    )

    if (confirmed) {

      try {

        await patch('/api/eventos/' + item.id + '/cancelar')

        setSuccess('Evento cancelado com sucesso.')

        load(filtroNome, filtroStatus)
      }
      catch (error) {
        alert(error.message)
      }
    }
  }

  const openEncerrar = (item) => {

    setEncerrandoId(item.id)

    setEncerramento(initialEncerramento)

    setEncerrarOpen(true)
  }

  const encerrarEvento = async () => {

    const confirmed = window.confirm(
      'Tem certeza que deseja encerrar este evento?'
    )

    if (confirmed) {

      try {

        await patch(
          '/api/eventos/' + encerrandoId + '/encerrar',
          {
            observacoesHistorico:
              encerramento.observacoesHistorico || null,

            resultadoFinanceiro:
              encerramento.resultadoFinanceiro || null
          }
        )

        setEncerrarOpen(false)

        setSuccess('Evento encerrado com sucesso.')

        load(filtroNome, filtroStatus)
      }
      catch (error) {
        alert(error.message)
      }
    }
  }

  const remove = async (item) => {

    const confirmed = window.confirm(
      'Excluir "' + item.nome + '"?'
    )

    if (confirmed) {

      try {

        await del('/api/eventos/' + item.id)

        setItems(prev =>
          prev.filter(current => current.id !== item.id)
        )

        setSuccess('Evento excluído com sucesso.')
      }
      catch (error) {
        alert(error.message)
      }
    }
  }

  const toggleExpand = (id) => {

    setExpanded(prev => {

      if (prev.includes(id))
        return prev.filter(current => current !== id)

      return [...prev, id]
    })
  }

  useEffect(() => {

    if (!success)
      return

    const timer = setTimeout(() => {

      setSuccess('')

    }, 4000)

    return () => clearTimeout(timer)

  }, [success])

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

      {success &&
        <div className="message success">
          {success}
        </div>
      }

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

          {erro &&
            <div className="message inline">
              {erro}
            </div>
          }

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

            <div className="field-container">
              <label className="field-label">
                <span>
                  Data Início
                  <span className="required-star">*</span>
                </span>
              </label>

              <input
                type="datetime-local"
                className={
                  fieldErrors.dataInicio
                    ? 'is-invalid'
                    : ''
                }
                value={form.dataInicio}
                onChange={e =>
                  handleFieldChange(
                    'dataInicio',
                    e.target.value
                  )
                }
              />

              {fieldErrors.dataInicio &&
                <span className="field-error">
                  {fieldErrors.dataInicio}
                </span>
              }
            </div>

            <div className="field-container">
              <label className="field-label">
                <span>
                  Data Fim
                  <span className="required-star">*</span>
                </span>
              </label>

              <input
                type="datetime-local"
                className={
                  fieldErrors.dataFim
                    ? 'is-invalid'
                    : ''
                }
                value={form.dataFim}
                onChange={e =>
                  handleFieldChange(
                    'dataFim',
                    e.target.value
                  )
                }
              />

              {fieldErrors.dataFim &&
                <span className="field-error">
                  {fieldErrors.dataFim}
                </span>
              }
            </div>

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

            <div className="field-container">
              <label className="field-label">
                <span>Resultado Financeiro</span>
              </label>

              <input
                type="number"
                step="0.01"
                value={encerramento.resultadoFinanceiro}
                onChange={e =>
                  setEncerramento(prev => ({
                    ...prev,
                    resultadoFinanceiro: e.target.value
                  }))
                }
              />
            </div>

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
                    {item.dataInicio}
                  </span>

                  <span>
                    <strong>Fim:</strong>
                    {' '}
                    {item.dataFim}
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

                    <span>
                      <strong>Resultado:</strong>
                      {' '}
                      {item.resultadoFinanceiro || '-'}
                    </span>

                    <span>
                      <strong>Observações:</strong>
                      {' '}
                      {item.observacoesHistorico || '-'}
                    </span>
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