# CLAUDE.md

## Project

**PEÇA** — marketplace where cell-phone repair technicians list and find replacement parts; a sale
closes outside the platform, over WhatsApp. Actors: visitor and authenticated technician. Product
context: [`docs/Product.md`](docs/Product.md).

- `peca-backend` (this repository, the SDD hub): REST API, Java 25 + Spring Boot 4.1. Owns the
  specs, the docs and the `docker-compose.yml` that runs everything.
- `peca-front-end` (sibling at `../peca-front-end`): UI, Next.js 16. Talks to the API **only from
  its server**; the browser never calls the API directly.

Each repository's `AGENTS.md` stays authoritative for its stack, passwordless auth and design
rules: [`AGENTS.md`](AGENTS.md), [`../peca-front-end/AGENTS.md`](../peca-front-end/AGENTS.md).
Read the one for every app you touch.

## Language

- Conversation, reports, commit messages, `/specs/**` and `/docs/**`: **Português (Brasil)**.
- `CLAUDE.md`, `/ai/**`, `.claude/**`: **English**.
- Code identifiers, URL paths, database identifiers and code comments: **Portuguese**, as the code
  already is — see [`/ai/conventions.md#naming`](ai/conventions.md#naming).

## Source of Truth

**`/specs` is the single source of truth for system behavior.** Do not implement, modify, or
review behavior without first reading the relevant spec.

> Spec structure: `/specs/<feature>/spec.md` (objective, requirements, acceptance criteria, edge
> cases) · `rules.md` (authoritative business rules, numbered `R-n`) · `flows.md` (end-to-end
> flows) · `tasks.md` (implementation checklist). New specs follow `/specs/_template/`. Existing
> features without a spec are listed in `/specs/_inventory.md` and get one through
> `/peca.reverse` before they are changed.

Architecture reference: [`/ai/module_map.md`](ai/module_map.md) — where each type of code belongs
(consult before touching any module). Cross-cutting mechanics and commands:
[`/ai/conventions.md`](ai/conventions.md). Observed architecture and open points:
[`docs/Architecture.md`](docs/Architecture.md). `ROADMAP.md` in each repository tracks delivery
status, not behavior.

## Mandatory Workflow

Before any code change:

1. Read `spec.md`, `rules.md`, and `flows.md` for the relevant feature under `/specs`.
2. Validate acceptance criteria — if anything is ambiguous, ask; never infer a requirement.
3. Update `tasks.md` if missing or outdated.
4. Read `/ai/module_map.md` and map each task to the correct app/module.
5. Implement only in the owning module/app.
6. Write tests in the owning module/app.
7. Verify every acceptance criterion is covered.
8. Check that spec and implementation do not diverge.
9. Run the security analysis (`/peca.security`) — no open Critical/High finding before
   finishing.
10. Commit through `/peca.commit` — nothing reaches the repository without its leak scan.

Around these steps, the `AGENTS.md` workflow applies in every touched repository: work on an
isolated branch, update `ROADMAP.md` under its honesty rule (`[x]` only after human testing), and
open the PR with `gh` using that repository's template.

Lanes: `/peca.go` (full workflow with check-points) · `/peca.express` (simple
features, no questions) · `/peca.fix` (small change to a specified feature) ·
`/peca.reverse` (spec an existing feature). Skills live in `.claude/skills/peca.*`.

## Non-Negotiable Rules

- Never implement a feature without a spec.
- Never write business logic outside `/specs`.
- Never choose a module without consulting `/ai/module_map.md`.
- Never hand-edit generated code.
- Keep business/domain code free of framework and persistence imports where
  `/ai/module_map.md` requires it.
- Writes that must succeed or fail together (as the spec states) run in one transaction.
- Lint clean and tests green before a change is done — commands in
  [`/ai/conventions.md#commands`](ai/conventions.md#commands).
- Never commit secrets, credentials, `.env*` files, dumps or real personal data; commit through
  `/peca.commit`, whose leak scan must pass
  ([`/ai/conventions.md#secrets-and-sensitive-files`](ai/conventions.md#secrets-and-sensitive-files)).
- Never weaken a security control (TLS verification, auth/CSRF/CORS guards, debug modes) to make
  something work; if a task needs it, the spec is wrong — ask.
- If spec and code diverge, fix the spec first.
- When editing docs or skills, follow [`/ai/docs-authoring-rules.md`](ai/docs-authoring-rules.md)
  — one fact one home, no duplication, keep always-loaded files lean without dropping rules.
- Every API endpoint carries Swagger/OpenAPI annotations
  ([`/ai/conventions.md#api-documentation`](ai/conventions.md#api-documentation)).
- The schema changes only through a new Flyway migration; physical identifiers are `snake_case`
  ([`/ai/conventions.md#database`](ai/conventions.md#database)).
- Every new personal-data field is classified in its spec under LGPD and never logged
  ([`/ai/conventions.md#data-protection`](ai/conventions.md#data-protection)).
- UI follows the design system before implementation; no native `confirm`/`alert`/`prompt`
  ([`/ai/conventions.md#ui-design-references`](ai/conventions.md#ui-design-references)).

## Stack & Style

- Runtime: everything runs on one machine with `docker compose up --build`; no external services
  except an optional real SMTP.
- `peca-backend`: Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Security resource server, Validation,
  Mail, Actuator), springdoc, Maven Wrapper.
- `peca-front-end`: Node 24, Next.js 16 (App Router, Cache Components), React 19, TypeScript,
  Tailwind v4, shadcn/ui, npm.
- Data: PostgreSQL 18 with Flyway; photos on a local volume; H2 only in backend tests.
- Tests: JUnit 5, Mockito, AssertJ and Spring Boot test slices in the backend; none in the front.
- Thin entry points (controllers, handlers, UI): validation, auth, mapping, delegation. Business
  logic in application/domain code; persistence in adapters/repositories.
- Tests focus on critical paths: business rules and use cases have unit tests with fakes for
  ports (fast, no network); integration/e2e cover the main flows.

## Module Boundaries

Quick shape: `browser → peca-front-end (Next server) → peca-backend (controller → service →
repository) → PostgreSQL / disk / SMTP`.
**Full boundary table and the "Logic leakage check" (run it before finalizing any change) live in
[`/ai/module_map.md`](ai/module_map.md)** — consult before touching any module.
