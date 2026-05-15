import { useEffect, useState } from 'react'
import { del, get, patch, post, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { formatarCpf } from '../utils/format'
import { useAuth } from '../state/AuthContext'

const initialForm = {
  nome: '', email: '', senha: '', confirmarSenha: '',
  cpf: '', rg: '', celular: '',
  rua: '', bairro: '', cep: '', cidade: '', estado: '',
  nivelAcesso: 2, tipoUsuario: 'colaborador',
  statusAtivo: true, dataDesligamento: '',
  usarDataAtual: true,
}

export default function Usuarios() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [filtro, setFiltro] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [erro, setErro] = useState('')

  const load = async () => {
    try {
      const params = {}
      if (filtro) params.nome = filtro
      if (filtroTipo) params.tipo = filtroTipo
      const data = await get('/api/usuarios', { params })
      let lista = Array.isArray(data) ? data : []
      if (filtroStatus === 'ativo') lista = lista.filter(i => i.statusAtivo)
      if (filtroStatus === 'inativo') lista = lista.filter(i => !i.statusAtivo)
      setItems(lista)
    } catch (error) { setErro(error.message) }
  }

  useEffect(() => { load() }, [filtro, filtroTipo, filtroStatus])

  const openNew = () => {
    setForm(initialForm); setEditing(null); setErro(''); setFormOpen(true)
  }

  const openEdit = async (item) => {
    setErro('')
    try {
      const data = await get('/api/usuarios/' + item.id)
      setForm({
        nome: data.nome || '', email: data.email || '',
        senha: '', confirmarSenha: '',
        cpf: data.cpf || '', rg: data.rg || '', celular: data.celular || '',
        rua: data.rua || '', bairro: data.bairro || '', cep: data.cep || '',
        cidade: data.cidade || '', estado: data.estado || '',
        nivelAcesso: data.nivelAcesso || 2,
        tipoUsuario: data.tipoUsuario || 'colaborador',
        statusAtivo: data.statusAtivo !== false,
        dataDesligamento: '',
        usarDataAtual: true,
      })
      setEditing(item.id); setFormOpen(true)
    } catch (error) { setErro(error.message) }
  }

  const save = async (event) => {
    event.preventDefault(); setErro('')
    try {
      if (!form.nome || !form.email) throw new Error('Nome e email sao obrigatorios')

      if (editing) {
        await put('/api/usuarios/' + editing, form)
        if (form.statusAtivo !== undefined) {
          await patch('/api/usuarios/' + editing + '/status', {
            status_ativo: form.statusAtivo,
            data_desligamento: form.statusAtivo ? undefined : (form.usarDataAtual ? 'SYSDATE' : form.dataDesligamento.replace(/\D/g, '')),
          })
        }
      } else {
        if (!form.senha || form.senha.length < 4) throw new Error('Senha deve ter no minimo 4 caracteres')
        if (form.senha !== form.confirmarSenha) throw new Error('Senhas nao conferem')
        await post('/api/cadastrar-interno', {
          nome: form.nome, email: form.email, senha: form.senha,
          cpf: form.cpf ? form.cpf.replace(/\D/g, '') : '',
          nivelAcesso: form.nivelAcesso, tipoUsuario: form.tipoUsuario, data: '',
        })
      }

      setFormOpen(false); setEditing(null); setForm(initialForm)
    } catch (error) { setErro(error.message) }
  }

  const remove = async (item) => {
    if (!window.confirm('Excluir "' + (item.nome || '') + '"?')) return
    setItems(prev => prev.filter(x => x.id !== item.id))
    try { await del('/api/usuarios/' + item.id) }
    catch (error) { setErro(error.message) }
  }

  return (
    <>
      <PageHeader title="Usuarios" subtitle="Gerencie os usuarios do sistema" actionLabel="Novo Usuario" onAction={openNew} />
      {erro && <div className="message">{erro}</div>}

      <div style={{ display: 'flex', gap: 10, marginBottom: 20, flexWrap: 'wrap', alignItems: 'center' }}>
        <input placeholder="Pesquisar por nome, CPF ou email..." value={filtro} onChange={e => setFiltro(e.target.value)}
          style={{ flex: 1, minWidth: 200, padding: '8px 10px', border: '1px solid #E0E0E0', borderRadius: 4, fontSize: 13 }} />
        <select value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}
          style={{ padding: '8px 10px', border: '1px solid #E0E0E0', borderRadius: 4, fontSize: 13, minWidth: 140 }}>
          <option value="">Todos os tipos</option>
          <option value="colaborador">Colaborador</option>
          <option value="voluntario">Voluntario</option>
        </select>
        <select value={filtroStatus} onChange={e => setFiltroStatus(e.target.value)}
          style={{ padding: '8px 10px', border: '1px solid #E0E0E0', borderRadius: 4, fontSize: 13, minWidth: 140 }}>
          <option value="">Todos os status</option>
          <option value="ativo">Ativo</option>
          <option value="inativo">Inativo</option>
        </select>
      </div>

      {formOpen && (
        <section className="editor-card">
          <div className="editor-title">
            <h2>{editing ? 'Editar Usuario' : 'Novo Usuario'}</h2>
            <button className="ghost-icon" onClick={() => setFormOpen(false)}><Icon name="close" size={16} /></button>
          </div>
          <form className="inline-form" onSubmit={save}>
            <label><span>Nome *</span><input value={form.nome} onChange={e => setForm(p => ({ ...p, nome: e.target.value }))} /></label>
            <label><span>Email *</span><input value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} /></label>
            <label><span>CPF *</span><input value={form.cpf} onChange={e => setForm(p => ({ ...p, cpf: formatarCpf(e.target.value) }))} maxLength={14} /></label>
            <label><span>RG</span><input value={form.rg} onChange={e => setForm(p => ({ ...p, rg: e.target.value }))} /></label>
            <label><span>Celular</span><input value={form.celular} onChange={e => setForm(p => ({ ...p, celular: e.target.value }))} /></label>
            <label><span>Tipo</span>
              <select value={form.tipoUsuario} onChange={e => setForm(p => ({ ...p, tipoUsuario: e.target.value }))}>
                <option value="colaborador">Colaborador</option>
                <option value="voluntario">Voluntario</option>
              </select>
            </label>
            <label><span>Nivel</span>
              <select value={form.nivelAcesso} onChange={e => setForm(p => ({ ...p, nivelAcesso: parseInt(e.target.value) }))}>
                <option value={2}>Restrito</option>
                <option value={1}>Total</option>
              </select>
            </label>
            {editing && (
              <>
                <label><span>Ativo</span>
                  <select value={form.statusAtivo ? '1' : '0'} onChange={e => setForm(p => ({ ...p, statusAtivo: e.target.value === '1' }))}>
                    <option value="1">Sim</option>
                    <option value="0">Nao</option>
                  </select>
                </label>
                {!form.statusAtivo && (
                  <label className="full" style={{ marginTop: 4 }}>
                    <span style={{ fontSize: 11, color: '#888', marginBottom: 8, display: 'block' }}>Data de desligamento</span>
                    <div style={{ display: 'flex', gap: 10, marginBottom: 8, flexWrap: 'wrap' }}>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, padding: '6px 12px', border: '1px solid ' + (form.usarDataAtual ? '#2A5C82' : '#E0E0E0'), borderRadius: 4, background: form.usarDataAtual ? 'rgba(42,92,130,0.05)' : '#fff' }}>
                        <input type="radio" checked={form.usarDataAtual} onChange={() => setForm(p => ({ ...p, usarDataAtual: true }))} />
                        Hoje
                      </label>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, padding: '6px 12px', border: '1px solid ' + (!form.usarDataAtual ? '#2A5C82' : '#E0E0E0'), borderRadius: 4, background: !form.usarDataAtual ? 'rgba(42,92,130,0.05)' : '#fff' }}>
                        <input type="radio" checked={!form.usarDataAtual} onChange={() => setForm(p => ({ ...p, usarDataAtual: false }))} />
                        Digitar
                      </label>
                    </div>
                    {!form.usarDataAtual && (
                      <input placeholder="ddmmaaaa ou dd/mm/aaaa" value={form.dataDesligamento} onChange={e => setForm(p => ({ ...p, dataDesligamento: e.target.value }))}
                        style={{ width: '100%', padding: '8px 10px', border: '1px solid #E0E0E0', borderRadius: 4, fontSize: 13 }} autoFocus />
                    )}
                  </label>
                )}
              </>
            )}

            <label className="full" style={{ marginTop: 16, paddingTop: 12, borderTop: '1px solid #E0E0E0' }}>
              <span style={{ fontSize: 11, color: '#888' }}>Endereco (opcional)</span>
            </label>
            <label><span>Rua</span><input value={form.rua} onChange={e => setForm(p => ({ ...p, rua: e.target.value }))} /></label>
            <label><span>Bairro</span><input value={form.bairro} onChange={e => setForm(p => ({ ...p, bairro: e.target.value }))} /></label>
            <label><span>CEP</span><input value={form.cep} onChange={e => setForm(p => ({ ...p, cep: e.target.value }))} /></label>
            <label><span>Cidade</span><input value={form.cidade} onChange={e => setForm(p => ({ ...p, cidade: e.target.value }))} /></label>
            <label><span>Estado</span><input value={form.estado} onChange={e => setForm(p => ({ ...p, estado: e.target.value }))} maxLength={2} /></label>

            {!editing && (
              <>
                <label><span>Senha *</span><input type="password" value={form.senha} onChange={e => setForm(p => ({ ...p, senha: e.target.value }))} /></label>
                <label><span>Confirmar Senha *</span><input type="password" value={form.confirmarSenha} onChange={e => setForm(p => ({ ...p, confirmarSenha: e.target.value }))} /></label>
              </>
            )}

            <div className="form-submit" style={{ justifyContent: 'flex-start', gap: 10 }}>
              <button className="danger-action">{editing ? 'Salvar' : 'Cadastrar'}</button>
              <button type="button" onClick={() => setFormOpen(false)}
                style={{ padding: '9px 20px', border: '1px solid #E0E0E0', borderRadius: 4, background: '#fff', cursor: 'pointer', fontSize: 13 }}>
                Cancelar
              </button>
            </div>
          </form>
        </section>
      )}

      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome ? item.nome.replace(/"/g, '') : ''}</h3>
              <div className="meta-row">
                <span>{item.email ? item.email.replace(/"/g, '') : ''}</span>
                <span>{item.cpf ? formatarCpf(item.cpf.replace(/"/g, '')) : ''}</span>
                <span>{item.tipoUsuario ? item.tipoUsuario.replace(/"/g, '') : ''}</span>
                <span className={'badge ' + (item.statusAtivo ? 'badge-success' : 'badge-error')}>{item.statusAtivo ? 'Ativo' : 'Inativo'}</span>
              </div>
            </div>
            <div className="card-actions">
              <button className="icon-button" onClick={() => openEdit(item)} title="Editar"><Icon name="edit" size={16} /></button>
              {user?.id !== item.id && (
                <button className="icon-button danger" onClick={() => remove(item)} title="Excluir"><Icon name="trash" size={16} /></button>
              )}
            </div>
          </article>
        ))}
        {items.length === 0 && !erro && <p style={{ textAlign: 'center', color: '#888', padding: 32 }}>Nenhum usuario encontrado</p>}
      </div>
    </>
  )
}
