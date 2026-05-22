import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import PrivateRoute from './components/PrivateRoute'
import Login from './pages/Login'
import Investimentos from './pages/Investimentos'
import Aportes from './pages/Aportes'
import SimplePage from './pages/SimplePage'
import Configuracoes from './pages/Configuracoes'
import Materiais from './pages/Materiais'
import CategoriasMateriais from './pages/CategoriasMateriais'
import Usuarios from './pages/Usuarios'
import Permissoes from './pages/Permissoes'
import CategoriasEventos from './pages/CategoriasEventos'
import CategoriasDespesa from './pages/CategoriasDespesa'
import Despesas from './pages/Despesas'
import Eventos from './pages/Eventos'
import LocaisEvento from './pages/LocaisEvento'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route index element={<Navigate to="/investimentos" replace />} />
        <Route path="investimentos" element={<Investimentos />} />
        <Route path="aportes" element={<Aportes />} />
        <Route path="doacoes" element={<SimplePage title="Doacoes" subtitle="Acompanhe doacoes financeiras e campanhas da igreja" />} />
        <Route path="configuracoes" element={<Configuracoes />} />
        <Route path="materiais" element={<Materiais />} />
        <Route path="eventos" element={<Eventos />} />
        <Route path="categorias-eventos" element={<CategoriasEventos />} />
        <Route path="locais-evento" element={<LocaisEvento />} />
        <Route path="categorias-materiais" element={<CategoriasMateriais />} />
        <Route path="despesas" element={<Despesas />} />
        <Route path="categorias-despesa" element={<CategoriasDespesa />} />
        <Route path="usuarios" element={<Usuarios />} />
        <Route path="permissoes" element={<Permissoes />} />
      </Route>
      <Route path="*" element={<Navigate to="/investimentos" replace />} />
    </Routes>
  )
}