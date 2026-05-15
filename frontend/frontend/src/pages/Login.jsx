import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { post } from '../api/http'
import { useAuth } from '../state/AuthContext'
import Icon from '../components/Icon'
import { formatarCpf } from '../utils/format'

export default function Login() {
  const [form, setForm] = useState({ email: '', senha: '' })
  const [trocaSenha, setTrocaSenha] = useState({ ativo: false, cpf: '', senha: '', confirmar: '' })
  const [esqueciSenha, setEsqueciSenha] = useState(false)
  const [redefinir, setRedefinir] = useState({ cpf: '', senha: '', confirmar: '' })
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

      await post('/api/alterar-Primeira-Senha', { cpf: trocaSenha.cpf.replace(/\D/g, ''), senha: trocaSenha.senha })
      setTrocaSenha({ ativo: false, cpf: '', senha: '', confirmar: '' })
      setForm(prev => ({ ...prev, senha: '' }))
      setErro('Senha alterada. Entre novamente.')
    } catch (error) {
      setErro(error.message)
    } finally {
      setLoading(false)
    }
  }

  const redefinirSenha = async (event) => {
    event.preventDefault()
    setErro('')
    if (redefinir.senha !== redefinir.confirmar) { setErro('Senhas nao conferem'); return }
    if (redefinir.senha.length < 4) { setErro('Minimo 4 caracteres'); return }
    if (!redefinir.cpf) { setErro('Digite seu CPF'); return }
    setLoading(true)
    try {
      await post('/api/alterar-Primeira-Senha', { cpf: redefinir.cpf.replace(/\D/g, ''), senha: redefinir.senha })
      setRedefinir({ cpf: '', senha: '', confirmar: '' })
      setEsqueciSenha(false)
      setErro('Senha redefinida! Faca login.')
    } catch (error) { setErro(error.message) }
    finally { setLoading(false) }
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

        {!trocaSenha.ativo && !esqueciSenha && (
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
            <div style={{ textAlign: 'center', marginTop: 12, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <button type="button" onClick={() => setEsqueciSenha(true)} style={{ background: 'none', border: 'none', color: '#23598d', cursor: 'pointer', fontSize: 13 }}>Esqueceu a senha?</button>
              <Link to="/cadastro" style={{ fontSize: 13 }}>Criar cadastro</Link>
            </div>
          </form>
        )}

        {esqueciSenha && (
          <form className="auth-card" onSubmit={redefinirSenha}>
            <h2>Redefinir Senha</h2>
            <p>Digite seu CPF e a nova senha.</p>
            {erro && <div className="message">{erro}</div>}
            <label>
              <span>CPF</span>
              <input value={redefinir.cpf} onChange={e => setRedefinir(prev => ({ ...prev, cpf: e.target.value }))} placeholder="000.000.000-00" />
            </label>
            <label>
              <span>Nova senha</span>
              <input type="password" value={redefinir.senha} onChange={e => setRedefinir(prev => ({ ...prev, senha: e.target.value }))} />
            </label>
            <label>
              <span>Confirmar senha</span>
              <input type="password" value={redefinir.confirmar} onChange={e => setRedefinir(prev => ({ ...prev, confirmar: e.target.value }))} />
            </label>
            <button className="danger-action" disabled={loading}>{loading ? 'Redefinindo...' : 'Redefinir Senha'}</button>
            <div style={{ textAlign: 'center', marginTop: 12 }}>
              <button type="button" onClick={() => { setEsqueciSenha(false); setRedefinir({ cpf: '', senha: '', confirmar: '' }); setErro('') }} style={{ background: 'none', border: 'none', color: '#23598d', cursor: 'pointer', fontSize: 13 }}>Voltar para o login</button>
            </div>
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
