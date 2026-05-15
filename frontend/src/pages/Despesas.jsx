import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { formatarData, moeda, valorParaNumero } from '../utils/format'
import { useAuth } from '../state/AuthContext'

const initialForm = { descricao: '', valor: '', dataVencimento: '', categoriaDespesaId: '' }

export default function Despesas() {
  const [items, setItems] = useState([])
  const [tipos, setTipos] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [filtroDescricao, setFiltroDescricao] = useState('')
  const { can } = useAuth()
  const canLancar = can('LANCAR_DESPESA')
  const canManage = can('GERENCIAR_DESPESA')

  const load = async (descricao) => {
    try {
      let path = '/api/despesas'
      if (descricao) path += '?descricao=' + encodeURIComponent(descricao)
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  const loadTipos = async () => {
    try {
      const data = await get('/api/categorias-despesa')
      setTipos(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error('Erro ao carregar categorias de despesa', error)
    }
  }

  useEffect(() => { load(); loadTipos() }, [])

  useEffect(() => {
    const timer = setTimeout(() => { load(filtroDescricao) }, 300)
    return () => clearTimeout(timer)
  }, [filtroDescricao])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFormOpen(true)
  }

  const openEdit = async (item) => {
    setErro('')
    try {
      const data = await get('/api/despesas/' + item.id)
      setForm({
        descricao: data.descricao || '',
        valor: String(data.valor || '').replace('.', ','),
        dataVencimento: data.dataVencimento || '',
        categoriaDespesaId: String(data.categoriaDespesaId || ''),
      })
      setEditing(item.id)
      setFormOpen(true)
    } catch (error) {
      setErro(error.message)
    }
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      const value = valorParaNumero(form.valor)

      if (!form.descricao || !value) {
        throw new Error('Descricao e valor sao obrigatorios')
      }
      if (!form.categoriaDespesaId) {
        throw new Error('Selecione uma categoria de despesa')
      }
      if (!form.dataVencimento) {
        throw new Error('Data de vencimento é obrigatoria')
      }

      const payload = {
        descricao: form.descricao,
        valor: value,
        categoriaDespesaId: Number(form.categoriaDespesaId),
        dataVencimento: form.dataVencimento,
      }

      if (editing) {
        await put('/api/despesas/' + editing, payload)
      } else {
        await post('/api/despesas', payload)
      }

      setFormOpen(false)
      setEditing(null)
      setForm(initialForm)
      load(filtroDescricao)
    } catch (error) {
      setErro(error.message)
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir despesa "' + item.descricao + '"?')

    if (confirmed) {
      try {
        await del('/api/despesas/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader title="Despesas" subtitle="Gerencie as despesas da igreja" actionLabel={canLancar || canManage ? 'Adicionar Despesa' : ''} onAction={openNew} />

      <section className="filter-bar">
        <input placeholder="Filtrar por descricao..." value={filtroDescricao} onChange={e => setFiltroDescricao(e.target.value)} />
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Despesa' : 'Nova Despesa'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Descricao</span>
              <input value={form.descricao} onChange={e => setForm(prev => ({ ...prev, descricao: e.target.value }))} placeholder="Descricao da despesa" />
            </label>
            <label>
              <span>Valor (R$)</span>
              <input value={form.valor} onChange={e => setForm(prev => ({ ...prev, valor: e.target.value }))} placeholder="0,00" />
            </label>
            <label>
              <span>Categoria de Despesa</span>
              <select value={form.categoriaDespesaId} onChange={e => setForm(prev => ({ ...prev, categoriaDespesaId: e.target.value }))}>
                <option value="">Selecione...</option>
                {tipos.map(t => (
                  <option key={t.id} value={t.id}>{t.nome}</option>
                ))}
              </select>
            </label>
            <label>
              <span>Data Vencimento</span>
              <input value={form.dataVencimento} onChange={e => setForm(prev => ({ ...prev, dataVencimento: formatarData(e.target.value) }))} placeholder="dd/mm/aaaa" />
            </label>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Despesa'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.descricao}</h3>
              <div className="meta-row">
                <span><Icon name="dollar" size={15} /> {moeda(item.valor)}</span>
                <span><Icon name="calendar" size={15} /> {item.dataVencimento}</span>
                <span className="tag">{item.categoriaDespesaNome}</span>
              </div>
            </div>
            <div className="card-actions">
              {canManage && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {canManage && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
