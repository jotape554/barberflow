import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/http';
import Modal from '../components/Modal';
import EstadoVazio from '../components/EstadoVazio';

const VAZIO = { nome: '', descricao: '', preco: '', duracaoMinutos: '', ativo: true };

export default function Servicos() {
  const [servicos, setServicos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(VAZIO);
  const [erro, setErro] = useState('');
  const [searchParams, setSearchParams] = useSearchParams();

  function carregar() {
    setCarregando(true);
    api.get('/api/servicos').then(setServicos).finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  // Vindo do atalho "Novo serviço" do painel (?novo=1) — já abre o formulário.
  useEffect(() => {
    if (searchParams.get('novo') === '1') {
      abrirNovo();
      setSearchParams({}, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirNovo() {
    setEditando(null);
    setForm(VAZIO);
    setErro('');
    setModalAberto(true);
  }

  function abrirEdicao(servico) {
    setEditando(servico);
    setForm({
      nome: servico.nome,
      descricao: servico.descricao || '',
      preco: servico.preco,
      duracaoMinutos: servico.duracaoMinutos,
      ativo: servico.ativo,
    });
    setErro('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    try {
      const payload = { ...form, preco: Number(form.preco), duracaoMinutos: Number(form.duracaoMinutos) };
      if (editando) await api.put(`/api/servicos/${editando.id}`, payload);
      else await api.post('/api/servicos', payload);
      setModalAberto(false);
      carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function excluir(servico) {
    if (!confirm(`Excluir "${servico.nome}"? Essa ação não poderá ser desfeita.`)) return;
    await api.delete(`/api/servicos/${servico.id}`);
    carregar();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Serviços</h2>
          <p>O que a barbearia oferece — preço e duração de cada um.</p>
        </div>
        <button className="btn btn-latao" onClick={abrirNovo}>Novo serviço</button>
      </div>

      <div className="panel">
        {carregando ? (
          <p style={{ padding: 20 }}>Carregando...</p>
        ) : servicos.length === 0 ? (
          <EstadoVazio mensagem="Nenhum serviço cadastrado ainda." />
        ) : (
          <table>
            <thead>
              <tr><th>Nome</th><th>Preço</th><th>Duração</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {servicos.map((s) => (
                <tr key={s.id}>
                  <td>{s.nome}</td>
                  <td>{Number(s.preco).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</td>
                  <td>{s.duracaoMinutos} min</td>
                  <td><span className={`badge ${s.ativo ? 'badge-confirmado' : 'badge-cancelado'}`}>{s.ativo ? 'Ativo' : 'Inativo'}</span></td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn btn-secundario" onClick={() => abrirEdicao(s)} style={{ marginRight: 8 }}>Editar</button>
                    <button className="btn btn-perigo" onClick={() => excluir(s)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalAberto && (
        <Modal titulo={editando ? 'Editar serviço' : 'Novo serviço'} onFechar={() => setModalAberto(false)}>
          {erro && <div className="erro">{erro}</div>}
          <form onSubmit={salvar}>
            <div className="campo">
              <label>Nome</label>
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </div>
            <div className="campo">
              <label>Descrição</label>
              <input value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Preço (R$)</label>
                <input type="number" step="0.01" value={form.preco} onChange={(e) => setForm({ ...form, preco: e.target.value })} required />
              </div>
              <div className="campo">
                <label>Duração (minutos)</label>
                <input type="number" value={form.duracaoMinutos} onChange={(e) => setForm({ ...form, duracaoMinutos: e.target.value })} required />
              </div>
            </div>
            <div className="campo">
              <label>
                <input type="checkbox" style={{ width: 'auto', marginRight: 8 }} checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                Serviço ativo (aparece no agendamento público)
              </label>
            </div>
            <div className="modal-acoes">
              <button type="button" className="btn btn-secundario" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button className="btn btn-latao">Salvar</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
