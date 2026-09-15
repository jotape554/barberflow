import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ITENS = [
  { to: '/painel', label: 'Painel' },
  { to: '/painel/agenda', label: 'Agenda' },
  { to: '/painel/clientes', label: 'Clientes' },
  { to: '/painel/servicos', label: 'Serviços' },
  { to: '/painel/profissionais', label: 'Profissionais' },
  { to: '/painel/financeiro', label: 'Financeiro' },
];

export default function Sidebar() {
  const { usuario, sair } = useAuth();
  const navigate = useNavigate();

  function handleSair() {
    sair();
    navigate('/login');
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">Barber<span>Pro</span></div>
      <nav>
        {ITENS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/painel'}
            className={({ isActive }) => (isActive ? 'active' : '')}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div style={{ color: 'rgba(239,235,226,0.6)', fontSize: '0.82rem', marginBottom: 10 }}>
          {usuario?.nome} · {usuario?.papel?.toLowerCase()}
        </div>
        <button className="sidebar-link" onClick={handleSair}>Sair</button>
      </div>
    </aside>
  );
}
