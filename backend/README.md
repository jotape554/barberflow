# BarberFlow — Fases 1 a 6 (Backend)

Este pacote já entrega o backend com: multi-tenant real, autenticação JWT,
CRUDs protegidos, **financeiro automático** (Receita + Comissão geradas ao
concluir um agendamento, sem duplicar), **dashboard com dados reais** (nunca
inventados) e o **agendamento público** com cálculo de horários realmente
disponíveis — o cliente final nunca vê nem consegue escolher um horário
ocupado.

## O que tem de novo nesta rodada (Fases 3 a 6)

- `Receita`, `Despesa`, `Comissao`: entidades + repositórios + serviços,
  todos isolados por `barbearia_id`.
- Ao chamar `PATCH /api/agendamentos/{id}/concluir?formaPagamento=PIX`, o
  sistema gera automaticamente a Receita (valor = preço do serviço) e a
  Comissão do profissional (com base no `percentualComissao` cadastrado) —
  e é idempotente: chamar duas vezes não duplica.
- `GET /api/dashboard`: faturamento de hoje/mês, agendamentos de hoje,
  concluídos, clientes cadastrados e ticket médio do mês — tudo calculado
  na hora a partir do banco. Sem dado nenhum.
- `GET /public/barbearias/{slug}/horarios-disponiveis`: calcula os horários
  livres de um profissional para um serviço em uma data, considerando dias e
  horário de trabalho + agendamentos existentes.
- `POST /public/barbearias/{slug}/agendamentos`: fluxo completo do cliente
  final — localiza (por telefone) ou cria o Cliente automaticamente e cria
  o Agendamento, sem precisar de login.
- Permissões básicas com `@PreAuthorize`: financeiro (Receitas/Despesas) e
  exclusões de Cliente/Profissional/Serviço restritas a
  ADMINISTRADOR/GERENTE. (Ainda falta restringir o PROFISSIONAL a ver
  apenas a própria agenda — fica para o próximo passo.)

## Como isso se encaixa no que você já tinha

- Mantive o pacote `com.example.demo` para você conseguir colar isso por
  cima do projeto existente sem precisar mudar imports.
- As entidades foram recriadas em português (Barbearia, Cliente,
  Profissional, Servico, Agendamento) já com o campo `barbearia_id`
  obrigatório — é isso que garante que uma barbearia nunca acesse dados de
  outra.
- Os módulos de **Plano/Assinatura do cliente final** (corte ilimitado) e o
  cálculo de comissão/financeiro **não foram recriados aqui** — eles entram
  na Fase 3/4. Se você já tinha essas entidades prontas, me envie os
  arquivos que eu integro sem perder o que já funciona.
- `PlanoSaas`/`StatusAssinaturaSaas` na Barbearia é só a estrutura básica
  pedida na Fase 7 (planos do SaaS) — sem cobrança real ainda, como você
  pediu.

## Como rodar

1. Copie estas pastas por cima do seu projeto (mesma estrutura Gradle que
   você já usa com `gradlew.bat bootRun`).
2. Ajuste `src/main/resources/application.properties` com as credenciais
   reais do seu Postgres (pode ser o mesmo banco `barberpro_saas` — se
   preferir manter o nome antigo `barbearia_saas`, é só trocar a URL).
3. `./gradlew.bat bootRun` (Windows) ou `./gradlew bootRun` (Mac/Linux).
   Como `ddl-auto=update`, as tabelas novas são criadas automaticamente.

## Testando o fluxo (exemplo com curl/PowerShell)

```
POST /auth/registro
{
  "nomeBarbearia": "Barbearia do João",
  "nomeAdmin": "João",
  "email": "joao@teste.com",
  "senha": "123456"
}
```
Isso já cria a Barbearia + o usuário ADMINISTRADOR e devolve um `token`.

```
POST /auth/login  -> devolve token
```

Use o token em todas as chamadas para `/api/**`:
```
Authorization: Bearer <token>
```

Todas as rotas `/api/clientes`, `/api/profissionais`, `/api/servicos` e
`/api/agendamentos` já filtram automaticamente pela barbearia do token —
não existe parâmetro `barbeariaId` no corpo da requisição, evitando que
alguém tente acessar dados de outra conta manipulando o payload.

Rotas `/public/barbearias/{slug}` não exigem token — são a base para a
página pública de agendamento da Fase 6.

## Testando o fluxo financeiro/público (exemplo)

```
PATCH /api/agendamentos/5/concluir?formaPagamento=PIX
Authorization: Bearer <token>
```
→ agendamento vira CONCLUIDO, gera 1 Receita e 1 Comissão.

```
GET /public/barbearias/barbearia-do-joao/horarios-disponiveis
    ?profissionalId=1&servicoId=2&data=2026-09-20
```
→ lista de horários livres (sem precisar de token).

```
POST /public/barbearias/barbearia-do-joao/agendamentos
{
  "nomeCliente": "Maria",
  "telefoneCliente": "11999999999",
  "profissionalId": 1,
  "servicoId": 2,
  "data": "2026-09-20",
  "horaInicio": "14:00"
}
```

## Próximos passos

- Relatórios detalhados (por período, profissional, forma de pagamento).
- Restringir PROFISSIONAL à própria agenda/comissões (hoje ele já não
  acessa outra barbearia, mas dentro da própria vê tudo).
- Reintegrar Plano/Assinatura do cliente final (corte ilimitado), se você
  já tinha essa lógica pronta — me manda o código que eu integro.
- Frontend: painel administrativo (React) + página pública de agendamento
  + site institucional do BarberFlow. Ainda não foi feito — é o maior
  pedaço que falta para "vender" o produto visualmente.
