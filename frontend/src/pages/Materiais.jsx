import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { useAuth } from '../state/AuthContext'

const initialForm = { nome: '', descricao: '', quantidadeEstoque: '', categoriaMaterialId: '' }

export default function Materiais() {
  const { user, can } = useAuth()
  const podeGerenciar = user?.nivelAcesso === 1 || can('GESTAO_DOACOES')
  const [items, setItems] = useState([])
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')

  const load = async (nome, categoriaId) => {
    try {
      let path = '/api/materiais'
      const params = []
      if (nome) params.push('nome=' + encodeURIComponent(nome))
      if (categoriaId) params.push('categoriaId=' + categoriaId)
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  const carregarCategorias = () => {
    get('/api/categorias-materiais').then(data => {
      setCategorias(Array.isArray(data) ? data : [])
    }).catch(() => {})
  }

  useEffect(carregarCategorias, [])

  useEffect(() => {
    const timer = setTimeout(() => {
      load(filtroNome, filtroCategoria || null)
    }, 300)
    return () => clearTimeout(timer)
  }, [filtroNome, filtroCategoria])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFormOpen(true)
    carregarCategorias()
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
      descricao: item.descricao || '',
      quantidadeEstoque: String(item.quantidadeEstoque ?? ''),
      categoriaMaterialId: String(item.categoriaMaterialId ?? ''),
    })
    setEditing(item.id)
    setErro('')
    setFormOpen(true)
    carregarCategorias()
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      const quantidade = parseInt(form.quantidadeEstoque, 10)

      if (!form.nome || isNaN(quantidade)) {
        throw new Error('Nome e quantidade sao obrigatÃ³rios')
      }

      const payload = {
        nome: form.nome,
        descricao: form.descricao,
        quantidadeEstoque: quantidade,
        categoriaMaterialId: form.categoriaMaterialId ? parseInt(form.categoriaMaterialId, 10) : null,
      }

      if (editing) {
        await put('/api/materiais/' + editing, payload)
      } else {
        await post('/api/materiais', payload)
      }

      setFormOpen(false)
      setEditing(null)
      setForm(initialForm)
      load(filtroNome, filtroCategoria || null)
    } catch (error) {
      setErro(error.message)
    }
  }

  const remove = async (item) => {
    const confirmed = window.confirm('Excluir "' + item.nome + '"?')

    if (confirmed) {
      try {
        await del('/api/materiais/' + item.id)
        setItems(prev => prev.filter(current => current.id !== item.id))
      } catch (error) {
        alert(error.message)
      }
    }
  }

  return (
    <>
      <PageHeader title="Materiais" subtitle="Controle materiais e recursos fisicos da igreja" actionLabel={podeGerenciar ? 'Adicionar Material' : ''} onAction={openNew} />

      <section className="filter-bar">
        <input
          placeholder="Filtrar por nome..."
          value={filtroNome}
          onChange={e => setFiltroNome(e.target.value)}
        />
        <select
          value={filtroCategoria}
          onChange={e => setFiltroCategoria(e.target.value)}
        >
          <option value="">Todas as categorias</option>
          {categorias.map(cat => (
            <option key={cat.id} value={cat.id}>{cat.nome}</option>
          ))}
        </select>
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Material' : 'Novo Material'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Nome</span>
              <input value={form.nome} onChange={e => setForm(prev => ({ ...prev, nome: e.target.value }))} placeholder="Nome do material" />
            </label>
            <label>
              <span>Descricao</span>
              <input value={form.descricao} onChange={e => setForm(prev => ({ ...prev, descricao: e.target.value }))} placeholder="Descricao do material" />
            </label>
            <label>
              <span>Quantidade em Estoque</span>
              <input value={form.quantidadeEstoque} onChange={e => setForm(prev => ({ ...prev, quantidadeEstoque: e.target.value }))} placeholder="0" />
            </label>
            <label>
              <span>Categoria</span>
              <select value={form.categoriaMaterialId} onChange={e => setForm(prev => ({ ...prev, categoriaMaterialId: e.target.value }))}>
                <option value="">Selecione...</option>
                {categorias.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.nome}</option>
                ))}
              </select>
            </label>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar AlteraÃ§Ãµes' : 'Salvar Material'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map((item, index) => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                {item.descricao && <span><Icon name="box" size={14} /> {item.descricao}</span>}
                <span>Estoque: {item.quantidadeEstoque}</span>
                {categorias.find(c => c.id === item.categoriaMaterialId) && <span>{categorias.find(c => c.id === item.categoriaMaterialId).nome}</span>}
              </div>
            </div>
            <div className="card-actions">
              {podeGerenciar && <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>}
              {podeGerenciar && <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>}
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
