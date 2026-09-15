import { useEffect, useState } from 'react';
import { api } from '../api/http';

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

const STATUS_LABEL = {
  TRIAL: 'Período de teste',
  ATIVA: 'Ativa',
  INATIVA: 'Inativa',
  CANCELADA: 'Cancelada',
};

export default function Assinatura() {
  const [status, setStatus] = useState(null);
  const [planos, setPlanos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [escolhendo, setEscolhendo] = useState(null);
  const [mensagemPlano, setMensagemPlano] = useState('');

  function carregar() {
    setCarregando(true);
    Promise.all([api.get('/api/assinatura'), api.get('/api/assinatura/planos')])
      .then(([s, p]) => {
        setStatus(s);
        setPlanos(p);
      })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  async function escolherPlano(plano) {
    setEscolhendo(plano);
    setMensagemPlano('');
    try {
      const atualizado = await api.patch(`/api/assinatura/plano?plano=${plano}`);
      setStatus(atualizado);
      setMensagemPlano(
        'Plano registrado! A cobrança automática ainda não está disponível — fale com o suporte para ativar seu acesso enquanto isso.'
      );
    } catch (e) {
      setErro(e.message);
    } finally {
      setEscolhendo(null);
    }
  }

  if (carregando) {
    return (
      <div>
        <div className="page-header"><div><h2>Assinatura</h2></div></div>
        <p>Carregando...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Assinatura</h2>
          <p>Seu plano no BarberPro.</p>
        </div>
      </div>

      {erro && <div className="erro">{erro}</div>}

      {status && (
        <div className="panel" style={{ padding: 24, marginBottom: 28 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16, alignItems: 'center' }}>
            <div>
              <div className="rotulo" style={{ color: 'var(--texto-suave)', fontSize: '0.82rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
                Status atual
              </div>
              <div style={{ fontFamily: 'var(--fonte-titulo)', fontSize: '1.4rem', marginTop: 4 }}>
                {STATUS_LABEL[status.status] || status.status}
                {status.status === 'TRIAL' && status.diasRestantesTrial != null && (
                  <span style={{ fontFamily: 'var(--fonte-corpo)', fontSize: '0.95rem', color: 'var(--texto-suave)', marginLeft: 10 }}>
                    {!status.acessoLiberado
                      ? '· terminou'
                      : status.diasRestantesTrial > 0
                        ? `· termina em ${status.diasRestantesTrial} dia${status.diasRestantesTrial === 1 ? '' : 's'}`
                        : '· termina hoje'}
                  </span>
                )}
              </div>
            </div>
            <span className={`badge ${status.acessoLiberado ? 'badge-confirmado' : 'badge-cancelado'}`}>
              {status.acessoLiberado ? 'Acesso liberado' : 'Acesso bloqueado'}
            </span>
          </div>

          {!status.acessoLiberado && (
            <div className="erro" style={{ marginTop: 18, marginBottom: 0 }}>
              Seu período de teste terminou e o acesso ao painel está bloqueado. Escolha um plano abaixo para continuar.
            </div>
          )}
        </div>
      )}

      {mensagemPlano && (
        <div className="panel" style={{ padding: '14px 20px', marginBottom: 20, borderColor: 'var(--latao)', background: 'var(--latao-suave)' }}>
          {mensagemPlano}
        </div>
      )}

      <div className="form-grid">
        {planos.map((p) => (
          <div key={p.plano} className="metric-card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <div className="rotulo">{p.plano}</div>
              <div className="valor" style={{ fontSize: '1.6rem' }}>
                {formatarMoeda(p.precoMensal)}<span style={{ fontFamily: 'var(--fonte-corpo)', fontSize: '0.85rem', color: 'var(--texto-suave)' }}>/mês</span>
              </div>
            </div>
            <p style={{ color: 'var(--texto-suave)', fontSize: '0.9rem', margin: 0, flex: 1 }}>{p.descricao}</p>
            <button
              className={status?.plano === p.plano ? 'btn btn-secundario' : 'btn btn-latao'}
              disabled={status?.plano === p.plano || escolhendo === p.plano}
              onClick={() => escolherPlano(p.plano)}
            >
              {status?.plano === p.plano ? 'Plano atual' : escolhendo === p.plano ? 'Escolhendo...' : 'Escolher este plano'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
