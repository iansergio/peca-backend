# Cross-cutting Conventions — PEÇA

Detailed engineering conventions referenced (one line each) from `CLAUDE.md`. This file is loaded
**on demand** — read the section for the surface you are touching. The one-line imperative in
`CLAUDE.md` is authoritative; this file holds the mechanics and rationale.

Items tagged `(observed)` come from the code; `(adopted)` items are decisions made when SDD was set
up.

## Commands

Per app/repository. These are the commands the skills and agents run; keep them exact.

| App | Install | Test | Lint | Format | Dependency audit | Run locally |
|---|---|---|---|---|---|---|
| `peca-backend` (repository root) | Not needed; Maven resolves on build | `./mvnw clean package` (one class: `./mvnw test -Dtest=PecaServiceTest`) | Not configured | Not configured | Not configured | `docker compose up -d postgres mailpit` then `./mvnw spring-boot:run` |
| `peca-front-end` (`../peca-front-end`) | `npm ci` | Not configured; `npm run build` plus manual browser validation | `npm run lint` | Not configured | `npm audit` | `npm run dev` with `.env.local` |
| Everything | | | | | | `docker compose up --build` from `peca-backend` |

Definition of "lint clean and tests green" `(adopted)`: `./mvnw clean package` in the backend;
`npm run lint` and `npm run build` in the front, plus manual validation on a mobile-size screen.
Do not add linters, test runners or audit tools without a spec.

Runtime: JDK 25 and Node 24. On the maintainer's workstation the default `java` is 21, so run Maven
with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`. Local URLs: front `:3000`, Swagger
`:8080/swagger-ui.html`, Mailpit `:8025`.

## Testing

- Stack `(observed)`: JUnit 5, Mockito, AssertJ, `spring-boot-starter-*-test`, H2 in PostgreSQL mode.
  The front has no test stack.
- Unit tests for business rules and use cases mock collaborators with Mockito `(observed)`, and
  take a fixed `Clock` for anything time-based — fast, deterministic, no network, no real time.
- Integration/e2e tests cover the critical paths named in the spec; security rules are covered in
  `SegurancaIntegrationTest` `(observed)`.
- Every test cites the rule or acceptance criterion it covers (`R-3`, `AC-2`) `(adopted)`.
- Where tests live `(observed)`: `src/test/java`, same package as the class under test; `*Test` for
  unit and slices, `*IntegrationTest` for a full context. Test method names are in Portuguese.
- `src/test/resources/application.properties` replaces the main file: copy any new required
  property there. Tests run on Hibernate's `create-drop`, with Flyway off, so they do not exercise
  migrations. After writing a migration, boot it against PostgreSQL with `docker compose up --build`.

## Naming

- Project decision `(adopted)`, overriding the template's English default: code identifiers, URL
  paths, query params, database identifiers and comments are in **Portuguese**, matching the
  codebase (`PecaService`, `/api/pecas`, `codigos_verificacao`). Framework vocabulary stays as is
  (`Controller`, `Service`, `Repository`, `Request`, `Response`, `dto`). User-facing copy:
  Português (Brasil).
- Spec slugs and screen file names stay in English kebab-case, as the skills require; commit types
  stay in English.
- Casing `(observed)`: Java classes `PascalCase`, members `camelCase`, JSON fields `camelCase`;
  front files `kebab-case.tsx`, except App Router names; env vars `UPPER_SNAKE_CASE`; tables and
  columns `snake_case`, tables in the plural.
- Accents `(observed)`: Java code and comments avoid accented characters, except in text the user
  reads (`EmailService`). Front code and comments use normal Portuguese spelling.
- Java files indent with tabs `(observed)`.

## Errors and validation

- Validate every input at the trust boundary (type, length, format, range); never rely on a
  client or gateway as the only check.
- Errors returned to clients are generic (code + message); no stack traces, queries or internal
  paths. Internal errors are logged with context but without secrets or personal data.
- API errors `(observed)`: RFC 9457 `ProblemDetail` from `exception/GlobalExceptionHandler`. Each
  case has a dedicated exception mapped there: 400, 401, 403, 404, 409, 413, 429 with `Retry-After`,
  500, 502. Request validation uses Bean Validation on DTO records with `@Valid`.
- Front `(observed)`: `lib/api/*` turns statuses into user messages (`ErroApi`, `ErroLimite`) or
  result objects (`{ ok: false, erro }`). Read-side functions never throw, so pages render a
  friendly state.

## Configuration and environment variables

- Config is read from the environment, never hard-coded; every variable is documented in a
  committed `*.example` template. Real `.env*` files are git-ignored.
- Server-only secrets are never exposed to clients: no `NEXT_PUBLIC_` variable in this project;
  `API_URL` is read on the server only.
- Backend `(observed)`: `application.properties` maps env vars with `${VAR:default}`, loads an
  optional `.env` from the root, and binds typed `*Properties` records. Startup fails if
  `JWT_SECRET` has fewer than 32 bytes. README lists each variable.

## Secrets and sensitive files

- Secrets live in a local `.env` (backend), `.env.local` (front) or process environment passed by
  Compose, and reach the code only through configuration; never as literals.
- Never in the repository, in any commit: `.env*` (except committed `*.example` templates with
  placeholders), key material and credential stores, infrastructure state, database dumps and
  exports, HAR/log files, real personal data. The baseline list and the scan that enforces it are
  the **Leak scan** of the `/peca.commit` skill; project-specific patterns and allowed
  exceptions go in that skill's `<!-- sdd:project -->` block.
- `.gitignore` carries the baseline patterns — `.env`, `.env.*`, `!.env.example`, `*.pem`,
  `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.keystore`, `*.tfstate`, `*.dump`, `*.sqlite*`, `*.har`,
  `*.log` — plus the project's own. Ignoring is defense in depth; the leak scan is the gate.
- Fixtures, seeds, snapshots, test data and examples in specs/docs are synthetic: reserved domains
  (`example.com`, `.test`) and made-up names and numbers `(adopted)`.
- A secret that reached a commit is compromised even if removed later: rotate it first, then
  clean the history only if the team decides to. The repository owner is notified and rotates it
  where it is used: `JWT_SECRET` and the SMTP app password in `.env`.

## API documentation

`(observed)` Every controller has `@Tag`; every endpoint has `@Operation`, one `@ApiResponse` per
status and, when protected, `@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`. DTO records
carry `@Schema` with synthetic examples. Documentation text is in Portuguese. springdoc serves the
UI at `/swagger-ui.html` and the spec at `/v3/api-docs`; `OpenApiDocsTests` guards it.

## Database

- Flyway owns the schema; Hibernate runs with `ddl-auto=validate`, so a changed entity needs a
  migration `(observed)`. Add `V<n>__<descricao>.sql`; never edit an applied migration `(adopted)`.
- Physical identifiers `(observed)`: `snake_case`; constraints `pk_<tabela>`, `uk_<tabela>_<coluna>`,
  `fk_<tabela>_<referencia>`; indexes `idx_<tabela>_<coluna>`; UUID keys generated by JPA.
- Never raw SQL outside migrations; queries use Spring Data derived methods or Specifications
  `(observed)`.
- Transactions: `@Transactional` on service methods, `readOnly = true` for reads, and
  `open-in-view=false`, so a mapper needing lazy data runs inside the service `(observed)`.

## i18n — user-facing strings

No i18n layer: the product is pt-BR only and copy lives inline in components `(observed)`. The front
writes its own error messages; validation errors (400) on listing and profile forms may embed the
API's `detail`. Add a dictionary only through a spec.

## Data protection

LGPD applies. Classify every new personal-data field in its spec, with visibility and retention
`(adopted)`.

| Data | Where | Visibility `(observed)` |
|---|---|---|
| Email | `usuarios.email`, `codigos_verificacao.email`, JWT claim | Private: only its owner, through `UsuarioResponse`. Never in `PecaResponse` |
| Name | `usuarios.nome`, `codigos_verificacao.nome` | Public for sellers, in `VendedorResponse` |
| WhatsApp | `usuarios.whatsapp` | Public for sellers, in `VendedorResponse` |
| OTP code | `codigos_verificacao.codigo` | Secret; never logged or returned |
| Visitor IP | In memory, for the rate limit | Not persisted |

Never log emails, OTP codes, tokens, WhatsApp numbers or request bodies. Retention and plaintext
OTP storage are still open: see `docs/Architecture.md`.

## UI design references

- The design system lives in `../peca-front-end/docs/*.png` and its tokens in
  `../peca-front-end/app/globals.css`. Use no color outside that palette `(observed)`.
- New screens are designed first: `/peca.screen` generates the prompt and the exported HTML is
  saved as `docs/Screens/<actor>-<screen>.html` in `peca-backend` `(adopted)`. Follow the design
  faithfully; usage rules are in `docs/Screens/README.md`.
- Mobile first, shadcn/ui before custom components, and the CTA and feedback rules in the front's
  `AGENTS.md`.
- No native `confirm`/`alert`/`prompt`: use `components/ui/alert-dialog.tsx` for confirmations and
  `sonner` toasts for feedback `(observed)`.

## Commit convention

- Format: Conventional Commits, `type: assunto` with an optional scope `(observed)`. Examples:
  `feat: mantem o envio de email real via SMTP configuravel no .env`,
  `fix: whatsapp do vendedor pode vir nulo e sem DDI`.
- Language of messages: Português (Brasil).
- One commit per touched repository; subject ≤ 72 characters, imperative, says what and why.
- Never commit secrets, `.env*` files or real personal data — see
  [Secrets and sensitive files](#secrets-and-sensitive-files). Never push without being asked.
