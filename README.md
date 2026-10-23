# PECA Backend

API do PECA, marketplace onde técnicos de celular anunciam e buscam peças. Spring Boot 4.1 / Java 25, banco Azure SQL Database (schema versionado com Flyway), imagens no Supabase Storage e login sem senha por código OTP + JWT.

## Rodar local

Pré-requisitos: JDK 25 e um banco SQL Server acessível (Azure SQL ou o `sqlserver-local` do Docker Compose, abaixo).

```bash
cp .env.example .env   # preencha as variáveis
./mvnw spring-boot:run
```

O `.env` da raiz é lido automaticamente. Variáveis de ambiente reais têm prioridade sobre ele. Na primeira subida, o Flyway cria as tabelas (`src/main/resources/db/migration`).

Testes e build: `./mvnw clean package` (os testes usam H2 em memória e não precisam de banco).

## Rodar com Docker

```bash
docker compose up --build
```

Sobe a API em `http://localhost:8080` usando as variáveis do `.env`. Para usar o Azure SQL a partir da sua máquina, libere o seu IP no firewall do servidor no portal do Azure.

### Sem Azure: SQL Server local

O serviço opcional `sqlserver-local` (perfil `banco-local`) roda um SQL Server 2022 com os dados em um volume nomeado. Um serviço de init cria o banco `peca` na primeira vez. No `.env` (a senha precisa de 8+ caracteres com maiúsculas, minúsculas, números e símbolos):

```properties
MSSQL_SA_PASSWORD=<senha-forte>
DB_URL=jdbc:sqlserver://sqlserver-local:1433;database=peca;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=<a mesma senha>
```

```bash
docker compose --profile banco-local up --build
```

> `trustServerCertificate=true` aceita o certificado autoassinado do container. Use **apenas** nesse banco local, nunca no Azure.

Para rodar a API fora do Docker contra esse banco (`./mvnw spring-boot:run`), troque `sqlserver-local` por `localhost` no `DB_URL`. A porta 1433 é publicada no host.

Dicas para o `.env`: não coloque aspas nos valores. Se uma senha tiver `$`, escreva `$$` (o Docker Compose interpola `$` no `env_file`).

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | JDBC do SQL Server. Azure: `jdbc:sqlserver://<servidor>.database.windows.net:1433;database=<banco>;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=60` |
| `DB_USERNAME` / `DB_PASSWORD` | sim | Credenciais do banco |
| `JPA_DDL_AUTO` | não | Padrão `validate` (o schema vem do Flyway) |
| `JPA_SHOW_SQL` | não | `true` para logar o SQL. Padrão `false` |
| `JWT_SECRET` | sim | Segredo HS256 com 32+ caracteres (`openssl rand -base64 48`) |
| `JWT_EXPIRACAO` | não | Validade do token em ISO-8601. Padrão `PT2H` |
| `MAIL_HOST` / `MAIL_PORT` | não | SMTP. Padrão `smtp.gmail.com:587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | sim | Conta SMTP (no Gmail, use uma senha de app) |
| `MAIL_FROM` | não | Remetente exibido. Padrão `MAIL_USERNAME` |
| `SUPABASE_URL` / `SUPABASE_SERVICE_KEY` | sim | Supabase Storage (upload das imagens) |
| `SUPABASE_STORAGE_BUCKET` | não | Bucket público. Padrão `pecas` |
| `CORS_ALLOWED_ORIGINS` | sim, para o front | Origens do front-end separadas por vírgula (ex: `https://peca.app,http://localhost:5173`). `*` não é aceito. Vazio = nenhuma origem liberada |
| `PORT` | não | Porta HTTP. Padrão `8080` |
| `JAVA_OPTS` | não | Opções da JVM no container. Padrão `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` (definir substitui o padrão) |
| `MSSQL_SA_PASSWORD` | só `banco-local` | Senha do `sa` do SQL Server local do Compose |

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/v3/api-docs`)
- Health: `http://localhost:8080/actuator/health`, com as probes `/actuator/health/liveness` e `/actuator/health/readiness`. A readiness inclui o banco e fica `DOWN` (HTTP 503) se ele estiver inacessível. É ela que o `HEALTHCHECK` da imagem usa.
