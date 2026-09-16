import { useEffect, useRef, useState } from 'react';
import { api } from '../api/http';
import RecursoBloqueado from '../components/RecursoBloqueado';

const SUGESTOES_PADRAO = [
  'Como está minha barbearia hoje?',
  'Quanto faturei este mês?',
  'Quais serviços vendem mais?',
  'Como estão meus profissionais?',
  'Quais clientes preciso reativar?',
];

export default function AssistenteIa() {
  const [mensagens, setMensagens] = useState([]);
  const [pergunta, setPergunta] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [sugestoes, setSugestoes] = useState(SUGESTOES_PADRAO);
  const [bloqueio, setBloqueio] = useState(null);
  const [erro, setErro] = useState('');
  const fimDaListaRef = useRef(null);

  useEffect(() => {
    api.get('/api/assistente/sugestoes')
      .then(setSugestoes)
      .catch((e) => {
        if (e.dados?.upgradeNecessario) setBloqueio(e.dados);
      });
  }, []);

  useEffect(() => {
    fimDaListaRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [mensagens, enviando]);

  async function enviar(texto) {
    const perguntaFinal = (texto ?? pergunta).trim();
    if (!perguntaFinal || enviando) return;

    setErro('');
    setMensagens((atual) => [...atual, { autor: 'usuario', texto: perguntaFinal }]);
    setPergunta('');
    setEnviando(true);

    try {
      const resposta = await api.post('/api/assistente/perguntar', { pergunta: perguntaFinal });
      setMensagens((atual) => [...atual, { autor: 'assistente', texto: resposta.resposta }]);
    } catch (e) {
      if (e.dados?.upgradeNecessario) {
        setBloqueio(e.dados);
        setMensagens((atual) => atual.slice(0, -1));
      } else {
        setErro(e.message);
      }
    } finally {
      setEnviando(false);
    }
  }

  function aoEnviarForm(e) {
    e.preventDefault();
    enviar();
  }

  if (bloqueio) {
    return (
      <div>
        <div className="page-header">
          <div>
            <h2>Assistente IA</h2>
            <p>Converse com a IA sobre os dados da sua barbearia.</p>
          </div>
        </div>
        <RecursoBloqueado planoNecessario={bloqueio.planoNecessario} mensagem={bloqueio.mensagem} />
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Assistente IA</h2>
          <p>Pergunte sobre faturamento, atendimentos, comissões e clientes — sempre com dados reais.</p>
        </div>
      </div>

      {erro && <div className="erro">{erro}</div>}

      <div className="panel chat-assistente">
        <div className="chat-mensagens">
          {mensagens.length === 0 && (
            <div className="chat-digitando">Faça uma pergunta ou escolha uma sugestão abaixo pra começar.</div>
          )}
          {mensagens.map((m, i) => (
            <div key={i} className={`chat-bolha ${m.autor === 'usuario' ? 'chat-bolha-usuario' : 'chat-bolha-assistente'}`}>
              {m.texto}
            </div>
          ))}
          {enviando && <div className="chat-digitando">Consultando os dados...</div>}
          <div ref={fimDaListaRef} />
        </div>

        {sugestoes.length > 0 && (
          <div className="chat-sugestoes">
            {sugestoes.map((s) => (
              <button key={s} type="button" className="chat-sugestao" onClick={() => enviar(s)} disabled={enviando}>
                {s}
              </button>
            ))}
          </div>
        )}

        <form className="chat-form" onSubmit={aoEnviarForm}>
          <input
            value={pergunta}
            onChange={(e) => setPergunta(e.target.value)}
            placeholder="Pergunte algo sobre sua barbearia..."
            disabled={enviando}
          />
          <button className="btn btn-latao" disabled={enviando || !pergunta.trim()}>Enviar</button>
        </form>
      </div>
    </div>
  );
}
