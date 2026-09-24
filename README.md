# AMCash

Aplicação de finanças pessoais para acompanhar receitas, despesas, lançamentos parcelados e fluxo de caixa mensal.

## Estrutura

- `amcash-web`: interface mobile-first em Next.js e TypeScript.
- `amcash-api`: API em Spring Boot com autenticação pelo Google e JWT.

## Executando o frontend

```bash
cd amcash-web
npm install
npm run dev
```

O frontend fica disponível em `http://localhost:3000`.

## Ambiente local

O PostgreSQL pode ser iniciado pelo Docker:

```bash
docker compose up -d postgres
```

No Windows, o script abaixo carrega `amcash-api/.env.local`, inicia o banco e executa a API:

```powershell
cd amcash-api
.\run-local.ps1
```

## Executando a API manualmente

Pré-requisitos: Java 25 e PostgreSQL.

Configure as variáveis de ambiente conforme necessário:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `JWT_SECRET`

Depois execute:

```bash
cd amcash-api
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`.

## API financeira

Depois do login com Google, envie o JWT retornado em todas as chamadas:

```text
Authorization: Bearer <token>
```

Endpoints disponíveis:

- `GET /api/v1/transactions?year=2026&month=10`: lançamentos e resumo do mês.
- `POST /api/v1/transactions`: cria receita ou despesa, incluindo repetições.
- `GET /api/v1/transactions/{id}`: detalhes do lançamento.
- `PUT /api/v1/transactions/{id}`: edita o lançamento.
- `DELETE /api/v1/transactions/{id}`: exclui o lançamento.
- `POST /api/v1/transactions/{id}/subexpenses`: adiciona uma subdespesa.
- `PUT /api/v1/transactions/{id}/subexpenses/{subexpenseId}`: edita uma subdespesa.
- `DELETE /api/v1/transactions/{id}/subexpenses/{subexpenseId}`: exclui uma subdespesa.

As frequências aceitas são `NONE`, `DAILY`, `WEEKLY` e `MONTHLY`. O campo
`recurrenceCount` representa quantas ocorrências adicionais devem ser criadas.
Todas as consultas são isoladas pelo usuário identificado no JWT.

## Verificações

```bash
cd amcash-web
npm run lint
npm run build

cd ../amcash-api
./mvnw test
```
