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

## Verificações

```bash
cd amcash-web
npm run lint
npm run build

cd ../amcash-api
./mvnw test
```
