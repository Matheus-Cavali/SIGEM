import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { post } from '../api/http'
import Icon from '../components/Icon'
import { formatarCpf, formatarData } from '../utils/format'

const initialForm = {
  nome: '',
  email: '',
  senha: '',
  confirmar: '',
  cpf: '',
  tipoUsuario: 'colaborador',
  data: '',
}

export default function Cadastro() {
  const [form, setForm] = useState(initialForm)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const update = (field, value) => {
    let nextValue = value

    if (field === 'cpf') {
      nextValue = formatarCpf(value)
    }

    if (field === 'data') {
      nextValue = formatarData(value)
    }

    setForm(prev => ({ ...prev, [field]: nextValue }))
  }

  const enviar = async (event) => {
    event.preventDefault()
    setErro('')
    setSucesso('')
    setLoading(true)

    try {
      if (form.senha !== form.confirmar) {
        throw new Error('As senhas nao conferem')
      }

      await post('/api/cadastrar', {
        nome: form.nome,
        email: form.email,
        senha: form.senha,
        cpf: form.cpf,
        nivelAcesso: 2,
        tipoUsuario: form.tipoUsuario,
        data: form.data.replace(/\D/g, ''),
      })
      setForm(initialForm)
      setSucesso('Cadastro realizado. Voce ja pode entrar.')
      setTimeout(() => navigate('/login'), 900)
    } catch (error) {
      setErro(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <section className="auth-panel auth-panel-wide">
        <div className="auth-brand">
          <span><Icon name="church" size={24} /></span>
          <div>
            <h1>Igreja</h1>
            <p>Cadastro de colaboradores e voluntarios</p>
          </div>
        </div>

        <form className="auth-card" onSubmit={enviar}>
          <h2>Criar cadastro</h2>
          <p>Informe os dados principais conforme as tabelas usuario, colaborador e voluntario.</p>
          {erro && <div className="message">{erro}</div>}
          {sucesso && <div className="message success">{sucesso}</div>}

          <div className="form-grid">
            <label>
              <span>Nome</span>
              <input value={form.nome} onChange={e => update('nome', e.target.value)} placeholder="Nome completo" />
            </label>
            <label>
              <span>Email</span>
              <input type="email" value={form.email} onChange={e => update('email', e.target.value)} placeholder="seu@email.com" />
            </label>
            <label>
              <span>CPF</span>
              <input value={form.cpf} onChange={e => update('cpf', e.target.value)} placeholder="000.000.000-00" />
            </label>
            <label>
              <span>Data</span>
              <input value={form.data} onChange={e => update('data', e.target.value)} placeholder="dd/mm/aaaa" />
            </label>
            <label>
              <span>Tipo</span>
              <select value={form.tipoUsuario} onChange={e => update('tipoUsuario', e.target.value)}>
                <option value="colaborador">Colaborador</option>
                <option value="voluntario">Voluntario</option>
              </select>
            </label>
            <label>
              <span>Senha</span>
              <input type="password" value={form.senha} onChange={e => update('senha', e.target.value)} placeholder="Minimo 4 caracteres" />
            </label>
            <label>
              <span>Confirmar senha</span>
              <input type="password" value={form.confirmar} onChange={e => update('confirmar', e.target.value)} placeholder="Repita a senha" />
            </label>
          </div>

          <button className="danger-action" disabled={loading}>{loading ? 'Cadastrando...' : 'Cadastrar'}</button>
          <Link to="/login">Ja tenho acesso</Link>
        </form>
      </section>
    </div>
  )
}
