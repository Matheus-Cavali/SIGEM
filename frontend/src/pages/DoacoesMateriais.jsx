import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import { toast } from 'react-toastify'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import DateField from '../components/form/DateField'
import TextField from '../components/form/TextField'
import BaseField from '../components/form/BaseField'
import Modal from '../components/Modal'
import { dmyToISO } from '../utils/date'
import '../components/form/BaseField/BaseField.scss'
import '../components/Modal/Modal.scss'

const initialForm = { materialId: '', quantidade: '' }

export default function DoacoesMateriais() {
  const [items, setItems] = useState([])
  const [materiais, setMateriais] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroMaterial, setFiltroMaterial] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const [erroData, setErroData] = useState('')
  const [showNovoMaterial, setShowNovoMaterial] = useState(false)
  const [novoMaterialForm, setNovoMaterialForm] = useState({ nome: '', descricao: '', quantidadeEstoque: '0', categoriaMaterialId: '' })
  const [novoMaterialFieldErrors, setNovoMaterialFieldErrors] = useState({})
  const [salvandoMaterial, setSalvandoMaterial] = useState(false)
  const [editing, setEditing] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)
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
      toast.error(error.message)
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
    setForm({ ...initialForm })
    setEditing(null)
    setFieldErrors({})
    setFormOpen(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const openEdit = (item) => {
    setForm({
      materialId: String(item.materialId),
      quantidade: String(item.quantidade),
    })
    setEditing(item.id)
    setFieldErrors({})
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
          quantidade: parseInt(form.quantidade, 10),
        }

        if (editing) {
          await put('/api/doacoes-materiais/' + editing, payload)
          toast.success('Doação alterada com sucesso.')
          setFormOpen(false)
        } else {
          await post('/api/doacoes-materiais', payload)
          toast.success('Doação cadastrada com sucesso.')
        }

        setEditing(null)
        setForm(initialForm)
        load(filtroMaterial, filtroCategoria || null, filtroDataInicio, filtroDataFim)
        loadMateriais()
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          toast.error(error.message)
        }
      }
    }
  }

  const handleNovoMaterialChange = (field, value) => {
    setNovoMaterialForm(prev => ({ ...prev, [field]: value }))
    setNovoMaterialFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const salvarNovoMaterial = async () => {
    setNovoMaterialFieldErrors({})

    const erros = {}
    if (!novoMaterialForm.nome.trim()) erros.nome = 'Nome é obrigatório'
    if (!novoMaterialForm.descricao.trim()) erros.descricao = 'Descrição é obrigatória'
    if (novoMaterialForm.quantidadeEstoque === '') {
      erros.quantidadeEstoque = 'Quantidade em estoque é obrigatória'
    } else {
      const qtd = parseInt(novoMaterialForm.quantidadeEstoque, 10)
      if (qtd < 0) erros.quantidadeEstoque = 'Quantidade não pode ser negativa'
    }
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

      toast.success('Material cadastrado com sucesso.')
      setShowNovoMaterial(false)
    } catch (error) {
      if (error.fieldErrors) {
        setNovoMaterialFieldErrors(error.fieldErrors)
      } else {
        toast.error(error.message)
      }
    } finally {
      setSalvandoMaterial(false)
    }
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/doacoes-materiais/' + confirmDelete.id)
      setItems(prev => prev.filter(current => current.id !== confirmDelete.id))
      toast.success('Doação de material excluída com sucesso.')
      loadMateriais()
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

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
        <p>Excluir doação de "{confirmDelete?.materialNome}"?</p>
      </Modal>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Doação de Material' : 'Nova Doação de Material'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
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
              <button className="primary-action">{editing ? 'Salvar Alterações' : 'Salvar Doação'}</button>
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
