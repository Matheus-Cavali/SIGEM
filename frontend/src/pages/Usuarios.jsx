import { useEffect, useState } from 'react'
import { del, get, post, put, request } from '../api/http'
import { toast } from 'react-toastify'
import { useAuth } from '../state/AuthContext'
import { formatarCpf } from '../utils/format'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import '../components/Modal/Modal.scss'
import { CpfField, PhoneField, CepField } from '../components/form'

const initialForm = {
  nome: '', email: '', senha: '', cpf: '', rg: '',
  celular: '', rua: '', bairro: '', cep: '', cidade: '', estado: '',
  tipoUsuario: 'colaborador', nivelAcesso: '2',
}

export default function Usuarios() {
  const { user, can } = useAuth()
  const podeGerenciar = can('GESTAO_USUARIOS')

  const [items, setItems] = useState([])
  const [form, setForm] = useState(initialForm)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [confirmToggle, setConfirmToggle] = useState(null)

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
      if (!error.fieldErrors) toast.error(error.message)
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
    setFieldErrors({})
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
    setFieldErrors({})
    setFormOpen(true)
  }

  const validarCampos = () => {
    const erros = {}
    if (!form.nome.trim()) erros.nome = 'Nome é obrigatório'
    if (!form.email.trim()) erros.email = 'Email é obrigatório'
    if (!editing && !form.senha) erros.senha = 'Senha é obrigatória'
    if (!editing && form.senha && form.senha.length < 4) erros.senha = 'Senha deve ter no mínimo 4 caracteres'
    if (!form.cpf.trim()) erros.cpf = 'CPF é obrigatório'
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
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          toast.error(error.message)
        }
      }
    }
  }

  const toggleStatus = (item) => {
    setConfirmToggle(item)
  }

  const handleConfirmToggle = async () => {
    if (!confirmToggle) return
    const novoStatus = !confirmToggle.statusAtivo
    try {
      await request('/api/usuarios/' + confirmToggle.id + '/status', {
        method: 'PATCH',
        body: JSON.stringify({ status_ativo: novoStatus }),
      })
      setItems(prev => prev.map(u => u.id === confirmToggle.id ? { ...u, statusAtivo: novoStatus } : u))
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmToggle(null)
    }
  }

  const remove = (item) => {
    setConfirmDelete(item)
  }

  const handleConfirmDelete = async () => {
    if (!confirmDelete) return
    try {
      await del('/api/usuarios/' + confirmDelete.id)
      setItems(prev => prev.filter(u => u.id !== confirmDelete.id))
    } catch (error) {
      toast.error(error.message)
    } finally {
      setConfirmDelete(null)
    }
  }

  const handleCpfChange = (value) => {
    setForm(prev => ({ ...prev, cpf: formatarCpf(value) }))
    setFieldErrors(prev => ({ ...prev, cpf: '' }))
  }

  const handleFieldChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setFieldErrors(prev => ({ ...prev, [field]: '' }))
  }

  const nivelLabel = (nivel) => {
    if (nivel === 1) return 'Total'
    if (nivel === 2) return 'Restrito'
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
          <form className="inline-form" onSubmit={save}>
            <div className="field-container">
              <label className="field-label">Nome <span className="required-star">*</span></label>
              <input className={'field-control' + (fieldErrors.nome ? ' is-invalid' : '')}
                     value={form.nome} onChange={e => handleFieldChange('nome', e.target.value)} placeholder="Nome completo" />
              {fieldErrors.nome && <span className="field-error">{fieldErrors.nome}</span>}
            </div>
            <div className="field-container">
              <label className="field-label">Email <span className="required-star">*</span></label>
              <input className={'field-control' + (fieldErrors.email ? ' is-invalid' : '')}
                     type="email" value={form.email} onChange={e => handleFieldChange('email', e.target.value)} placeholder="email@exemplo.com" />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </div>
            {!editing && (
              <div className="field-container">
                <label className="field-label">Senha <span className="required-star">*</span></label>
                <input className={'field-control' + (fieldErrors.senha ? ' is-invalid' : '')}
                       type="password" value={form.senha} onChange={e => handleFieldChange('senha', e.target.value)} placeholder="Minimo 4 caracteres" />
                {fieldErrors.senha && <span className="field-error">{fieldErrors.senha}</span>}
              </div>
            )}
            <CpfField label="CPF" value={form.cpf} setValue={handleCpfChange} error={fieldErrors.cpf} required placeholder="000.000.000-00" />
            <div className="field-container">
              <label className="field-label">RG</label>
              <input className="field-control" value={form.rg} onChange={e => handleFieldChange('rg', e.target.value)} placeholder="RG" />
            </div>
            <PhoneField label="Celular" value={form.celular} setValue={(v) => handleFieldChange('celular', v)} />
            <CepField label="CEP" value={form.cep} setValue={(v) => handleFieldChange('cep', v)} />
            <div className="field-container">
              <label className="field-label">Rua</label>
              <input className="field-control" value={form.rua} onChange={e => handleFieldChange('rua', e.target.value)} placeholder="Rua, numero" />
            </div>
            <div className="field-container">
              <label className="field-label">Bairro</label>
              <input className="field-control" value={form.bairro} onChange={e => handleFieldChange('bairro', e.target.value)} placeholder="Bairro" />
            </div>
            <div className="field-container">
              <label className="field-label">Cidade</label>
              <input className="field-control" value={form.cidade} onChange={e => handleFieldChange('cidade', e.target.value)} placeholder="Cidade" />
            </div>
            <div className="field-container">
              <label className="field-label">Estado</label>
              <input className="field-control" value={form.estado} onChange={e => handleFieldChange('estado', e.target.value)} placeholder="Estado (UF)" maxLength={2} />
            </div>
            <div className="field-container">
              <label className="field-label">Tipo de Usuario</label>
              <select className="field-control" value={form.tipoUsuario} onChange={e => handleFieldChange('tipoUsuario', e.target.value)}>
                <option value="colaborador">Colaborador</option>
                <option value="voluntario">Voluntario</option>
              </select>
            </div>
            <div className="field-container">
              <label className="field-label">Nivel de Acesso</label>
              <select className="field-control" value={form.nivelAcesso} onChange={e => handleFieldChange('nivelAcesso', e.target.value)}>
                <option value="2">Restrito</option>
                <option value="1">Total</option>
              </select>
            </div>
            <div className="form-submit">
              <button className="primary-action">{editing ? 'Salvar Alteracoes' : 'Salvar Usuario'}</button>
            </div>
          </form>
        </section>
      )}

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
        <p>Excluir permanentemente o usuário "{confirmDelete?.nome}"?</p>
      </Modal>

      <Modal
        open={!!confirmToggle}
        title="Confirmar alteração de status"
        variant="info"
        hideCloseButton
        onClose={() => setConfirmToggle(null)}
        footer={
          <>
            <button className="ghost-action" onClick={() => setConfirmToggle(null)}>Cancelar</button>
            <button className="danger-action" onClick={handleConfirmToggle}>
              {confirmToggle?.statusAtivo ? 'Desativar' : 'Reativar'}
            </button>
          </>
        }
      >
        <p>
          {confirmToggle?.statusAtivo
            ? 'Desativar usuário "' + confirmToggle?.nome + '"? Ele não poderá acessar o sistema.'
            : 'Reativar usuário "' + confirmToggle?.nome + '"?'
          }
        </p>
      </Modal>

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
