import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { post } from '../api/http'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem('sigem_session')

    if (stored) {
      try {
        setUser(JSON.parse(stored))
      } catch (error) {
        localStorage.removeItem('sigem_session')
      }
    }

    setLoading(false)
  }, [])

  const login = async (email, senha) => {
    const data = await post('/api/login', { email, senha })
    let result = { type: 'ok' }

    if (data.status === 'TROCA_OBRIGATORIA') {
      result = { type: 'change-password', cpf: data.cpf }
    } else {
      const session = {
        token: data.token,
        id: data.id,
        nome: data.nome,
        email: data.email,
        nivelAcesso: data.nivelAcesso,
        tipoUsuario: data.tipoUsuario,
        permissoes: data.permissoes || [],
      }
      localStorage.setItem('sigem_session', JSON.stringify(session))
      setUser(session)
    }

    return result
  }

  const logout = () => {
    localStorage.removeItem('sigem_session')
    setUser(null)
  }

  const can = (permission) => {
    let allowed = false

    if (user?.nivelAcesso === 1) {
      allowed = true
    } else if (Array.isArray(permission)) {
      allowed = permission.some(item => user?.permissoes?.includes(item))
    } else {
      allowed = user?.permissoes?.includes(permission)
    }

    return allowed
  }

  const value = useMemo(() => ({ user, loading, login, logout, can }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
