import { Link } from 'react-router-dom';

export default function PoliticaPrivacidade() {
  return (
    <div className="doc-legal">
      <div className="doc-topo">
        <Link to="/">← Voltar para o início</Link>
        <h1>Política de Privacidade</h1>
        <div className="doc-atualizado">Última atualização: 16 de setembro de 2026</div>
      </div>

      <div className="doc-aviso">
        Este documento descreve, de forma simples e direta, quais dados o BarberFlow coleta e
        como eles são usados. Não substitui aconselhamento jurídico profissional.
      </div>

      <p>
        O BarberFlow é um sistema de gestão para barbearias. Esta política se aplica a dois
        tipos de dado: os dados da <strong>barbearia que contrata o BarberFlow</strong> (dono,
        gerentes, profissionais) e os dados dos <strong>clientes finais dessa barbearia</strong>
        {' '}(as pessoas que agendam um corte).
      </p>

      <h2>1. Quais dados coletamos</h2>
      <p><strong>Da barbearia (quem assina o BarberFlow):</strong></p>
      <ul>
        <li>Nome, e-mail e senha (armazenada de forma criptografada, nunca em texto simples) do administrador e dos usuários com acesso ao painel;</li>
        <li>Nome da barbearia, endereço, WhatsApp, Instagram e logotipo, quando informados;</li>
        <li>Dados dos profissionais cadastrados: nome, telefone, e-mail, função, horários de trabalho e percentual de comissão;</li>
        <li>Dados de cobrança da assinatura (processados pela Stripe — ver seção 3; não guardamos número de cartão de crédito).</li>
      </ul>
      <p><strong>Dos clientes finais da barbearia (quem agenda um horário):</strong></p>
      <ul>
        <li>Nome, telefone e, quando informado, e-mail e observações sobre o cliente;</li>
        <li>Histórico de agendamentos, serviços realizados e forma de pagamento utilizada.</li>
      </ul>
      <p>
        Esses dados de clientes finais pertencem à barbearia que os cadastrou — o BarberFlow atua
        apenas como o sistema que armazena e organiza essa informação em nome da barbearia.
      </p>

      <h2>2. Para que usamos esses dados</h2>
      <ul>
        <li>Fazer o sistema funcionar: agenda, cadastro de clientes, cálculo automático de comissão, geração de relatórios financeiros;</li>
        <li>Permitir que o cliente final marque horário pela página pública de agendamento, sem precisar criar login;</li>
        <li>Enviar e-mails operacionais, como redefinição de senha;</li>
        <li>Cobrar a assinatura mensal do plano contratado pela barbearia;</li>
        <li>Quando a barbearia usa o Assistente de IA (recurso do plano Premium), enviamos os dados financeiros e operacionais necessários para responder à pergunta feita — ver seção 3.</li>
      </ul>
      <p>Não vendemos dados pessoais para terceiros, e não usamos os dados para publicidade.</p>

      <h2>3. Com quem compartilhamos dados</h2>
      <p>Usamos os seguintes serviços de terceiros para o BarberFlow funcionar:</p>
      <ul>
        <li><strong>Stripe</strong> — processa o pagamento da assinatura. O BarberFlow nunca vê nem armazena o número do cartão de crédito;</li>
        <li><strong>SendGrid</strong> — envia e-mails do sistema, como o de redefinição de senha;</li>
        <li><strong>Anthropic (Claude)</strong> — quando a barbearia usa o Assistente de IA, a pergunta feita e os dados financeiros/operacionais necessários para respondê-la são enviados a esse serviço para gerar a resposta. Isso só acontece quando a barbearia usa esse recurso ativamente;</li>
        <li><strong>Railway</strong> — hospeda o banco de dados e os servidores do BarberFlow.</li>
      </ul>
      <p>
        Cada um desses serviços tem sua própria política de privacidade e é responsável pela
        segurança dos dados que processa em nosso nome.
      </p>

      <h2>4. Como protegemos os dados</h2>
      <ul>
        <li>Senhas são armazenadas com criptografia (nunca em texto legível);</li>
        <li>Toda comunicação com o sistema acontece via HTTPS (conexão criptografada);</li>
        <li>Uma barbearia nunca tem acesso aos dados de outra barbearia — o sistema separa tecnicamente os dados de cada conta;</li>
        <li>Dentro de uma mesma barbearia, o acesso é restrito por função: um profissional só vê a própria agenda e comissão, não os dados financeiros gerais nem os dados de outros profissionais.</li>
      </ul>

      <h2>5. Por quanto tempo guardamos os dados</h2>
      <p>
        Os dados ficam armazenados enquanto a conta da barbearia estiver ativa. Se a assinatura
        for cancelada, os dados podem ser mantidos por um período adicional para eventual
        reativação ou cumprimento de obrigação legal, e depois removidos. Você pode pedir a
        exclusão antecipada dos seus dados a qualquer momento pelo contato abaixo.
      </p>

      <h2>6. Seus direitos (LGPD)</h2>
      <p>Conforme a Lei Geral de Proteção de Dados (Lei 13.709/2018), você pode solicitar a qualquer momento:</p>
      <ul>
        <li>Confirmação de que tratamos seus dados, e acesso a eles;</li>
        <li>Correção de dados incompletos, inexatos ou desatualizados;</li>
        <li>Exclusão dos seus dados pessoais;</li>
        <li>Portabilidade dos dados para outro fornecedor;</li>
        <li>Informação sobre com quem compartilhamos seus dados.</li>
      </ul>
      <p>
        Se você é um cliente final de uma barbearia que usa o BarberFlow e quer exercer esses
        direitos sobre os seus próprios dados (nome, telefone, histórico de agendamentos),
        entre em contato diretamente com a barbearia onde você agenda — ela é quem decide sobre
        esses dados. Se preferir, também pode falar com a gente pelo contato abaixo e
        intermediamos o pedido.
      </p>

      <h2>7. Contato</h2>
      <p>
        Dúvidas ou pedidos relacionados a esta política podem ser enviados para{' '}
        <a href="mailto:joaopedrodiasyt@gmail.com">joaopedrodiasyt@gmail.com</a>.
      </p>

      <h2>8. Mudanças nesta política</h2>
      <p>
        Podemos atualizar esta política conforme o BarberFlow evolui. Mudanças importantes serão
        comunicadas por e-mail ou por aviso dentro do painel.
      </p>
    </div>
  );
}
