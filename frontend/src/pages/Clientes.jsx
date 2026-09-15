import { useEffect, useState } from 'react';
import { api } from '../api/http';
import Modal from '../components/Modal';
import EstadoVazio from '../components/EstadoVazio';

const VAZIO = { nome: '', telefone: '', whatsapp: '', email: '', observacoes: '' };

export default function Clientes() {
  const [clientes, setClientes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(VAZIO);
  const [erro, setErro] = useState('');

  function carregar() {
    setCarregando(true);
    api.get('/api/clientes').then(setClientes).finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm(VAZIO);
    setErro('');
    setModalAberto(true);
  }

  function abrirEdicao(cliente) {
    setEditando(cliente);
    setForm({
      nome: cliente.nome || '',
      telefone: cliente.telefone || '',
      whatsapp: cliente.whatsapp || '',
      email: cliente.email || '',
      observacoes: cliente.observacoes || '',
    });
    setErro('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    try {
      if (editando) await api.put(`/api/clientes/${editando.id}`, form);
      else await api.post('/api/clientes', form);
      setModalAberto(false);
      carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function excluir(cliente) {
    if (!confirm(`Excluir ${cliente.nome}? Essa ação não poderá ser desfeita.`)) return;
    await api.delete(`/api/clientes/${cliente.id}`);
    carregar();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Clientes</h2>
          <p>Base de clientes da barbearia.</p>
        </div>
        <button className="btn btn-latao" onClick={abrirNovo}>Novo cliente</button>
      </div>

      <div className="panel">
        {carregando ? (
          <p style={{ padding: 20 }}>Carregando...</p>
        ) : clientes.length === 0 ? (
          <EstadoVazio mensagem="Você ainda não possui clientes cadastrados." />
        ) : (
          <table>
            <thead>
              <tr><th>Nome</th><th>Telefone</th><th>E-mail</th><th></th></tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id}>
                  <td>{c.nome}</td>
                  <td>{c.telefone || '—'}</td>
                  <td>{c.email || '—'}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn btn-secundario" onClick={() => abrirEdicao(c)} style={{ marginRight: 8 }}>Editar</button>
                    <button className="btn btn-perigo" onClick={() => excluir(c)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalAberto && (
        <Modal titulo={editando ? 'Editar cliente' : 'Novo cliente'} onFechar={() => setModalAberto(false)}>
          {erro && <div className="erro">{erro}</div>}
          <form onSubmit={salvar}>
            <div className="campo">
              <label>Nome</label>
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Telefone</label>
                <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
              </div>
              <div className="campo">
                <label>WhatsApp</label>
                <input value={form.whatsapp} onChange={(e) => setForm({ ...form, whatsapp: e.target.value })} />
              </div>
            </div>
            <div className="campo">
              <label>E-mail</label>
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="campo">
              <label>Observações</label>
              <textarea rows={3} value={form.observacoes} onChange={(e) => setForm({ ...form, observacoes: e.target.value })} />
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
