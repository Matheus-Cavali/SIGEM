import { useEffect, useState } from 'react'
import { del, get, post, put, request } from '../api/http'
import { useAuth } from '../state/AuthContext'
import { formatarCpf } from '../utils/format'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'

const initialForm = {
  nome: '', email: '', senha: '', cpf: '', rg: '',
  celular: '', rua: '', bairro: '', cep: '', cidade: '', estado: '',
  tipoUsuario: 'colaborador', nivelAcesso: '2',
}

export default function Usuarios() {
  const { user, can } = useAuth()
  const podeGerenciar = can('GESTAO_USUARIOS') || user?.nivelAcesso === 1

  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')

  const load = async (nome, tipo) => {
    try {
      let path = '/api/usuarios'
      const params = []
      if (nome) params.push('nome=' + encodeURIComponent(nome))
      if (tipo) params.push('tipo=' + tipo)
      if (params.length) path += '?' + params.join('&')
      const data = await get(path)
      setItems(Array.isArray(data) ? data : [])
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const timer = setTimeout(() => load(filtroNome, filtroTipo || null), 300)
    return () => clearTimeout(timer)
  }, [filtroNome, filtroTipo])

  const openNew = () => {
    setForm(initialForm)
    setEditing(null)
    setErro('')
    setFormOpen(true)
  }

  const openEdit = (item) => {
    setForm({
      nome: item.nome || '',
      email: item.email || '',
      senha: '',
      cpf: item.cpf || '',
      rg: item.rg || '',
      celular: item.celular || '',
      rua: item.rua || '',
      bairro: item.bairro || '',
      cep: item.cep || '',
      cidade: item.cidade || '',
      estado: item.estado || '',
      tipoUsuario: item.tipoUsuario || 'colaborador',
      nivelAcesso: String(item.nivelAcesso ?? '2'),
    })
    setEditing(item.id)
    setErro('')
    setFormOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setErro('')

    try {
      if (!form.nome) throw new Error('Nome é obrigatório')
      if (!form.email) throw new Error('Email é obrigatório')
      if (!form.senha && !editing) throw new Error('Senha é obrigatória')
      if (!form.cpf) throw new Error('CPF é obrigatório')
      if (!editing && form.senha.length < 4) throw new Error('Senha deve ter no mínimo 4 caracteres')

      const payload = {
        nome: form.nome,
        email: form.email,
        cpf: form.cpf,
        rg: form.rg || null,
        celular: form.celular || null,
        rua: form.rua || null,
        bairro: form.bairro || null,
        cep: form.cep || null,
        cidade: form.cidade || null,
        estado: form.estado || null,
        tipoUsuario: form.tipoUsuario,
        nivelAcesso: parseInt(form.nivelAcesso, 10),
      }

      if (editing) {
        await put('/api/usuarios/' + editing, payload)
      } else {
        await post('/api/cadastrar-interno', { ...payload, senha: form.senha })
      }

      setFormOpen(false)
      setEditing(null)
      setForm(initialForm)
      load(filtroNome, filtroTipo || null)
    } catch (error) {
      setErro(error.message)
    }
  }

  const toggleStatus = async (item) => {
    const novoStatus = !item.statusAtivo
    const acao = novoStatus ? 'reativar' : 'desativar'
    const confirmado = window.confirm(acao === 'desativar'
      ? 'Desativar usuario "' + item.nome + '"? Ele nao podera acessar o sistema.'
      : 'Reativar usuario "' + item.nome + '"?')
    if (!confirmado) return

    try {
      await request('/api/usuarios/' + item.id + '/status', {
        method: 'PATCH',
        body: JSON.stringify({ status_ativo: novoStatus }),
      })
      setItems(prev => prev.map(u => u.id === item.id ? { ...u, statusAtivo: novoStatus } : u))
    } catch (error) {
      alert(error.message)
    }
  }

  const remove = async (item) => {
    const confirmado = window.confirm('Excluir permanentemente o usuario "' + item.nome + '"?')
    if (!confirmado) return

    try {
      await del('/api/usuarios/' + item.id)
      setItems(prev => prev.filter(u => u.id !== item.id))
    } catch (error) {
      alert(error.message)
    }
  }

  const handleCpfChange = (e) => {
    setForm(prev => ({ ...prev, cpf: formatarCpf(e.target.value) }))
  }

  const nivelLabel = (nivel) => {
    if (nivel === 1) return 'Admin'
    if (nivel === 2) return 'Usuario'
    return 'Nivel ' + nivel
  }

  return (
    <>
      <PageHeader title="Usuarios" subtitle="Gerencie os usuarios do sistema"
                  actionLabel={podeGerenciar ? 'Adicionar Usuario' : null} onAction={podeGerenciar ? openNew : null} />

      <section className="filter-bar">
        <input placeholder="Filtrar por nome..." value={filtroNome}
               onChange={e => setFiltroNome(e.target.value)} />
        <select value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}>
          <option value="">Todos os tipos</option>
          <option value="colaborador">Colaboradores</option>
          <option value="voluntario">Voluntarios</option>
        </select>
      </section>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Usuario' : 'Novo Usuario'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          {erro && <div className="message inline">{erro}</div>}
          <form className="inline-form" onSubmit={save}>
            <label>
              <span>Nome</span>
              <input value={form.nome} onChange={e => setForm(prev => ({ ...prev, nome: e.target.value }))}
                     placeholder="Nome completo" />
            </label>
            <label>
              <span>Email</span>
              <input type="email" value={form.email} onChange={e => setForm(prev => ({ ...prev, email: e.target.value }))}
                     placeholder="email@exemplo.com" />
            </label>
            {!editing && (
              <label>
                <span>Senha</span>
                <input type="password" value={form.senha} onChange={e => setForm(prev => ({ ...prev, senha: e.target.value }))}
                       placeholder="Minimo 4 caracteres" />
              </label>
            )}
            <label>
              <span>CPF</span>
              <input value={form.cpf} onChange={handleCpfChange} placeholder="000.000.000-00" maxLength={14} />
            </label>
            <label>
              <span>RG</span>
              <input value={form.rg} onChange={e => setForm(prev => ({ ...prev, rg: e.target.value }))}
                     placeholder="RG" />
            </label>
            <label>
              <span>Celular</span>
              <input value={form.celular} onChange={e => setForm(prev => ({ ...prev, celular: e.target.value }))}
                     placeholder="(00) 00000-0000" />
            </label>
            <label>
              <span>CEP</span>
              <input value={form.cep} onChange={e => setForm(prev => ({ ...prev, cep: e.target.value }))}
                     placeholder="00000-000" />
            </label>
            <label>
              <span>Rua</span>
              <input value={form.rua} onChange={e => setForm(prev => ({ ...prev, rua: e.target.value }))}
                     placeholder="Rua, numero" />
            </label>
            <label>
              <span>Bairro</span>
              <input value={form.bairro} onChange={e => setForm(prev => ({ ...prev, bairro: e.target.value }))}
                     placeholder="Bairro" />
            </label>
            <label>
              <span>Cidade</span>
              <input value={form.cidade} onChange={e => setForm(prev => ({ ...prev, cidade: e.target.value }))}
                     placeholder="Cidade" />
            </label>
            <label>
              <span>Estado</span>
              <input value={form.estado} onChange={e => setForm(prev => ({ ...prev, estado: e.target.value }))}
                     placeholder="Estado (UF)" maxLength={2} />
            </label>
            <label>
              <span>Tipo de Usuario</span>
              <select value={form.tipoUsuario} onChange={e => setForm(prev => ({ ...prev, tipoUsuario: e.target.value }))}>
                <option value="colaborador">Colaborador</option>
                <option value="voluntario">Voluntario</option>
              </select>
            </label>
            <label>
              <span>Nivel de Acesso</span>
              <select value={form.nivelAcesso} onChange={e => setForm(prev => ({ ...prev, nivelAcesso: e.target.value }))}>
                <option value="2">Usuario</option>
                <option value="1">Administrador</option>
              </select>
            </label>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Usuario'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome}</h3>
              <div className="meta-row">
                <span><Icon name="users" size={14} /> {item.email}</span>
                {item.cpf && <span>CPF: {formatarCpf(item.cpf)}</span>}
                <span>{item.tipoUsuario}</span>
                <span>{nivelLabel(item.nivelAcesso)}</span>
                <span className={'status-badge ' + (item.statusAtivo ? 'status-ativo' : 'status-inativo')}>
                  {item.statusAtivo ? 'Ativo' : 'Inativo'}
                </span>
                {item.primeiroAcesso && <span className="status-badge status-pendente">1o Acesso</span>}
              </div>
            </div>
            {podeGerenciar && (
              <div className="card-actions">
                <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>
                <button className={'icon-button ' + (item.statusAtivo ? 'icon-button--warning' : 'icon-button--success')}
                        onClick={() => toggleStatus(item)}
                        title={item.statusAtivo ? 'Desativar' : 'Reativar'}>
                  <Icon name="close" size={16} />
                </button>
                <button className="icon-button icon-button--danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>
              </div>
            )}
          </article>
        ))}
        {!items.length && <p className="empty-state">Nenhum usuario encontrado.</p>}
      </div>
    </>
  )
}
