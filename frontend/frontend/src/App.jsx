import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import PrivateRoute from './components/PrivateRoute'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import Investimentos from './pages/Investimentos'
import Aportes from './pages/Aportes'
import SimplePage from './pages/SimplePage'
import Materiais from './pages/Materiais'
import CategoriasEventos from './pages/CategoriasEventos'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
      <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route index element={<Navigate to="/investimentos" replace />} />
        <Route path="investimentos" element={<Investimentos />} />
        <Route path="aportes" element={<Aportes />} />
        <Route path="doacoes" element={<SimplePage title="Doacoes" subtitle="Acompanhe doacoes financeiras e campanhas da igreja" />} />
        <Route path="materiais" element={<Materiais />} />
        <Route path="categorias-eventos" element={<CategoriasEventos />} />
        <Route path="configuracoes" element={<SimplePage title="Configuracoes" subtitle="Ajustes gerais e permissoes do sistema" />} />
      </Route>
      <Route path="*" element={<Navigate to="/investimentos" replace />} />
    </Routes>
  )
}