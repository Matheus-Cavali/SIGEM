import { useEffect, useState } from 'react'
import { get, post } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { formatarData } from '../utils/format'
import '../components/form/BaseField/BaseField.scss'

const initialForm = { materialId: '', quantidade: '' }

export default function DoacoesMateriais() {
  const [items, setItems] = useState([])
  const [materiais, setMateriais] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [erro, setErro] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroMaterial, setFiltroMaterial] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const { user, can } = useAuth()
  const canManage = can('GESTAO_DOACOES')

  const loadMateriais = async () => {
    try {
      const data = await get('/api/materiais')
      setMateriais(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const loadCategorias = async () => {
    try {
      const data = await get('/api/categorias-materiais')
      setCategorias(Array.isArray(data) ? data : [])
    } catch (_) {}
  }

  const load = async (materialNome, categoriaId, dataInicio, dataFim) => {
    try {
      let path = '/api/doacoes-materiais'
      const params = []
      if (materialNome) params.push('materialNome=' + encodeURIComponent(materialNome))
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

  useEffect(() => { load(); loadMateriais(); loadCategorias() }, [])

  const nomeCategoria = (materialId) => {
    const m = materiais.find(x => x.id === materialId)
    const c = categorias.find(x => x.id === m?.categoriaMaterialId)
    return c?.nome || ''
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      load(filtroMaterial, filtroCategoria || null, filtroDataInicio, filtroDataFim)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroMaterial, filtroCategoria, filtroDataInicio, filtroDataFim])

  const openNew = () => {
    setForm(initialForm)
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
    if (!form.materialId) erros.materialId = 'Material é obrigatório'
    if (form.quantidade === '') {
      erros.quantidade = 'Quantidade é obrigatória'
    } else if (parseInt(form.quantidade, 10) <= 0) {
      erros.quantidade = 'Quantidade deve ser maior que zero'
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
          materialId: parseInt(form.materialId, 10),
          quantidade: parseInt(form.quantidade, 10)
        }

        await post('/api/doacoes-materiais', payload)
        setSuccess('Doação cadastrada com sucesso.')
        setForm(initialForm)
        load(filtroMaterial, filtroCategoria || null, filtroDataInicio, filtroDataFim)
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
      }
    }
  }

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(timer)
  }, [success])

  return (
    <>
      <PageHeader title="Doações de Material" subtitle="Registre entradas de materiais doados a igreja" actionLabel={canManage ? 'Nova Doação' : ''} onAction={canManage ? openNew : null} />

      <section className="filter-bar">
        <select
          value={filtroMaterial}
          onChange={e => setFiltroMaterial(e.target.value)}
        >
          <option value="">Todos os materiais</option>
          {materiais.map(m => <option key={m.id} value={m.nome}>{m.nome}</option>)}
        </select>
        <select
          value={filtroCategoria}
          onChange={e => setFiltroCategoria(e.target.value)}
        >
          <option value="">Todas as categorias</option>
          {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
        <input
          type="text"
          placeholder="Data início (dd/mm/aaaa)"
          value={filtroDataInicio}
          onChange={e => setFiltroDataInicio(formatarData(e.target.value))}
        />
        <input
          type="text"
          placeholder="Data fim (dd/mm/aaaa)"
          value={filtroDataFim}
          onChange={e => setFiltroDataFim(formatarData(e.target.value))}
        />
      </section>

      {success && <div className="message success">{success}</div>}

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>Nova Doação de Material</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save} noValidate>
            <div className="field-container">
              <label className="field-label"><span>Material <span className="required-star">*</span></span></label>
              <select className={'field-control' + (fieldErrors.materialId ? ' is-invalid' : '')}
                      value={form.materialId} onChange={e => handleFieldChange('materialId', e.target.value)}>
                <option value="">Selecione...</option>
                {materiais.map(m => <option key={m.id} value={m.id}>{m.nome} (Estoque: {m.quantidadeEstoque})</option>)}
              </select>
              {fieldErrors.materialId && <span className="field-error">{fieldErrors.materialId}</span>}
            </div>
            <div className="field-container">
              <label className="field-label"><span>Quantidade <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.quantidade ? ' is-invalid' : '')}
                     value={form.quantidade} onChange={e => handleFieldChange('quantidade', e.target.value)} placeholder="0" type="number" min="1" />
              {fieldErrors.quantidade && <span className="field-error">{fieldErrors.quantidade}</span>}
            </div>
            <div className="form-submit">
              <button className="primary-action">Salvar Doação</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.materialNome}</h3>
              <div className="meta-row">
                <span><Icon name="box" size={14} /> Qtd: {item.quantidade}</span>
                <span>Cat: {nomeCategoria(item.materialId)}</span>
                <span>Data do registro: {item.dataFormatada}</span>
                <span>Registrado por: {item.colaboradorNome}</span>
              </div>
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
