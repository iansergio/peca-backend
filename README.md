# PECA Backend

API do PECA, marketplace onde técnicos de celular anunciam e buscam peças. Spring Boot 4.1 / Java 25, banco PostgreSQL do Supabase (schema versionado com Flyway), imagens no Supabase Storage e login sem senha por código OTP + JWT.

## Rodar local

Pré-requisitos: JDK 25 e um PostgreSQL acessível (o do Supabase ou o `postgres-local` do Docker Compose, abaixo).

```bash
cp .env.example .env   # preencha as variáveis
./mvnw spring-boot:run
```

O `.env` da raiz é lido automaticamente. Variáveis de ambiente reais têm prioridade sobre ele. Na primeira subida, o Flyway cria as tabelas (`src/main/resources/db/migration`).

Testes e build: `./mvnw clean package`. Os testes usam H2 em memória no modo PostgreSQL, rodam as mesmas migrations do Flyway e validam o schema contra as entidades (`ddl-auto=validate`). Não precisam de banco.

### Conexão com o Supabase

Use o **pooler em modo sessão** (Supabase > Connect > Session pooler, porta 5432):

```properties
DB_URL=jdbc:postgresql://aws-0-<regiao>.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.<ref-do-projeto>
DB_PASSWORD=<senha-do-banco>
```

A conexão direta (`db.<ref>.supabase.co`) só tem endereço IPv6 e não funciona no Azure App Service, que sai por IPv4. O modo transação do pooler (porta 6543) não é recomendado, porque não aceita os prepared statements que o driver JDBC usa por padrão.

## Rodar com Docker

```bash
docker compose up --build
```

Sobe a API em `http://localhost:8080` usando as variáveis do `.env`. O Dockerfile serve só para uso local; em produção a API roda como JAR no Azure App Service.

### Sem Supabase: PostgreSQL local

O serviço opcional `postgres-local` (perfil `banco-local`) roda um PostgreSQL 17 com os dados em um volume nomeado e cria o banco `peca` na primeira subida. No `.env`:

```properties
POSTGRES_PASSWORD=<senha-local>
DB_URL=jdbc:postgresql://postgres-local:5432/peca
DB_USERNAME=postgres
DB_PASSWORD=<a mesma senha>
```

```bash
docker compose --profile banco-local up --build
```

Para rodar a API fora do Docker contra esse banco (`./mvnw spring-boot:run`), troque `postgres-local` por `localhost` no `DB_URL`. A porta 5432 é publicada no host.

Dicas para o `.env`: não coloque aspas nos valores. Se uma senha tiver `$`, escreva `$$` (o Docker Compose interpola `$` no `env_file`).

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | JDBC do PostgreSQL. Supabase (pooler em modo sessão): `jdbc:postgresql://aws-0-<regiao>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` / `DB_PASSWORD` | sim | Credenciais do banco. No pooler do Supabase o usuário é `postgres.<ref-do-projeto>` |
| `JPA_DDL_AUTO` | não | Padrão `validate` (o schema vem do Flyway) |
| `JPA_SHOW_SQL` | não | `true` para logar o SQL. Padrão `false` |
| `JWT_SECRET` | sim | Segredo HS256 com 32+ caracteres (`openssl rand -base64 48`) |
| `JWT_EXPIRACAO` | não | Validade do token em ISO-8601. Padrão `PT2H` |
| `MAIL_HOST` / `MAIL_PORT` | não | SMTP. Padrão `smtp.gmail.com:587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | sim | Conta SMTP (no Gmail, use uma senha de app) |
| `MAIL_FROM` | não | Remetente exibido. Padrão `MAIL_USERNAME` |
| `SUPABASE_URL` / `SUPABASE_SERVICE_KEY` | sim | Supabase Storage (upload das imagens) |
| `SUPABASE_STORAGE_BUCKET` | não | Bucket público. Padrão `pecas` |
| `CORS_ALLOWED_ORIGINS` | não | Origens liberadas para chamar a API pelo navegador, separadas por vírgula (ex: `https://peca.app,http://localhost:3000`). `*` não é aceito. Vazio = nenhuma origem liberada. O `peca-front-end` (Next.js) chama a API só pelo servidor e não precisa de CORS |
| `PORT` | não | Porta HTTP. Padrão `8080` |
| `JAVA_OPTS` | não | Opções da JVM no container. Padrão `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` (definir substitui o padrão) |
| `POSTGRES_PASSWORD` | só `banco-local` | Senha do usuário `postgres` do PostgreSQL local do Compose |

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/v3/api-docs`)
- Health: `http://localhost:8080/actuator/health`, com as probes `/actuator/health/liveness` e `/actuator/health/readiness`. A readiness inclui o banco e fica `DOWN` (HTTP 503) se ele estiver inacessível. É ela que o `HEALTHCHECK` da imagem usa.
