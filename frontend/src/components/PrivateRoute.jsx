import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../state/AuthContext'
import { useParameters } from '../state/ParametersContext'

export default function PrivateRoute({ children }) {
  const { user, loading } = useAuth()
  const { configured, loading: paramsLoading } = useParameters()
  const location = useLocation()
  let content = children

  if (loading || paramsLoading) {
    content = <div className="loading-page">Carregando...</div>
  } else if (!user) {
    content = <Navigate to="/login" replace />
  } else if (!configured && location.pathname !== '/configuracoes') {
    content = <Navigate to="/configuracoes" replace />
  }

  return content
}
