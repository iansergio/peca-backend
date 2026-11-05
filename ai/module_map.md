# Module Map — PEÇA

Where each type of code belongs. **Consult this file before touching any module.**

Two repositories, side by side. `peca-front-end` is the only client of `peca-backend`: Server
Components, Server Actions and route handlers call the REST API from the Next server, with the
JWT read from an httpOnly cookie. The browser never reaches the API, so photos go through the
front's `/imagens` route too. Specs, docs and `docker-compose.yml` live in `peca-backend`. The only
async work is the OTP email, on the bounded in-process executor in `config/AsyncConfig`. There are
no external queues, payments or admin area by decision until a spec adds them.

## Repositories / apps

| App / layer | Repository / path | Stack | Owns |
|---|---|---|---|
| API | `peca-backend` (`src/main/java/com/peca/backend`) | Java 25, Spring Boot 4.1 | Business rules, authentication, authorization, persistence, photo storage, OTP email |
| UI | `../peca-front-end` | Next.js 16, React 19, Tailwind v4, shadcn/ui | Screens, session cookie, UX validation, client-side filtering of the listing |
| Database | `peca-backend` (`src/main/resources/db/migration`) | PostgreSQL 18, Flyway | Schema |
| Contract | `peca-backend` DTOs in `*/dto`, mirrored in `../peca-front-end/lib/types.ts` and `lib/api/*.ts` | OpenAPI (`/v3/api-docs`) | The API is the provider; the front keeps a hand-written mirror |
| Local environment | `peca-backend` (`docker-compose.yml`) | Docker Compose | PostgreSQL, Mailpit, API and front together |

## Architecture style and rigor levels

Layered architecture, packaged by feature: Spring's Controller → Service → Repository → Entity,
with DTOs at the edge, as `AGENTS.md` requires. Rigor stays proportional to the rules at stake.

**Level 1 — Volatile external integrations → one service class per integration.** SMTP
(`email/EmailService`) and disk (`storage/ArmazenamentoImagemService`). They are concrete classes,
without a port interface, mocked in tests. Extract an interface only when a spec adds a second
provider.

**Level 2 — Relevant business rules → in the service, covered by unit tests.** Passwordless auth,
limits, ownership and contact rules live in `AuthService`, `PecaService` and `UsuarioService`,
tested with Mockito and an injected `Clock`. The service may use Spring (`@Service`,
`@Transactional`) and JPA entities; the code has no separate framework-free domain layer.

**Level 3 — Simple CRUD → direct layers, no ceremony.** Controller → service → repository.

### Layout of a business module

```text
peca-backend — src/main/java/com/peca/backend/<feature>/
  <Feature>Controller.java      HTTP mapping, @Valid, Swagger annotations, delegation
  <Feature>Service.java         rules, ownership checks, @Transactional boundary
  <Feature>Repository.java      Spring Data JPA (Specifications when filtering)
  <Feature>.java                JPA entity (plain getters/setters, no rules)
  dto/                          request/response records with Bean Validation and @Schema
peca-front-end
  app/<rota>/page.tsx           page (Server Component by default)
  app/<rota>/actions.ts         Server Actions: session check, validation, call to lib/api
  components/<dominio>/         feature components
  components/ui/                shadcn/ui components
  lib/api/<recurso>.ts          one function per endpoint, on top of lib/api/cliente.ts
  lib/sessao.ts, lib/jwt.ts     session cookies and display-only JWT reading
  proxy.ts                      optimistic route guard
```

- **Dependency direction**: controller → service → repository → entity. Features may call another
  feature's service or repository (`peca → usuario, storage`; `auth → usuario, email`). Front:
  `app → components → lib`.
- **Wiring**: Spring constructor injection; configuration through `@ConfigurationProperties`
  records. `@Autowired` appears only to pick the public constructor when a package-private one
  takes a test `Clock`.
- **Shared technical modules**: `exception` (domain exceptions and `GlobalExceptionHandler`),
  `config` (CORS, OpenAPI), `auth/UsuarioAutenticado` (JWT `sub` to user id). Any feature may use
  them.

## Where each thing belongs

| Code type | Location |
|---|---|
| Business rule / invariant | `peca-backend`, `<feature>/<Feature>Service.java`, stated in `/specs/<feature>/rules.md` |
| Use case / orchestration | `peca-backend`, service method with `@Transactional` |
| External integration interface (port) | None today; see Level 1 |
| Integration implementation / repository (adapter) | `email/`, `storage/`, `<feature>/<Feature>Repository.java` |
| Entry point (controller / route / command / handler) | `peca-backend` `*Controller.java`; `peca-front-end` `app/**/route.ts`, `app/**/actions.ts` |
| Contract shared between apps | `peca-backend` `*/dto/*` records; mirror in `../peca-front-end/lib/types.ts` and `lib/api/*.ts` |
| UI component / page / hook | `../peca-front-end/app/**`, `components/<dominio>/`; shadcn/ui in `components/ui/` |
| UI copy (i18n) | Inline pt-BR in the front; there is no i18n layer |
| Database schema / migration | `src/main/resources/db/migration/V<n>__<descricao>.sql` |
| Configuration / env loading | `src/main/resources/application.properties` plus `*Properties` records; front reads `process.env.API_URL` in `lib/api/cliente.ts` |
| Tests | `src/test/java/...`, same package as the class: `*Test` for unit and slices, `*IntegrationTest` for full context. The front has no tests; validate by hand in the browser |

## Logic leakage check (before finalizing any change)

- Entry points contain no business logic — they validate, authorize, map and delegate.
- Integration adapters decide no business rules — they only translate protocol/provider.
- Shared types/contracts do not compute rules, prices, or permissions.
- Clients/UI do not duplicate service/domain logic.
- Business/domain code does not import framework or persistence libraries where the rigor level
  requires it.
- Tests assert behavior **defined in the spec**, not inferred behavior.
- Writes the spec requires to be all-or-nothing run in **one transaction**.
- The browser never calls the API: no `API_URL` or token in a Client Component, no
  `NEXT_PUBLIC_` variable pointing to the backend.
- Controllers never receive or return JPA entities; only DTOs.
- Authorization is enforced in the backend; `proxy.ts` and the front's session checks are UX only.
- Front validation that mirrors `PecaRequest` (`lib/validar-anuncio.ts`, `lib/regras-anuncio.ts`)
  stays in sync with it and never replaces it.
- Filtering in the front (`lib/filtro-pecas.ts`) is a known workaround; new filters belong in the API.
