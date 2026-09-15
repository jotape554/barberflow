import { useEffect, useState } from 'react';
import { api } from '../api/http';
import Modal from '../components/Modal';
import EstadoVazio from '../components/EstadoVazio';

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

const ABAS = [
  { id: 'receitas', label: 'Receitas' },
  { id: 'despesas', label: 'Despesas' },
  { id: 'comissoes', label: 'Comissões' },
];

const DESPESA_VAZIA = { descricao: '', categoria: '', valor: '', data: '', observacao: '' };

export default function Financeiro() {
  const [aba, setAba] = useState('receitas');
  const [receitas, setReceitas] = useState([]);
  const [despesas, setDespesas] = useState([]);
  const [comissoes, setComissoes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [formDespesa, setFormDespesa] = useState(DESPESA_VAZIA);

  function carregar() {
    setCarregando(true);
    Promise.all([
      api.get('/api/receitas'),
      api.get('/api/despesas'),
      api.get('/api/comissoes'),
    ])
      .then(([r, d, c]) => {
        setReceitas(r);
        setDespesas(d);
        setComissoes(c);
      })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  function abrirNovaDespesa() {
    setFormDespesa({ ...DESPESA_VAZIA, data: new Date().toISOString().slice(0, 10) });
    setErro('');
    setModalAberto(true);
  }

  async function salvarDespesa(e) {
    e.preventDefault();
    try {
      await api.post('/api/despesas', { ...formDespesa, valor: Number(formDespesa.valor) });
      setModalAberto(false);
      carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function excluirDespesa(despesa) {
    if (!confirm(`Excluir a despesa "${despesa.descricao}"?`)) return;
    await api.delete(`/api/despesas/${despesa.id}`);
    carregar();
  }

  async function marcarComissaoPaga(comissao) {
    await api.patch(`/api/comissoes/${comissao.id}/pagar`);
    carregar();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Financeiro</h2>
          <p>Receitas geradas pelos atendimentos, despesas e comissões.</p>
        </div>
        {aba === 'despesas' && (
          <button className="btn btn-latao" onClick={abrirNovaDespesa}>Nova despesa</button>
        )}
      </div>

      {erro && <div className="erro">{erro}</div>}

      <div className="abas" style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {ABAS.map((a) => (
          <button
            key={a.id}
            className={aba === a.id ? 'btn btn-latao' : 'btn btn-secundario'}
            onClick={() => setAba(a.id)}
          >
            {a.label}
          </button>
        ))}
      </div>

      <div className="panel">
        {carregando ? (
          <p style={{ padding: 20 }}>Carregando...</p>
        ) : aba === 'receitas' ? (
          receitas.length === 0 ? (
            <EstadoVazio mensagem="Nenhuma receita registrada ainda." />
          ) : (
            <table>
              <thead>
                <tr><th>Data</th><th>Cliente</th><th>Serviço</th><th>Forma</th><th>Valor</th></tr>
              </thead>
              <tbody>
                {receitas.map((r) => (
                  <tr key={r.id}>
                    <td>{r.data}</td>
                    <td>{r.cliente?.nome || '—'}</td>
                    <td>{r.servico?.nome || '—'}</td>
                    <td>{r.formaPagamento}</td>
                    <td>{formatarMoeda(r.valor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        ) : aba === 'despesas' ? (
          despesas.length === 0 ? (
            <EstadoVazio mensagem="Nenhuma despesa lançada ainda." />
          ) : (
            <table>
              <thead>
                <tr><th>Data</th><th>Descrição</th><th>Categoria</th><th>Valor</th><th></th></tr>
              </thead>
              <tbody>
                {despesas.map((d) => (
                  <tr key={d.id}>
                    <td>{d.data}</td>
                    <td>{d.descricao}</td>
                    <td>{d.categoria || '—'}</td>
                    <td>{formatarMoeda(d.valor)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button className="btn btn-perigo" onClick={() => excluirDespesa(d)}>Excluir</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        ) : comissoes.length === 0 ? (
          <EstadoVazio mensagem="Nenhuma comissão gerada ainda." />
        ) : (
          <table>
            <thead>
              <tr><th>Data</th><th>Profissional</th><th>%</th><th>Valor</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {comissoes.map((c) => (
                <tr key={c.id}>
                  <td>{c.data}</td>
                  <td>{c.profissional?.nome || '—'}</td>
                  <td>{Number(c.percentual)}%</td>
                  <td>{formatarMoeda(c.valor)}</td>
                  <td><span className={`badge ${c.paga ? 'badge-confirmado' : 'badge-cancelado'}`}>{c.paga ? 'Paga' : 'Pendente'}</span></td>
                  <td style={{ textAlign: 'right' }}>
                    {!c.paga && (
                      <button className="btn btn-secundario" onClick={() => marcarComissaoPaga(c)}>Marcar como paga</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalAberto && (
        <Modal titulo="Nova despesa" onFechar={() => setModalAberto(false)}>
          {erro && <div className="erro">{erro}</div>}
          <form onSubmit={salvarDespesa}>
            <div className="campo">
              <label>Descrição</label>
              <input value={formDespesa.descricao} onChange={(e) => setFormDespesa({ ...formDespesa, descricao: e.target.value })} required />
            </div>
            <div className="form-grid">
              <div className="campo">
                <label>Categoria</label>
                <input value={formDespesa.categoria} onChange={(e) => setFormDespesa({ ...formDespesa, categoria: e.target.value })} />
              </div>
              <div className="campo">
                <label>Valor (R$)</label>
                <input type="number" step="0.01" value={formDespesa.valor} onChange={(e) => setFormDespesa({ ...formDespesa, valor: e.target.value })} required />
              </div>
            </div>
            <div className="campo">
              <label>Data</label>
              <input type="date" value={formDespesa.data} onChange={(e) => setFormDespesa({ ...formDespesa, data: e.target.value })} required />
            </div>
            <div className="campo">
              <label>Observação</label>
              <textarea rows={3} value={formDespesa.observacao} onChange={(e) => setFormDespesa({ ...formDespesa, observacao: e.target.value })} />
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
