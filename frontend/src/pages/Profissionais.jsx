import { useEffect, useRef, useState } from 'react';
import { api } from '../api/http';
import Modal from '../components/Modal';
import EstadoVazio from '../components/EstadoVazio';

const VAZIO = {
  nome: '', telefone: '', email: '', funcao: '',
  diasTrabalho: '', horarioInicio: '', horarioFim: '',
  percentualComissao: '', ativo: true,
};

export default function Profissionais() {
  const [profissionais, setProfissionais] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(VAZIO);
  const [erro, setErro] = useState('');
  const modalRef = useRef(null);

  const [acessoAberto, setAcessoAberto] = useState(false);
  const [profissionalAcesso, setProfissionalAcesso] = useState(null);
  const [formAcesso, setFormAcesso] = useState({ email: '', senha: '' });
  const [erroAcesso, setErroAcesso] = useState('');
  const [sucessoAcesso, setSucessoAcesso] = useState('');

  function carregar() {
    setCarregando(true);
    api.get('/api/profissionais').then(setProfissionais).finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm(VAZIO);
    setErro('');
    setModalAberto(true);
  }

  function abrirEdicao(profissional) {
    setEditando(profissional);
    setForm({
      nome: profissional.nome || '',
      telefone: profissional.telefone || '',
      email: profissional.email || '',
      funcao: profissional.funcao || '',
      diasTrabalho: profissional.diasTrabalho || '',
      horarioInicio: profissional.horarioInicio || '',
      horarioFim: profissional.horarioFim || '',
      percentualComissao: profissional.percentualComissao ?? '',
      ativo: profissional.ativo,
    });
    setErro('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    try {
      const payload = { ...form, percentualComissao: Number(form.percentualComissao || 0) };
      if (editando) await api.put(`/api/profissionais/${editando.id}`, payload);
      else await api.post('/api/profissionais', payload);
      setModalAberto(false);
      carregar();
    } catch (err) {
      setErro(err.message);
      modalRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  async function excluir(profissional) {
    if (!confirm(`Excluir ${profissional.nome}? Essa ação não poderá ser desfeita.`)) return;
    await api.delete(`/api/profissionais/${profissional.id}`);
    carregar();
  }

  function abrirAcesso(profissional) {
    setProfissionalAcesso(profissional);
    setFormAcesso({ email: profissional.email || '', senha: '' });
    setErroAcesso('');
    setSucessoAcesso('');
    setAcessoAberto(true);
  }

  async function criarAcesso(e) {
    e.preventDefault();
    setErroAcesso('');
    try {
      await api.post(`/api/profissionais/${profissionalAcesso.id}/acesso`, formAcesso);
      setSucessoAcesso('Acesso criado! Já pode passar o e-mail e a senha pro profissional.');
    } catch (err) {
      setErroAcesso(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Profissionais</h2>
          <p>Equipe da barbearia e comissão de cada um.</p>
        </div>
        <button className="btn btn-latao" onClick={abrirNovo}>Novo profissional</button>
      </div>

      <div className="panel">
        {carregando ? (
          <p style={{ padding: 20 }}>Carregando...</p>
        ) : profissionais.length === 0 ? (
          <EstadoVazio mensagem="Nenhum profissional cadastrado ainda." />
        ) : (
          <table>
            <thead>
              <tr><th>Nome</th><th>Função</th><th>Comissão</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {profissionais.map((p) => (
                <tr key={p.id}>
                  <td>{p.nome}</td>
                  <td>{p.funcao || '—'}</td>
                  <td>{Number(p.percentualComissao || 0)}%</td>
                  <td><span className={`badge ${p.ativo ? 'badge-confirmado' : 'badge-cancelado'}`}>{p.ativo ? 'Ativo' : 'Inativo'}</span></td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn btn-secundario" onClick={() => abrirAcesso(p)} style={{ marginRight: 8 }}>Acesso ao painel</button>
                    <button className="btn btn-secundario" onClick={() => abrirEdicao(p)} style={{ marginRight: 8 }}>Editar</button>
                    <button className="btn btn-perigo" onClick={() => excluir(p)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalAberto && (
        <Modal ref={modalRef} titulo={editando ? 'Editar profissional' : 'Novo profissional'} onFechar={() => setModalAberto(false)}>
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
                <label>E-mail</label>
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Função</label>
                <input value={form.funcao} onChange={(e) => setForm({ ...form, funcao: e.target.value })} placeholder="Barbeiro" />
              </div>
              <div className="campo">
                <label>Comissão (%)</label>
                <input type="number" step="0.01" value={form.percentualComissao} onChange={(e) => setForm({ ...form, percentualComissao: e.target.value })} required />
              </div>
            </div>
            <div className="campo">
              <label>Dias de trabalho</label>
              <input value={form.diasTrabalho} onChange={(e) => setForm({ ...form, diasTrabalho: e.target.value })} placeholder="SEG,TER,QUA,QUI,SEX" />
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Horário início</label>
                <input value={form.horarioInicio} onChange={(e) => setForm({ ...form, horarioInicio: e.target.value })} placeholder="09:00" />
              </div>
              <div className="campo">
                <label>Horário fim</label>
                <input value={form.horarioFim} onChange={(e) => setForm({ ...form, horarioFim: e.target.value })} placeholder="18:00" />
              </div>
            </div>
            <div className="campo">
              <label>
                <input type="checkbox" style={{ width: 'auto', marginRight: 8 }} checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                Profissional ativo
              </label>
            </div>
            <div className="modal-acoes">
              <button type="button" className="btn btn-secundario" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button className="btn btn-latao">Salvar</button>
            </div>
          </form>
        </Modal>
      )}

      {acessoAberto && (
        <Modal titulo={`Acesso ao painel — ${profissionalAcesso?.nome}`} onFechar={() => setAcessoAberto(false)}>
          {erroAcesso && <div className="erro">{erroAcesso}</div>}
          {sucessoAcesso ? (
            <div className="sucesso">{sucessoAcesso}</div>
          ) : (
            <>
              <p style={{ marginTop: 0, color: 'var(--texto-suave)' }}>
                Com esse login, {profissionalAcesso?.nome} vai poder entrar no painel e ver
                só a própria agenda e comissão — nunca os dados da barbearia toda.
              </p>
              <form onSubmit={criarAcesso}>
                <div className="campo">
                  <label>E-mail de login</label>
                  <input
                    type="email"
                    value={formAcesso.email}
                    onChange={(e) => setFormAcesso({ ...formAcesso, email: e.target.value })}
                    required
                  />
                </div>
                <div className="campo">
                  <label>Senha</label>
                  <input
                    type="password"
                    value={formAcesso.senha}
                    onChange={(e) => setFormAcesso({ ...formAcesso, senha: e.target.value })}
                    required
                  />
                </div>
                <div className="modal-acoes">
                  <button type="button" className="btn btn-secundario" onClick={() => setAcessoAberto(false)}>Cancelar</button>
                  <button className="btn btn-latao">Criar acesso</button>
                </div>
              </form>
            </>
          )}
        </Modal>
      )}
    </div>
  );
}
