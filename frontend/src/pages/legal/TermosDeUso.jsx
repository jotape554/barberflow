import { Link } from 'react-router-dom';

export default function TermosDeUso() {
  return (
    <div className="doc-legal">
      <div className="doc-topo">
        <Link to="/">← Voltar para o início</Link>
        <h1>Termos de Uso</h1>
        <div className="doc-atualizado">Última atualização: 16 de setembro de 2026</div>
      </div>

      <div className="doc-aviso">
        Este documento descreve as regras de uso do BarberFlow de forma simples e direta. Não
        substitui aconselhamento jurídico profissional.
      </div>

      <h2>1. O que é o BarberFlow</h2>
      <p>
        O BarberFlow é um sistema de gestão (SaaS) para barbearias, oferecendo agenda, cadastro
        de clientes e profissionais, controle financeiro, cálculo automático de comissões,
        página pública de agendamento para os clientes finais, e recursos adicionais conforme o
        plano contratado. Ao criar uma conta, você concorda com estes termos.
      </p>

      <h2>2. Cadastro e responsabilidade da conta</h2>
      <ul>
        <li>Você precisa fornecer informações verdadeiras ao criar sua conta;</li>
        <li>Você é responsável por manter sua senha em sigilo e por tudo que acontece na sua conta;</li>
        <li>Se você criar acesso para profissionais da sua equipe, você é responsável por avisá-los sobre o uso adequado do sistema.</li>
      </ul>

      <h2>3. Planos, teste grátis e cobrança</h2>
      <ul>
        <li>Novas contas começam com um período de teste gratuito de 14 dias, com acesso completo ao sistema;</li>
        <li>Depois do teste, é necessário escolher e pagar um plano para continuar usando o painel administrativo (a cobrança é processada pela Stripe);</li>
        <li>Cada plano (Básico, Profissional, Premium) libera um conjunto diferente de recursos e limites, descritos na tela de assinatura dentro do sistema;</li>
        <li>Você pode cancelar a assinatura a qualquer momento pelo próprio painel; o acesso permanece até o fim do período já pago;</li>
        <li>Não garantimos reembolso de períodos parcialmente utilizados, salvo quando exigido por lei.</li>
      </ul>

      <h2>4. Uso aceitável</h2>
      <p>Ao usar o BarberFlow, você concorda em não:</p>
      <ul>
        <li>Usar o sistema para fins ilegais ou para prejudicar terceiros;</li>
        <li>Tentar acessar dados de outra barbearia sem autorização;</li>
        <li>Sobrecarregar o sistema de propósito (por exemplo, enviando um volume anormal de requisições automatizadas);</li>
        <li>Usar a página pública de agendamento para criar agendamentos falsos ou spam.</li>
      </ul>
      <p>Podemos suspender contas que violem essas regras.</p>

      <h2>5. Seus dados</h2>
      <p>
        Os dados que você cadastra (clientes, agendamentos, financeiro) pertencem a você e à sua
        barbearia. Usamos esses dados apenas para fazer o sistema funcionar, conforme descrito na
        nossa <Link to="/privacidade">Política de Privacidade</Link>.
      </p>

      <h2>6. Disponibilidade do serviço</h2>
      <p>
        Fazemos o possível para manter o BarberFlow disponível e funcionando corretamente, mas
        não garantimos disponibilidade ininterrupta. Podem ocorrer manutenções programadas ou
        interrupções não planejadas. Recomendamos manter seus próprios registros importantes
        (ex.: exportações periódicas) sempre que possível.
      </p>

      <h2>7. Limitação de responsabilidade</h2>
      <p>
        O BarberFlow é fornecido "como está". Na máxima extensão permitida por lei, não nos
        responsabilizamos por perdas indiretas, lucros cessantes, ou danos decorrentes de uso
        indevido do sistema, indisponibilidade temporária, ou decisões de negócio tomadas com
        base nas informações do sistema.
      </p>

      <h2>8. Cancelamento e encerramento</h2>
      <p>
        Você pode cancelar sua conta quando quiser. Podemos encerrar contas que violem estes
        termos, mediante aviso prévio sempre que possível. Após o cancelamento, seus dados são
        tratados conforme descrito na Política de Privacidade.
      </p>

      <h2>9. Mudanças nestes termos</h2>
      <p>
        Podemos atualizar estes termos conforme o BarberFlow evolui. Mudanças importantes serão
        comunicadas por e-mail ou por aviso dentro do painel.
      </p>

      <h2>10. Contato e legislação aplicável</h2>
      <p>
        Estes termos são regidos pelas leis do Brasil. Dúvidas podem ser enviadas para{' '}
        <a href="mailto:joaopedrodiasyt@gmail.com">joaopedrodiasyt@gmail.com</a>.
      </p>
    </div>
  );
}
