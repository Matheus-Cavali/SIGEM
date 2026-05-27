import { useEffect, useState } from 'react'
import { del, get, post } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import DateField from '../components/form/DateField'
import TextField from '../components/form/TextField'
import BaseField from '../components/form/BaseField'
import { dmyToISO } from '../utils/date'
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
  const [erroData, setErroData] = useState('')
  const [showNovoMaterial, setShowNovoMaterial] = useState(false)
  const [novoMaterialForm, setNovoMaterialForm] = useState({ nome: '', descricao: '', quantidadeEstoque: '0', categoriaMaterialId: '' })
  const [novoMaterialErro, setNovoMaterialErro] = useState('')
  const [novoMaterialFieldErrors, setNovoMaterialFieldErrors] = useState({})
  const [salvandoMaterial, setSalvandoMaterial] = useState(false)
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
        load(filtroMaterial, filtroCategoria || null, filtroDataInicio, filtroDataFim)
      }
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
        loadMateriais()
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
      }
    }
  }

  const handleNovoMaterialChange = (field, value) => {
    setNovoMaterialForm(prev => ({ ...prev, [field]: value }))
    setNovoMaterialFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const salvarNovoMaterial = async () => {
    setNovoMaterialErro('')
    setNovoMaterialFieldErrors({})

    const erros = {}
    if (!novoMaterialForm.nome.trim()) erros.nome = 'Nome é obrigatório'
    if (!novoMaterialForm.descricao.trim()) erros.descricao = 'Descrição é obrigatória'
    const qtd = parseInt(novoMaterialForm.quantidadeEstoque, 10)
    if (qtd < 0) erros.quantidadeEstoque = 'Quantidade não pode ser negativa'
    if (!novoMaterialForm.categoriaMaterialId) erros.categoriaMaterialId = 'Categoria é obrigatória'

    if (Object.keys(erros).length > 0) {
      setNovoMaterialFieldErrors(erros)
      return
    }

    setSalvandoMaterial(true)
    try {
      const payload = {
        nome: novoMaterialForm.nome,
        descricao: novoMaterialForm.descricao,
        quantidadeEstoque: parseInt(novoMaterialForm.quantidadeEstoque, 10) || 0,
        categoriaMaterialId: parseInt(novoMaterialForm.categoriaMaterialId, 10),
      }
      const data = await post('/api/materiais', payload)
      await loadMateriais()

      const novoId = data?.id
      if (novoId) {
        setForm(prev => ({ ...prev, materialId: String(novoId) }))
        setFieldErrors(prev => ({ ...prev, materialId: '' }))
      }

      setSuccess('Material cadastrado com sucesso.')
      setShowNovoMaterial(false)
    } catch (error) {
      if (error.fieldErrors) {
        setNovoMaterialFieldErrors(error.fieldErrors)
      } else {
        setNovoMaterialErro(error.message)
      }
    } finally {
      setSalvandoMaterial(false)
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir doação de "' + item.materialNome + '"?')

    if (confirmed) {
      try {
        await del('/api/doacoes-materiais/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
        setSuccess('Doação de material excluída com sucesso.')
        loadMateriais()
      } catch (error) {
        alert(error.message)
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
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
                <select className={'field-control' + (fieldErrors.materialId ? ' is-invalid' : '')}
                        value={form.materialId} onChange={e => handleFieldChange('materialId', e.target.value)} style={{ flex: 1 }}>
                  <option value="">Selecione...</option>
                  {materiais.map(m => <option key={m.id} value={m.id}>{m.nome} (Estoque: {m.quantidadeEstoque})</option>)}
                </select>
                {canManage && (
                  <button type="button" className="icon-button" onClick={() => setShowNovoMaterial(o => !o)} title={showNovoMaterial ? 'Fechar' : 'Cadastrar novo material'}>
                    <Icon name={showNovoMaterial ? 'minus' : 'plus'} size={18} />
                  </button>
                )}
              </div>
              {fieldErrors.materialId && <span className="field-error">{fieldErrors.materialId}</span>}
            </div>

            <div className="field-container">
              <label className="field-label"><span>Quantidade <span className="required-star">*</span></span></label>
              <input className={'field-control' + (fieldErrors.quantidade ? ' is-invalid' : '')}
                     value={form.quantidade} onChange={e => handleFieldChange('quantidade', e.target.value)} placeholder="0" type="number" min="1" />
              {fieldErrors.quantidade && <span className="field-error">{fieldErrors.quantidade}</span>}
            </div>

            {showNovoMaterial && (
              <div style={{
                gridColumn: '1 / -1',
                maxWidth: '600px',
                background: 'var(--color-surface, #fff)',
                border: '1px solid var(--color-border, #e5e7eb)',
                borderRadius: '8px',
                padding: '1rem',
                marginTop: '0.5rem',
              }}>
                <div style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  marginBottom: '0.75rem',
                }}>
                  <strong style={{ fontSize: '0.875rem' }}>Novo Material</strong>
                </div>
                {novoMaterialErro && <div className="message inline">{novoMaterialErro}</div>}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  <TextField label="Nome" required value={novoMaterialForm.nome}
                    setValue={v => handleNovoMaterialChange('nome', v)}
                    placeholder="Nome do material" error={novoMaterialFieldErrors.nome} />
                  <TextField label="Descrição" required value={novoMaterialForm.descricao}
                    setValue={v => handleNovoMaterialChange('descricao', v)}
                    placeholder="Descrição do material" error={novoMaterialFieldErrors.descricao} />
                  <BaseField label="Quantidade em Estoque" error={novoMaterialFieldErrors.quantidadeEstoque}>
                    <input className="field-control" type="number" min="0"
                      value={novoMaterialForm.quantidadeEstoque}
                      onChange={e => handleNovoMaterialChange('quantidadeEstoque', e.target.value)} />
                  </BaseField>
                  <BaseField label="Categoria" required error={novoMaterialFieldErrors.categoriaMaterialId}>
                    <select className="field-control"
                      value={novoMaterialForm.categoriaMaterialId}
                      onChange={e => handleNovoMaterialChange('categoriaMaterialId', e.target.value)}>
                      <option value="">Selecione...</option>
                      {categorias.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
                    </select>
                  </BaseField>
                  <div className="form-submit">
                    <button type="button" className="primary-action" disabled={salvandoMaterial}
                      onClick={salvarNovoMaterial}>
                      {salvandoMaterial ? 'Salvando...' : 'Salvar Material'}
                    </button>
                  </div>
                </div>
              </div>
            )}
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
                <span><Icon name="box" size={14} /> <strong>Qtd:</strong> {item.quantidade}</span>
                <span><strong>Categoria:</strong> {nomeCategoria(item.materialId)}</span>
                <span><strong>Data do registro:</strong> {item.dataFormatada}</span>
                <span><strong>Registrado por:</strong> {item.colaboradorNome}</span>
              </div>
            </div>
            <div className="card-actions">
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
