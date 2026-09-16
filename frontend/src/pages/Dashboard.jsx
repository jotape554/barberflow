import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/http';
import RecursoBloqueado from '../components/RecursoBloqueado';
import EstadoVazio from '../components/EstadoVazio';

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function Variacao({ percentual }) {
  if (percentual === null || percentual === undefined) {
    return <span className="variacao variacao-neutra">sem base de comparação</span>;
  }
  const positiva = percentual >= 0;
  return (
    <span className={`variacao ${positiva ? 'variacao-positiva' : 'variacao-negativa'}`}>
      {positiva ? '▲' : '▼'} {Math.abs(percentual).toLocaleString('pt-BR')}%
    </span>
  );
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

  if (bloqueio) {
    return (
      <div>
        <div className="page-header">
          <div>
            <h2>Painel</h2>
            <p>Um retrato de agora — sem números inventados.</p>
          </div>
        </div>
        <RecursoBloqueado planoNecessario={bloqueio.planoNecessario} mensagem={bloqueio.mensagem} />
      </div>
    );
  }

  const maiorValorDoDia = dados
    ? Math.max(1, ...dados.financeiro.faturamentoPorDia.map((p) => Number(p.valor)))
    : 1;

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

      {dados && (
        <>
          <div className="acoes-rapidas">
            <Link className="btn btn-latao" to="/painel/agenda?novo=1">Novo agendamento</Link>
            <Link className="btn btn-secundario" to="/painel/clientes?novo=1">Novo cliente</Link>
            <Link className="btn btn-secundario" to="/painel/servicos?novo=1">Novo serviço</Link>
            <Link className="btn btn-secundario" to="/painel/financeiro?aba=despesas&novo=1">Lançar despesa</Link>
            <Link className="btn btn-secundario" to="/painel/financeiro">Ver financeiro</Link>
            <Link className="btn btn-secundario" to="/painel/assistente">Abrir Assistente IA</Link>
          </div>

          <div className="cards-grid">
            <div className="metric-card">
              <div className="rotulo">Faturamento hoje</div>
              <div className="valor">{formatarMoeda(dados.visaoGeral.faturamentoHoje)}</div>
              <Variacao percentual={dados.comparacoes.variacaoHojeOntemPercentual} />
            </div>
            <div className="metric-card">
              <div className="rotulo">Faturamento na semana</div>
              <div className="valor">{formatarMoeda(dados.visaoGeral.faturamentoSemana)}</div>
              <Variacao percentual={dados.comparacoes.variacaoSemanaPercentual} />
            </div>
            <div className="metric-card">
              <div className="rotulo">Faturamento no mês</div>
              <div className="valor">{formatarMoeda(dados.visaoGeral.faturamentoMes)}</div>
              <Variacao percentual={dados.comparacoes.variacaoMesPercentual} />
            </div>
            <div className="metric-card">
              <div className="rotulo">Atendimentos hoje</div>
              <div className="valor">{dados.visaoGeral.atendimentosHoje}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Ticket médio do mês</div>
              <div className="valor">{formatarMoeda(dados.visaoGeral.ticketMedioMes)}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Clientes cadastrados</div>
              <div className="valor">{dados.visaoGeral.clientesCadastrados}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Novos clientes no mês</div>
              <div className="valor">{dados.visaoGeral.novosClientesMes}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Clientes recorrentes no mês</div>
              <div className="valor">{dados.visaoGeral.clientesRecorrentesMes}</div>
            </div>
          </div>

          <h3 className="secao-titulo">Faturamento nos últimos 14 dias</h3>
          <div className="panel">
            {dados.financeiro.faturamentoPorDia.every((p) => Number(p.valor) === 0) ? (
              <EstadoVazio mensagem="Ainda não há faturamento registrado nesse período." />
            ) : (
              <>
                <div className="mini-grafico">
                  {dados.financeiro.faturamentoPorDia.map((p) => (
                    <div
                      key={p.data}
                      className="mini-grafico-barra"
                      style={{ height: `${Math.max(2, (Number(p.valor) / maiorValorDoDia) * 100)}%` }}
                      title={`${p.data}: ${formatarMoeda(p.valor)}`}
                    />
                  ))}
                </div>
                <div className="mini-grafico-legenda">
                  <span>{dados.financeiro.faturamentoPorDia[0].data}</span>
                  <span>{dados.financeiro.faturamentoPorDia[dados.financeiro.faturamentoPorDia.length - 1].data}</span>
                </div>
              </>
            )}
          </div>

          <div className="cards-grid" style={{ marginTop: 20 }}>
            <div className="metric-card">
              <div className="rotulo">Despesas do mês</div>
              <div className="valor">{formatarMoeda(dados.financeiro.despesasMes)}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Comissões do mês</div>
              <div className="valor">{formatarMoeda(dados.financeiro.comissoesMes)}</div>
            </div>
            <div className="metric-card">
              <div className="rotulo">Lucro líquido estimado</div>
              <div className="valor">{formatarMoeda(dados.financeiro.lucroLiquidoMes)}</div>
            </div>
          </div>

          <h3 className="secao-titulo">Profissionais no mês</h3>
          <div className="panel">
            {dados.profissionais.length === 0 ? (
              <EstadoVazio mensagem="Nenhum atendimento concluído neste mês ainda." />
            ) : (
              dados.profissionais.map((p) => (
                <div key={p.nome} className="ranking-linha">
                  <div>
                    <div className="nome">{p.nome}</div>
                    <div className="detalhe">{p.atendimentos} atendimento(s) · {p.clientesAtendidos} cliente(s) · ticket médio {formatarMoeda(p.ticketMedio)}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div className="nome">{formatarMoeda(p.faturamento)}</div>
                    <div className="detalhe">comissão {formatarMoeda(p.comissao)}</div>
                  </div>
                </div>
              ))
            )}
          </div>

          <h3 className="secao-titulo">Serviços mais vendidos no mês</h3>
          <div className="panel">
            {dados.servicos.length === 0 ? (
              <EstadoVazio mensagem="Nenhum serviço concluído neste mês ainda." />
            ) : (
              dados.servicos.map((s) => (
                <div key={s.nome} className="ranking-linha">
                  <div>
                    <div className="nome">{s.nome}</div>
                    <div className="detalhe">{s.quantidade} venda(s) · ticket médio {formatarMoeda(s.ticketMedio)}</div>
                  </div>
                  <div className="nome">{formatarMoeda(s.faturamento)}</div>
                </div>
              ))
            )}
          </div>

          <h3 className="secao-titulo">Clientes a reativar (30+ dias sem agendar)</h3>
          <div className="panel">
            {dados.clientes.listaInativos.length === 0 ? (
              <EstadoVazio mensagem="Nenhum cliente parado há mais de 30 dias — parabéns!" />
            ) : (
              dados.clientes.listaInativos.map((c) => (
                <div key={c.nome + c.telefone} className="ranking-linha">
                  <div>
                    <div className="nome">{c.nome}</div>
                    <div className="detalhe">{c.telefone || 'sem telefone'}</div>
                  </div>
                  <div className="detalhe">
                    {c.ultimoAgendamento ? `Último atendimento: ${c.ultimoAgendamento}` : 'Nunca agendou'}
                  </div>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
