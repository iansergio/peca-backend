# PECA Backend

API do PECA, marketplace onde técnicos de celular anunciam e buscam peças. Spring Boot 4.1 / Java 25, banco PostgreSQL (schema versionado com Flyway), imagens gravadas no disco local e login sem senha por código OTP + JWT.

Tudo roda numa máquina só, em containers, sem serviços externos.

## Subir tudo com Docker (front, API, banco e email)

Pré-requisitos: Docker com Compose e o repositório do front clonado ao lado deste:

```
Repos/
├── peca-backend/      <- este repositório (tem o docker-compose.yml)
└── peca-front-end/
```

```bash
docker compose up --build
```

| Serviço | URL | O que é |
|---|---|---|
| `web` | http://localhost:3000 | Front-end (Next.js) |
| `api` | http://localhost:8080/swagger-ui.html | API e Swagger |
| `mailpit` | http://localhost:8025 | Caixa de entrada local: os códigos OTP chegam aqui enquanto o SMTP real não estiver configurado |
| `postgres` | `localhost:5432` (banco/usuário/senha `peca`) | PostgreSQL |

O Compose já traz valores de desenvolvimento para tudo, então não é preciso `.env`. Para mudar algo (senha do banco, `JWT_SECRET`, pasta do front), copie `.env.example` para `.env`.

Os dados ficam em volumes nomeados (`postgres-dados` e `imagens`). Para apagar tudo e começar do zero: `docker compose down -v`.

### Email do código OTP

- **Email real (Gmail):** no `.env`, descomente o bloco `MAIL_*` do `.env.example` e preencha com o seu Gmail e uma [senha de app](https://myaccount.google.com/apppasswords) (não a senha da conta). O código chega na caixa de entrada de verdade.
- **Sem configurar nada:** os emails vão para o Mailpit. Entre com qualquer email no front e veja o código em http://localhost:8025.

## Rodar a API fora do Docker

Pré-requisitos: JDK 25. Suba só o banco e o email pelo Compose e rode a API com o Maven:

```bash
docker compose up -d postgres mailpit
cp .env.example .env
./mvnw spring-boot:run
```

O `.env` da raiz é lido automaticamente. Variáveis de ambiente reais têm prioridade sobre ele. Na primeira subida, o Flyway cria as tabelas (`src/main/resources/db/migration`).

Testes e build: `./mvnw clean package` (os testes usam H2 em memória e não precisam de banco).

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | não | JDBC do PostgreSQL. Padrão `jdbc:postgresql://localhost:5432/peca` |
| `DB_USERNAME` / `DB_PASSWORD` | sim | Credenciais do banco (no Compose, padrão `peca`/`peca`) |
| `JPA_DDL_AUTO` | não | Padrão `validate` (o schema vem do Flyway) |
| `JPA_SHOW_SQL` | não | `true` para logar o SQL. Padrão `false` |
| `JWT_SECRET` | sim | Segredo HS256 com 32+ caracteres (`openssl rand -base64 48`) |
| `JWT_EXPIRACAO` | não | Validade do token em ISO-8601. Padrão `PT2H` |
| `MAIL_HOST` / `MAIL_PORT` | não | SMTP. Gmail: `smtp.gmail.com` / `587`. Padrão: Mailpit (`mailpit:1025` no Compose, `localhost:1025` fora dele) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | só com SMTP real | Conta SMTP (no Gmail, use uma senha de app) |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | só com SMTP real | `true` para o Gmail. Padrão `false` (Mailpit) |
| `MAIL_FROM` | não | Remetente exibido. Padrão `MAIL_USERNAME` |
| `STORAGE_DIR` | não | Pasta onde as imagens são gravadas. Padrão `./dados/imagens` (no Compose, o volume `imagens`) |
| `STORAGE_URL_PUBLICA` | não | Prefixo salvo em `imagemUrl`. Padrão `/imagens`, a rota do front que repassa para `GET /api/imagens/...` |
| `CORS_ALLOWED_ORIGINS` | não | Origens liberadas para chamar a API pelo navegador, separadas por vírgula. `*` não é aceito. O front chama a API só pelo servidor e não precisa de CORS |
| `PORT` | não | Porta HTTP. Padrão `8080` |
| `JAVA_OPTS` | não | Opções da JVM no container. Padrão `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` |
| `FRONTEND_DIR` | não | Só no Compose: caminho do repositório do front. Padrão `../peca-front-end` |

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/v3/api-docs`)
- Imagens dos anúncios: `GET /api/imagens/{pasta}/{arquivo}` (público)
- Health: `http://localhost:8080/actuator/health`, com as probes `/actuator/health/liveness` e `/actuator/health/readiness`. A readiness inclui o banco e fica `DOWN` (HTTP 503) se ele estiver inacessível. É ela que o `HEALTHCHECK` da imagem usa.
