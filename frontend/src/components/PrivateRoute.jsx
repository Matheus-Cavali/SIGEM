import { Navigate } from 'react-router-dom'
import { useAuth } from '../state/AuthContext'

export default function PrivateRoute({ children }) {
  const { user, loading } = useAuth()
  let content = children

  if (loading) {
    content = <div className="loading-page">Carregando...</div>
  } else if (!user) {
    content = <Navigate to="/login" replace />
  }

  return content
}
