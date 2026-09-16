import { useEffect, useState } from 'react';
import { api } from '../api/http';
import Modal from '../components/Modal';
import EstadoVazio from '../components/EstadoVazio';

const STATUS_LABEL = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  EM_ATENDIMENTO: 'Em atendimento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
  NAO_COMPARECEU: 'Não compareceu',
};

const STATUS_BADGE = {
  AGENDADO: 'badge-agendado',
  CONFIRMADO: 'badge-confirmado',
  EM_ATENDIMENTO: 'badge-em_atendimento',
  CONCLUIDO: 'badge-concluido',
  CANCELADO: 'badge-cancelado',
  NAO_COMPARECEU: 'badge-nao_compareceu',
};

const FORMAS_PAGAMENTO = ['DINHEIRO', 'PIX', 'DEBITO', 'CREDITO', 'OUTRO'];

const NOVO_VAZIO = { clienteId: '', profissionalId: '', servicoId: '', data: '', horaInicio: '', observacao: '' };

function hoje() {
  return new Date().toISOString().slice(0, 10);
}

export default function Agenda() {
  const [data, setData] = useState(hoje());
  const [agendamentos, setAgendamentos] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [profissionais, setProfissionais] = useState([]);
  const [servicos, setServicos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [modalConcluir, setModalConcluir] = useState(null);
  const [formaPagamento, setFormaPagamento] = useState('PIX');
  const [form, setForm] = useState(NOVO_VAZIO);

  function carregarAgendamentos() {
    setCarregando(true);
    api.get(`/api/agendamentos?data=${data}`)
      .then((pagina) => setAgendamentos(pagina.content))
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }

  useEffect(carregarAgendamentos, [data]);

  useEffect(() => {
    api.get('/api/clientes/todos').then(setClientes).catch(() => {});
    api.get('/api/profissionais').then(setProfissionais).catch(() => {});
    api.get('/api/servicos').then(setServicos).catch(() => {});
  }, []);

  function abrirNovo() {
    setForm({ ...NOVO_VAZIO, data });
    setErro('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    try {
      await api.post('/api/agendamentos', {
        clienteId: Number(form.clienteId),
        profissionalId: Number(form.profissionalId),
        servicoId: Number(form.servicoId),
        data: form.data,
        horaInicio: form.horaInicio,
        observacao: form.observacao,
      });
      setModalAberto(false);
      carregarAgendamentos();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function mudarStatus(agendamento, status) {
    await api.patch(`/api/agendamentos/${agendamento.id}/status?status=${status}`);
    carregarAgendamentos();
  }

  function abrirConcluir(agendamento) {
    setFormaPagamento('PIX');
    setModalConcluir(agendamento);
  }

  async function confirmarConclusao(e) {
    e.preventDefault();
    await api.patch(`/api/agendamentos/${modalConcluir.id}/concluir?formaPagamento=${formaPagamento}`);
    setModalConcluir(null);
    carregarAgendamentos();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Agenda</h2>
          <p>Agendamentos do dia selecionado.</p>
        </div>
        <button className="btn btn-latao" onClick={abrirNovo}>Novo agendamento</button>
      </div>

      <div className="campo" style={{ maxWidth: 220, marginBottom: 20 }}>
        <label>Data</label>
        <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
      </div>

      {erro && <div className="erro">{erro}</div>}

      <div className="panel">
        {carregando ? (
          <p style={{ padding: 20 }}>Carregando...</p>
        ) : agendamentos.length === 0 ? (
          <EstadoVazio mensagem="Nenhum agendamento para esta data." />
        ) : (
          <table>
            <thead>
              <tr><th>Hora</th><th>Cliente</th><th>Profissional</th><th>Serviço</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {agendamentos.map((a) => (
                <tr key={a.id}>
                  <td>{a.horaInicio}</td>
                  <td>{a.cliente?.nome}</td>
                  <td>{a.profissional?.nome}</td>
                  <td>{a.servico?.nome}</td>
                  <td><span className={`badge ${STATUS_BADGE[a.status]}`}>{STATUS_LABEL[a.status]}</span></td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    {a.status === 'AGENDADO' && (
                      <button className="btn btn-secundario" onClick={() => mudarStatus(a, 'CONFIRMADO')} style={{ marginRight: 8 }}>Confirmar</button>
                    )}
                    {(a.status === 'AGENDADO' || a.status === 'CONFIRMADO') && (
                      <>
                        <button className="btn btn-latao" onClick={() => abrirConcluir(a)} style={{ marginRight: 8 }}>Concluir</button>
                        <button className="btn btn-perigo" onClick={() => mudarStatus(a, 'CANCELADO')}>Cancelar</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalAberto && (
        <Modal titulo="Novo agendamento" onFechar={() => setModalAberto(false)}>
          {erro && <div className="erro">{erro}</div>}
          <form onSubmit={salvar}>
            <div className="campo">
              <label>Cliente</label>
              <select value={form.clienteId} onChange={(e) => setForm({ ...form, clienteId: e.target.value })} required>
                <option value="">Selecione</option>
                {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Profissional</label>
                <select value={form.profissionalId} onChange={(e) => setForm({ ...form, profissionalId: e.target.value })} required>
                  <option value="">Selecione</option>
                  {profissionais.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
                </select>
              </div>
              <div className="campo">
                <label>Serviço</label>
                <select value={form.servicoId} onChange={(e) => setForm({ ...form, servicoId: e.target.value })} required>
                  <option value="">Selecione</option>
                  {servicos.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
                </select>
              </div>
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Data</label>
                <input type="date" value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} required />
              </div>
              <div className="campo">
                <label>Hora de início</label>
                <input type="time" value={form.horaInicio} onChange={(e) => setForm({ ...form, horaInicio: e.target.value })} required />
              </div>
            </div>
            <div className="campo">
              <label>Observação</label>
              <textarea rows={3} value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
            </div>
            <div className="modal-acoes">
              <button type="button" className="btn btn-secundario" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button className="btn btn-latao">Salvar</button>
            </div>
          </form>
        </Modal>
      )}

      {modalConcluir && (
        <Modal titulo="Concluir agendamento" onFechar={() => setModalConcluir(null)}>
          <p style={{ marginTop: 0 }}>
            Isso gera automaticamente a receita e a comissão do profissional para este atendimento.
          </p>
          <form onSubmit={confirmarConclusao}>
            <div className="campo">
              <label>Forma de pagamento</label>
              <select value={formaPagamento} onChange={(e) => setFormaPagamento(e.target.value)}>
                {FORMAS_PAGAMENTO.map((f) => <option key={f} value={f}>{f}</option>)}
              </select>
            </div>
            <div className="modal-acoes">
              <button type="button" className="btn btn-secundario" onClick={() => setModalConcluir(null)}>Cancelar</button>
              <button className="btn btn-latao">Concluir</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
