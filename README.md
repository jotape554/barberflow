# BarberFlow

Sistema de gestão para barbearias (multi-tenant/SaaS): agenda, clientes,
serviços, profissionais e financeiro automático, com painel administrativo
e agendamento público para o cliente final.

## Estrutura

```
backend/    Spring Boot 3 (Java 21) + PostgreSQL + JWT
frontend/   React 18 + Vite (painel administrativo)
```

## Backend

```
cd backend
./gradlew bootRun
```

Configure o Postgres via variáveis de ambiente (todas têm um valor padrão
de desenvolvimento em `application.properties`):

```
DB_URL=jdbc:postgresql://localhost:5432/barberpro_saas
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=uma-chave-longa-e-secreta
```

Com `ddl-auto=update`, as tabelas são criadas automaticamente. Mais
detalhes do domínio (rotas, fluxo financeiro, agendamento público) estão em
[`backend/README.md`](backend/README.md).

## Frontend

```
cd frontend
npm install
npm run dev
```

O Vite já faz proxy de `/api`, `/auth` e `/public` para `http://localhost:8080`
(veja `frontend/vite.config.js`), então basta o backend estar rodando.

## Status

- Backend: autenticação/JWT, CRUDs multi-tenant, financeiro automático
  (receita + comissão ao concluir um agendamento), dashboard e
  disponibilidade de horários — completos.
- Frontend: login/registro, painel (dashboard, agenda, clientes, serviços,
  profissionais, financeiro) — completo.
- Pendente: página pública de agendamento (cliente final) e site
  institucional do BarberFlow.
