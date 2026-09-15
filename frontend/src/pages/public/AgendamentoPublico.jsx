import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../../api/http';
import '../../styles/public.css';

function hoje() {
  return new Date().toISOString().slice(0, 10);
}

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarHora(horario) {
  return horario.slice(0, 5);
}

export default function AgendamentoPublico() {
  const { slug } = useParams();

  const [barbearia, setBarbearia] = useState(null);
  const [servicos, setServicos] = useState([]);
  const [profissionais, setProfissionais] = useState([]);
  const [carregandoBarbearia, setCarregandoBarbearia] = useState(true);
  const [erroBarbearia, setErroBarbearia] = useState('');

  const [passo, setPasso] = useState(1);
  const [servico, setServico] = useState(null);
  const [profissional, setProfissional] = useState(null);
  const [data, setData] = useState(hoje());
  const [horario, setHorario] = useState(null);
  const [horarios, setHorarios] = useState([]);
  const [carregandoHorarios, setCarregandoHorarios] = useState(false);

  const [nomeCliente, setNomeCliente] = useState('');
  const [telefoneCliente, setTelefoneCliente] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [concluido, setConcluido] = useState(false);

  useEffect(() => {
    Promise.all([
      api.publica.get(`/public/barbearias/${slug}`),
      api.publica.get(`/public/barbearias/${slug}/servicos`),
      api.publica.get(`/public/barbearias/${slug}/profissionais`),
    ])
      .then(([b, s, p]) => {
        setBarbearia(b);
        setServicos(s);
        setProfissionais(p);
      })
      .catch((e) => setErroBarbearia(e.message || 'Barbearia não encontrada.'))
      .finally(() => setCarregandoBarbearia(false));
  }, [slug]);

  useEffect(() => {
    if (!servico || !profissional || !data) {
      setHorarios([]);
      return;
    }
    setHorario(null);
    setCarregandoHorarios(true);
    api.publica
      .get(`/public/barbearias/${slug}/horarios-disponiveis?profissionalId=${profissional.id}&servicoId=${servico.id}&data=${data}`)
      .then(setHorarios)
      .catch(() => setHorarios([]))
      .finally(() => setCarregandoHorarios(false));
  }, [slug, servico, profissional, data]);

  function escolherServico(s) {
    setServico(s);
    setPasso(2);
  }

  function voltarParaServicos() {
    setPasso(1);
  }

  function irParaConfirmacao() {
    setErro('');
    setPasso(3);
  }

  async function confirmarAgendamento(e) {
    e.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      await api.publica.post(`/public/barbearias/${slug}/agendamentos`, {
        nomeCliente,
        telefoneCliente,
        profissionalId: profissional.id,
        servicoId: servico.id,
        data,
        horaInicio: horario,
      });
      setConcluido(true);
    } catch (err) {
      setErro(err.message || 'Não foi possível concluir o agendamento.');
    } finally {
      setEnviando(false);
    }
  }

  if (carregandoBarbearia) {
    return (
      <div className="pub-shell">
        <div className="pub-content" style={{ marginTop: 60 }}>
          <p>Carregando...</p>
        </div>
      </div>
    );
  }

  if (erroBarbearia) {
    return (
      <div className="pub-shell">
        <div className="pub-content" style={{ marginTop: 60 }}>
          <div className="pub-card">
            <p className="erro">{erroBarbearia}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="pub-shell">
      <header className="pub-header">
        <h1>Agendar na <span className="latao">{barbearia.nome}</span></h1>
        {barbearia.endereco && <p>{barbearia.endereco}</p>}
      </header>

      <div className="pub-content">
        <div className="pub-card">
          {concluido ? (
            <div className="pub-confirmacao">
              <div className="icone">✓</div>
              <h2>Agendamento confirmado!</h2>
              <p>
                {servico.nome} com {profissional.nome} em{' '}
                {new Date(data + 'T00:00:00').toLocaleDateString('pt-BR')} às {formatarHora(horario)}.
              </p>
            </div>
          ) : (
            <>
              <div className="pub-passos">
                <div className={`pub-passo ${passo === 1 ? 'ativo' : passo > 1 ? 'feito' : ''}`}>1. Serviço</div>
                <div className={`pub-passo ${passo === 2 ? 'ativo' : passo > 2 ? 'feito' : ''}`}>2. Horário</div>
                <div className={`pub-passo ${passo === 3 ? 'ativo' : ''}`}>3. Confirmar</div>
              </div>

              {passo === 1 && (
                <div className="opcao-lista">
                  {servicos.length === 0 && <p>Nenhum serviço disponível no momento.</p>}
                  {servicos.map((s) => (
                    <button key={s.id} className="opcao" onClick={() => escolherServico(s)}>
                      <span>{s.nome} · {s.duracaoMinutos} min</span>
                      <span className="preco">{formatarMoeda(s.preco)}</span>
                    </button>
                  ))}
                </div>
              )}

              {passo === 2 && (
                <div>
                  <div className="campo">
                    <label>Profissional</label>
                    <div className="opcao-lista">
                      {profissionais.map((p) => (
                        <button
                          key={p.id}
                          className={`opcao ${profissional?.id === p.id ? 'selecionada' : ''}`}
                          onClick={() => setProfissional(p)}
                        >
                          <span>{p.nome}</span>
                          <span className="preco">{p.funcao || ''}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="campo">
                    <label>Data</label>
                    <input type="date" min={hoje()} value={data} onChange={(e) => setData(e.target.value)} />
                  </div>

                  {profissional && (
                    <div className="campo">
                      <label>Horário</label>
                      {carregandoHorarios ? (
                        <p>Carregando horários...</p>
                      ) : horarios.length === 0 ? (
                        <p>Nenhum horário disponível nesta data.</p>
                      ) : (
                        <div className="horarios-grid">
                          {horarios.map((h) => (
                            <button
                              key={h}
                              className={`horario-slot ${horario === h ? 'selecionado' : ''}`}
                              onClick={() => setHorario(h)}
                            >
                              {formatarHora(h)}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  <div className="modal-acoes" style={{ marginTop: 24 }}>
                    <button type="button" className="btn btn-secundario" onClick={voltarParaServicos}>Voltar</button>
                    <button
                      type="button"
                      className="btn btn-latao"
                      disabled={!profissional || !horario}
                      onClick={irParaConfirmacao}
                    >
                      Continuar
                    </button>
                  </div>
                </div>
              )}

              {passo === 3 && (
                <form onSubmit={confirmarAgendamento}>
                  <p style={{ marginTop: 0 }}>
                    {servico.nome} com {profissional.nome} em{' '}
                    {new Date(data + 'T00:00:00').toLocaleDateString('pt-BR')} às {formatarHora(horario)}.
                  </p>

                  {erro && <div className="erro">{erro}</div>}

                  <div className="campo">
                    <label>Seu nome</label>
                    <input value={nomeCliente} onChange={(e) => setNomeCliente(e.target.value)} required />
                  </div>
                  <div className="campo">
                    <label>Seu telefone/WhatsApp</label>
                    <input value={telefoneCliente} onChange={(e) => setTelefoneCliente(e.target.value)} required />
                  </div>

                  <div className="modal-acoes">
                    <button type="button" className="btn btn-secundario" onClick={() => setPasso(2)}>Voltar</button>
                    <button className="btn btn-latao" disabled={enviando}>
                      {enviando ? 'Confirmando...' : 'Confirmar agendamento'}
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
