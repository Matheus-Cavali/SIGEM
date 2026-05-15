import { useEffect, useState } from 'react'
import { del, get, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'

const initialForm = { nome: '', descricao: '', quantidadeEstoque: '', categoriaMaterialId: '' }

export default function Materiais() {
  const [items, setItems] = useState([])
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
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      const quantidade = parseInt(form.quantidadeEstoque, 10)

      if (!form.nome || isNaN(quantidade)) {
        throw new Error('Nome e quantidade sao obrigatorios')
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
      <PageHeader title="Materiais" subtitle="Controle materiais e recursos fisicos da igreja" actionLabel="Adicionar Material" onAction={openNew} />

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
          <option value="1">Categoria 1</option>
          <option value="2">Categoria 2</option>
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
                <option value="1">Categoria 1</option>
                <option value="2">Categoria 2</option>
              </select>
            </label>
            <div className="form-submit">
              <button className="danger-action">{editing ? 'Salvar Alteracoes' : 'Salvar Material'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map((item, index) => (
          <article className={'finance-card ' + (index % 2 ? 'accent-red' : '')} key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                {item.descricao && <span><Icon name="box" size={14} /> {item.descricao}</span>}
                <span>Estoque: {item.quantidadeEstoque}</span>
                {item.categoriaMaterialId && <span>Cat: {item.categoriaMaterialId}</span>}
              </div>
            </div>
            <div className="card-actions">
              <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>
              <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
