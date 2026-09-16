import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Sidebar from './components/Sidebar';
import { api } from './api/http';

import Login from './pages/auth/Login';
import Registro from './pages/auth/Registro';
import Dashboard from './pages/Dashboard';
import Clientes from './pages/Clientes';
import Servicos from './pages/Servicos';
import Profissionais from './pages/Profissionais';
import Agenda from './pages/Agenda';
import Financeiro from './pages/Financeiro';
import Assinatura from './pages/Assinatura';
import AgendamentoPublico from './pages/public/AgendamentoPublico';
import Institucional from './pages/Institucional';

function TrialBanner({ status }) {
  if (!status || status.status !== 'TRIAL' || status.diasRestantesTrial == null) return null;
  if (!status.acessoLiberado) return null;
  if (status.diasRestantesTrial > 3) return null;

  return (
    <div
      style={{
        background: 'var(--latao-suave)',
        border: '1px solid var(--latao)',
        borderRadius: 'var(--radius-sm)',
        padding: '10px 16px',
        marginBottom: 24,
        fontSize: '0.9rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 12,
        flexWrap: 'wrap',
      }}
    >
      <span>
        {status.diasRestantesTrial > 0
          ? `Seu período de teste termina em ${status.diasRestantesTrial} dia${status.diasRestantesTrial === 1 ? '' : 's'}.`
          : 'Seu período de teste termina hoje.'}
      </span>
      <a href="/painel/assinatura" className="btn btn-latao" style={{ padding: '6px 14px' }}>Escolher plano</a>
    </div>
  );
}

function PainelLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { usuario } = useAuth();
  const [status, setStatus] = useState(null);

  useEffect(() => {
    api.get('/api/assinatura')
      .then((s) => {
        setStatus(s);
        if (!s.acessoLiberado && location.pathname !== '/painel/assinatura') {
          navigate('/painel/assinatura', { replace: true });
        }
      })
      .catch(() => {});
  }, [location.pathname]);

  // Profissional não tem acesso ao painel geral (dashboard) da barbearia — só à própria agenda.
  useEffect(() => {
    if (usuario?.papel === 'PROFISSIONAL' && location.pathname === '/painel') {
      navigate('/painel/agenda', { replace: true });
    }
  }, [usuario, location.pathname]);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">
        <TrialBanner status={status} />
        <Outlet />
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Institucional />} />
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />
          <Route path="/b/:slug" element={<AgendamentoPublico />} />

          <Route
            path="/painel"
            element={
              <ProtectedRoute>
                <PainelLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="agenda" element={<Agenda />} />
            <Route path="clientes" element={<Clientes />} />
            <Route path="servicos" element={<Servicos />} />
            <Route path="profissionais" element={<Profissionais />} />
            <Route path="financeiro" element={<Financeiro />} />
            <Route path="assinatura" element={<Assinatura />} />
          </Route>

          <Route path="*" element={<Navigate to="/painel" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
