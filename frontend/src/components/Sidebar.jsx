import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ITENS = [
  { to: '/painel', label: 'Painel', icone: '◱' },
  { to: '/painel/agenda', label: 'Agenda', icone: '◷' },
  { to: '/painel/clientes', label: 'Clientes', icone: '◍' },
  { to: '/painel/servicos', label: 'Serviços', icone: '✂' },
  { to: '/painel/profissionais', label: 'Profissionais', icone: '◔' },
  { to: '/painel/financeiro', label: 'Financeiro', icone: '◈' },
];

function iniciais(nome) {
  if (!nome) return '?';
  const partes = nome.trim().split(/\s+/);
  return (partes[0][0] + (partes[1]?.[0] || '')).toUpperCase();
}

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
            <span aria-hidden="true" style={{ marginRight: 10, opacity: 0.85 }}>{item.icone}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div className="avatar">{iniciais(usuario?.nome)}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: 'var(--papel)', fontSize: '0.86rem', fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {usuario?.nome}
          </div>
          <button
            className="sidebar-link"
            onClick={handleSair}
            style={{ padding: 0, color: 'rgba(239,235,226,0.55)', fontSize: '0.78rem', textTransform: 'capitalize' }}
          >
            {usuario?.papel?.toLowerCase()} · Sair
          </button>
        </div>
      </div>
    </aside>
  );
}
