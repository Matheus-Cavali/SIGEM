import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { post } from '../api/http'
import { useAuth } from '../state/AuthContext'
import Icon from '../components/Icon'
import { formatarCpf } from '../utils/format'

export default function Login() {
  const [form, setForm] = useState({ email: '', senha: '' })
  const [trocaSenha, setTrocaSenha] = useState({ ativo: false, cpf: '', senha: '', confirmar: '' })
  const [erro, setErro] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const enviar = async (event) => {
    event.preventDefault()
    setErro('')
    setLoading(true)

    try {
      const result = await login(form.email, form.senha)

      if (result.type === 'change-password') {
        setTrocaSenha({ ativo: true, cpf: formatarCpf(result.cpf), senha: '', confirmar: '' })
      } else {
        navigate('/investimentos')
      }
    } catch (error) {
      setErro(error.message)
    } finally {
      setLoading(false)
    }
  }

  const alterarSenha = async (event) => {
    event.preventDefault()
    setErro('')
    setLoading(true)

    try {
      if (trocaSenha.senha !== trocaSenha.confirmar) {
        throw new Error('As senhas nao conferem')
      }

      await post('/api/alterar-Primeira-Senha', { cpf: trocaSenha.cpf, senha: trocaSenha.senha })
      setTrocaSenha({ ativo: false, cpf: '', senha: '', confirmar: '' })
      setForm(prev => ({ ...prev, senha: '' }))
      setErro('Senha alterada. Entre novamente.')
    } catch (error) {
      setErro(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <section className="auth-panel">
        <div className="auth-brand">
          <span><Icon name="church" size={24} /></span>
          <div>
            <h1>Igreja</h1>
            <p>Sistema de Gestao Ministerial</p>
          </div>
        </div>

        {!trocaSenha.ativo && (
          <form className="auth-card" onSubmit={enviar}>
            <h2>Entrar</h2>
            <p>Acesse os investimentos, aportes e registros da igreja.</p>
            {erro && <div className="message">{erro}</div>}
            <label>
              <span>Email ou CPF</span>
              <input value={form.email} onChange={e => setForm(prev => ({ ...prev, email: e.target.value }))} placeholder="seu@email.com" />
            </label>
            <label>
              <span>Senha</span>
              <input type="password" value={form.senha} onChange={e => setForm(prev => ({ ...prev, senha: e.target.value }))} placeholder="Digite sua senha" />
            </label>
            <button className="danger-action" disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</button>
            <Link to="/cadastro">Criar cadastro</Link>
          </form>
        )}

        {trocaSenha.ativo && (
          <form className="auth-card" onSubmit={alterarSenha}>
            <h2>Primeiro acesso</h2>
            <p>Defina uma nova senha antes de continuar.</p>
            {erro && <div className="message">{erro}</div>}
            <label>
              <span>CPF</span>
              <input value={trocaSenha.cpf} disabled />
            </label>
            <label>
              <span>Nova senha</span>
              <input type="password" value={trocaSenha.senha} onChange={e => setTrocaSenha(prev => ({ ...prev, senha: e.target.value }))} />
            </label>
            <label>
              <span>Confirmar senha</span>
              <input type="password" value={trocaSenha.confirmar} onChange={e => setTrocaSenha(prev => ({ ...prev, confirmar: e.target.value }))} />
            </label>
            <button className="danger-action" disabled={loading}>{loading ? 'Salvando...' : 'Salvar senha'}</button>
          </form>
        )}
      </section>
    </div>
  )
}
