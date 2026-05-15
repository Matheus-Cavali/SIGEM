import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../state/AuthContext'
import Icon from './Icon'

const mainItems = [
  { label: 'Investimentos', path: '/investimentos', icon: 'trend' },
  { label: 'Aportes', path: '/aportes', icon: 'dollar' },
  { label: 'Doacoes', path: '/doacoes', icon: 'heart' },
  { label: 'Materiais', path: '/materiais', icon: 'box' },
  { label: 'Cat Materiais', path: '/categorias-materiais', icon: 'box' },
  { label: 'Cat Eventos', path: '/categorias-eventos', icon: 'box' },
  { label: 'Despesas', path: '/despesas', icon: 'dollar' },
  { label: 'Cat Despesa', path: '/categorias-despesa', icon: 'dollar' },
  { label: 'Usuarios', path: '/usuarios', icon: 'users' }
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const sair = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => navigate('/investimentos')}>
          <span className="brand-mark"><Icon name="church" size={18} /></span>
          <span>Igreja</span>
        </button>

        <nav className="nav">
          <span className="nav-label">MENU</span>
          {mainItems.map(item => (
            <NavLink key={item.path} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')} to={item.path}>
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}

          {user?.nivelAcesso === 1 && (
            <NavLink className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')} to="/permissoes">
              <Icon name="settings" size={18} />
              <span>Permissoes</span>
            </NavLink>
          )}

          <span className="nav-label nav-label-spaced">GERAL</span>
          <NavLink className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')} to="/configuracoes">
            <Icon name="settings" size={18} />
            <span>Configuracoes</span>
          </NavLink>
          <button className="nav-item nav-button" onClick={sair}>
            <Icon name="logout" size={18} />
            <span>Sair</span>
          </button>
        </nav>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
