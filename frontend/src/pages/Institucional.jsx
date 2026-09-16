import { Link } from 'react-router-dom';
import '../styles/institucional.css';

const RECURSOS = [
  {
    icone: '📅',
    titulo: 'Agenda inteligente',
    texto: 'Cada profissional com seu próprio horário. O sistema nunca deixa dois clientes marcarem no mesmo horário.',
  },
  {
    icone: '💳',
    titulo: 'Cobrança automática',
    texto: 'Assinatura mensal cobrada sozinha via cartão, sem boleto pra imprimir ou pix pra conferir na mão.',
  },
  {
    icone: '💈',
    titulo: 'Agendamento público',
    texto: 'Um link só seu, pra clientes marcarem horário sozinhos, a qualquer hora, sem precisar te chamar no WhatsApp.',
  },
  {
    icone: '📊',
    titulo: 'Financeiro sem planilha',
    texto: 'Receitas, despesas e ticket médio calculados automaticamente a cada atendimento concluído.',
  },
  {
    icone: '✂️',
    titulo: 'Comissão automática',
    texto: 'Cada profissional já sai com a comissão calculada certinha, sem contar recibo por recibo no fim do mês.',
  },
  {
    icone: '🔐',
    titulo: 'Multi-barbearia',
    texto: 'Cada barbearia com seus próprios dados, totalmente isolados — mesmo sistema, zero bagunça entre clientes.',
  },
];

const DEPOIMENTOS = [
  {
    nome: 'Rafael M.',
    papel: 'Dono de barbearia · SP',
    texto: '“Parei de anotar horário em caderno. Hoje o cliente marca sozinho e eu só recebo a notificação.”',
  },
  {
    nome: 'Diego S.',
    papel: 'Barbeiro · RJ',
    texto: '“Sei exatamente quanto vou receber de comissão sem precisar perguntar pro dono. Fica tudo na tela.”',
  },
  {
    nome: 'Bruna A.',
    papel: 'Dona de barbearia · MG',
    texto: '“A cobrança da assinatura cai sozinha todo mês. Não preciso mais ficar de olho pra não esquecer de pagar.”',
  },
];

export default function Institucional() {
  return (
    <div className="nb">
      <nav className="nb-nav">
        <div className="nb-logo">
          <span className="nb-logo-mark">B</span>
          BarberPro
        </div>
        <div className="nb-nav-links">
          <a href="#recursos">Recursos</a>
          <a href="#como-funciona">Como funciona</a>
          <a href="#depoimentos">Depoimentos</a>
        </div>
        <div className="nb-nav-actions">
          <Link to="/login" className="nb-btn nb-btn-secundario nb-btn-sm">Entrar</Link>
          <Link to="/registro" className="nb-btn nb-btn-primario nb-btn-sm">Começar grátis</Link>
        </div>
      </nav>

      <header className="nb-hero">
        <div className="nb-hero-grid">
          <div>
            <span className="nb-badge">
              <span className="nb-badge-dot" />
              Novo: agenda pública para seus clientes
            </span>
            <h1>
              Sua barbearia <span className="destaque">rodando sozinha</span>
            </h1>
            <p className="sub">
              Agenda, financeiro, comissão e cobrança de assinatura — tudo automático,
              num só sistema feito pra barbearia de verdade.
            </p>
            <div className="nb-hero-ctas">
              <Link to="/registro" className="nb-btn nb-btn-primario">Testar grátis por 14 dias</Link>
              <a href="#como-funciona" className="nb-btn nb-btn-secundario">Ver como funciona</a>
            </div>
          </div>

          <div className="nb-mock">
            <div className="nb-mock-bar">
              <span className="nb-mock-dot" style={{ background: '#ff5f57' }} />
              <span className="nb-mock-dot" style={{ background: '#febc2e' }} />
              <span className="nb-mock-dot" style={{ background: '#28c840' }} />
            </div>
            <div className="nb-mock-body">
              <div className="nb-mock-row">
                <div className="nb-mock-card destaque">
                  <small>Faturamento hoje</small>
                  <strong>R$ 840</strong>
                </div>
                <div className="nb-mock-card">
                  <small>Agendamentos</small>
                  <strong>12</strong>
                </div>
                <div className="nb-mock-card">
                  <small>Clientes</small>
                  <strong>238</strong>
                </div>
              </div>
              <div className="nb-mock-card">
                <small>Faturamento da semana</small>
                <div className="nb-mock-bars" style={{ marginTop: 10 }}>
                  <span style={{ height: '40%' }} />
                  <span style={{ height: '65%' }} />
                  <span style={{ height: '50%' }} />
                  <span style={{ height: '85%' }} />
                  <span style={{ height: '60%' }} />
                  <span style={{ height: '95%' }} />
                  <span style={{ height: '70%' }} />
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <div className="nb-marquee-wrap">
        <div className="nb-marquee">
          {Array.from({ length: 2 }).map((_, i) => (
            <div key={i} style={{ display: 'flex', gap: 48 }}>
              <span>Agenda automática</span>
              <span>·</span>
              <span>Cobrança automática</span>
              <span>·</span>
              <span>Comissão automática</span>
              <span>·</span>
              <span>Cliente agenda sozinho</span>
              <span>·</span>
              <span>Sem caderno, sem planilha</span>
              <span>·</span>
            </div>
          ))}
        </div>
      </div>

      <section className="nb-section">
        <div className="nb-section-inner">
          <div className="nb-compare">
            <div className="nb-compare-card problema">
              <h3>Sem o BarberPro</h3>
              <ul>
                <li><span className="marca">✕</span> Agenda no caderno ou no WhatsApp, com risco de marcar dois clientes no mesmo horário</li>
                <li><span className="marca">✕</span> Comissão calculada na mão, recibo por recibo</li>
                <li><span className="marca">✕</span> Cliente precisa te chamar pra saber se tem horário livre</li>
                <li><span className="marca">✕</span> Cobrança da mensalidade dependendo de alguém lembrar</li>
              </ul>
            </div>
            <div className="nb-compare-card solucao">
              <h3>Com o BarberPro</h3>
              <ul>
                <li><span className="marca">✓</span> Agenda automática, sem conflito de horário entre profissionais</li>
                <li><span className="marca">✓</span> Comissão calculada sozinha a cada atendimento concluído</li>
                <li><span className="marca">✓</span> Cliente marca o próprio horário, a qualquer hora do dia</li>
                <li><span className="marca">✓</span> Assinatura cobrada automaticamente, todo mês, sem esquecer</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section className="nb-section nb-features-section" id="recursos">
        <div className="nb-section-inner">
          <div className="nb-section-head">
            <span className="nb-eyebrow">Recursos</span>
            <h2>Tudo que uma barbearia precisa, num lugar só</h2>
            <p>Sem depender de três aplicativos diferentes pra fazer a barbearia funcionar.</p>
          </div>
          <div className="nb-features-grid">
            {RECURSOS.map((r) => (
              <div key={r.titulo} className="nb-feature-card">
                <div className="nb-feature-icon">{r.icone}</div>
                <h3>{r.titulo}</h3>
                <p>{r.texto}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="nb-section nb-how-section" id="como-funciona">
        <div className="nb-section-inner">
          <div className="nb-section-head">
            <span className="nb-eyebrow">Como funciona</span>
            <h2>No ar em poucos minutos</h2>
            <p>Sem instalar nada, sem treinamento complicado.</p>
          </div>
          <div className="nb-how-flow">
            <div className="nb-how-step">
              <div className="nb-how-num">1</div>
              <h3>Crie sua conta</h3>
              <p>Cadastra sua barbearia e já começa com 14 dias de teste grátis, sem cartão.</p>
            </div>
            <div className="nb-how-step">
              <div className="nb-how-num">2</div>
              <h3>Cadastre serviços e profissionais</h3>
              <p>Defina preços, duração dos serviços e os horários de cada profissional.</p>
            </div>
            <div className="nb-how-step">
              <div className="nb-how-num">3</div>
              <h3>Compartilhe seu link</h3>
              <p>Envie o link da sua agenda pública nas redes sociais e comece a receber agendamentos.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="nb-section">
        <div className="nb-section-inner">
          <div className="nb-section-head">
            <span className="nb-eyebrow">Pra quem é</span>
            <h2>Feito pra todo mundo da barbearia</h2>
          </div>
          <div className="nb-personas-grid">
            <div className="nb-persona-card">
              <span className="nb-persona-pill">Dono da barbearia</span>
              <h3>Controle total, sem esforço</h3>
              <p>Veja faturamento, agendamentos e comissões em tempo real, sem precisar fechar o caixa na mão.</p>
            </div>
            <div className="nb-persona-card">
              <span className="nb-persona-pill">Profissional</span>
              <h3>Sua agenda, sua comissão</h3>
              <p>Acompanhe seus próprios atendimentos e o quanto tem a receber, sem depender de ninguém pra saber.</p>
            </div>
            <div className="nb-persona-card">
              <span className="nb-persona-pill">Cliente final</span>
              <h3>Agenda quando quiser</h3>
              <p>Marca o próprio horário direto pelo celular, sem precisar mandar mensagem nem esperar resposta.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="nb-section nb-testimonials-section" id="depoimentos">
        <div className="nb-section-inner">
          <div className="nb-section-head">
            <span className="nb-eyebrow">Depoimentos</span>
            <h2 style={{ color: 'var(--tinta)' }}>Quem já roda com o BarberPro</h2>
          </div>
          <div className="nb-testimonials-grid">
            {DEPOIMENTOS.map((d) => (
              <div key={d.nome} className="nb-testimonial-card">
                <div className="nb-stars">★★★★★</div>
                <p className="quote">{d.texto}</p>
                <div className="nb-testimonial-autor">
                  <div className="nb-testimonial-avatar">{d.nome.charAt(0)}</div>
                  <div>
                    <strong>{d.nome}</strong>
                    <span>{d.papel}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="nb-section nb-cta-final">
        <h2>Pronto pra tirar sua barbearia do caderno?</h2>
        <p>14 dias grátis, sem precisar cadastrar cartão de crédito.</p>
        <div className="nb-hero-ctas">
          <Link to="/registro" className="nb-btn nb-btn-primario">Criar minha barbearia grátis</Link>
        </div>
      </section>

      <footer className="nb-footer">
        <div className="nb-footer-grid">
          <div className="nb-footer-brand">
            <div className="nb-logo">
              <span className="nb-logo-mark">B</span>
              BarberPro
            </div>
            <p>O sistema de gestão feito pra barbearia: agenda, financeiro e cobrança de assinatura, tudo automático.</p>
          </div>
          <div className="nb-footer-col">
            <h4>Produto</h4>
            <ul>
              <li><a href="#recursos">Recursos</a></li>
              <li><a href="#como-funciona">Como funciona</a></li>
              <li><Link to="/registro">Começar grátis</Link></li>
            </ul>
          </div>
          <div className="nb-footer-col">
            <h4>Conta</h4>
            <ul>
              <li><Link to="/login">Entrar</Link></li>
              <li><Link to="/registro">Criar barbearia</Link></li>
            </ul>
          </div>
          <div className="nb-footer-col">
            <h4>Contato</h4>
            <ul>
              <li><a href="mailto:contato@barberpro.com.br">contato@barberpro.com.br</a></li>
            </ul>
          </div>
          <div className="nb-footer-col">
            <h4>Legal</h4>
            <ul>
              <li><Link to="/privacidade">Política de Privacidade</Link></li>
              <li><Link to="/termos">Termos de Uso</Link></li>
            </ul>
          </div>
        </div>
        <div className="nb-footer-bottom">
          <span>© {new Date().getFullYear()} BarberPro. Todos os direitos reservados.</span>
        </div>
      </footer>
    </div>
  );
}
