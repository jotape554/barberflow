import { useEffect, useState } from 'react';
import { api } from '../api/http';
import RecursoBloqueado from '../components/RecursoBloqueado';

function formatarMoeda(valor) {
  return (valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export default function Dashboard() {
  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [bloqueio, setBloqueio] = useState(null);

  useEffect(() => {
    api.get('/api/dashboard')
      .then(setDados)
      .catch((e) => {
        if (e.dados?.upgradeNecessario) setBloqueio(e.dados);
        else setErro(e.message);
      })
      .finally(() => setCarregando(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Painel</h2>
          <p>Um retrato de agora — sem números inventados.</p>
        </div>
      </div>

      {erro && <div className="erro">{erro}</div>}
      {carregando && <p>Carregando...</p>}

      {bloqueio && <RecursoBloqueado planoNecessario={bloqueio.planoNecessario} mensagem={bloqueio.mensagem} />}

      {dados && (
        <div className="cards-grid">
          <div className="metric-card">
            <div className="rotulo">Faturamento hoje</div>
            <div className="valor">{formatarMoeda(dados.faturamentoHoje)}</div>
          </div>
          <div className="metric-card">
            <div className="rotulo">Faturamento no mês</div>
            <div className="valor">{formatarMoeda(dados.faturamentoMes)}</div>
          </div>
          <div className="metric-card">
            <div className="rotulo">Agendamentos hoje</div>
            <div className="valor">{dados.agendamentosHoje}</div>
          </div>
          <div className="metric-card">
            <div className="rotulo">Concluídos hoje</div>
            <div className="valor">{dados.agendamentosConcluidosHoje}</div>
          </div>
          <div className="metric-card">
            <div className="rotulo">Clientes cadastrados</div>
            <div className="valor">{dados.clientesCadastrados}</div>
          </div>
          <div className="metric-card">
            <div className="rotulo">Ticket médio do mês</div>
            <div className="valor">{formatarMoeda(dados.ticketMedioMes)}</div>
          </div>
        </div>
      )}
    </div>
  );
}
