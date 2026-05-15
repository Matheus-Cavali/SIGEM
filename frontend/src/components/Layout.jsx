import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../state/AuthContext'
import Icon from './Icon'

const mainItems = [
  { label: 'Investimentos', path: '/investimentos', icon: 'trend' },
  { label: 'Aportes', path: '/aportes', icon: 'dollar' },
  { label: 'Usuarios', path: '/usuarios', icon: 'settings' },
  { label: 'Permissoes', path: '/permissoes', icon: 'target', adminOnly: true },
  { label: 'Doacoes', path: '/doacoes', icon: 'heart' },
  { label: 'Materiais', path: '/materiais', icon: 'box' },
]

const geralItems = [
  { label: 'Configuracoes', path: '/configuracoes', icon: 'settings' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const sair = () => { logout(); navigate('/login') }

  const visibleMainItems = mainItems.filter(item => !item.adminOnly || user?.nivelAcesso === 1)

  return (
    <div className="shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => navigate('/investimentos')}>
          <span className="brand-mark"><Icon name="church" size={18} /></span>
          <span>SIGEM</span>
        </button>

        <nav className="nav">
          <span className="nav-label">PRINCIPAL</span>
          {visibleMainItems.map(item => (
            <NavLink key={item.path} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')} to={item.path}>
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}

          <span className="nav-label nav-label-spaced">OUTROS</span>
          {geralItems.map(item => (
            <NavLink key={item.path} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')} to={item.path}>
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}

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
