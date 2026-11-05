---
name: peca.code
description: SDD step 3. Invoked by /peca.code or "implement / code the feature / run tasks.md". Implements the feature strictly from tasks.md, in the correct app/module/layer, with tests, clean code and green lint.
model: sonnet
metadata:
  depends_on: peca.tasks
---

# peca.code — Implement (SDD step 3)

Feature implementation based **only** on `tasks.md`. Specs and tasks must already exist.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`.

## Dependency and prerequisite (check before anything else)

Depends on `peca.tasks` (→ `peca.start`). Confirm that `/specs/<feature>/tasks.md`
exists.

If missing: **STOP immediately** and ask for `/peca.tasks` — the technical breakdown is part
of the contract. Also confirm that `spec.md`, `flows.md` and `rules.md` still exist (otherwise the
chain is broken — ask for `/peca.start`).

## Delegation to specialist agents

**Skip this section if you are a specialist dev agent** (`.claude/agents/*-dev.md`). You implement
your slice under [How to act](#how-to-act) and never delegate.

If the project has specialist agents in `.claude/agents/` **and** `tasks.md` has tasks in more than
one app/repo, act as **tech lead** and delegate the code to them instead of writing it yourself. A
single-app feature, or a project without specialist agents, skips this section and goes straight
to [How to act](#how-to-act).

1. **Split `tasks.md` by app/repo.** Each slice goes to the agent that owns that app. Shared
   contracts: the provider side (usually the API's DTO/schema) is the source; the consumer's
   mirror belongs to the consumer's slice.
2. **Check the contract.** If `tasks.md` states the contract between apps (`## Contract`), launch
   the agents **in parallel**, in one message. If it does not, run the provider first, then pass
   the contract from its report to the consumer.
3. **Brief each agent so the brief stands on its own.** Include the feature slug, its task IDs,
   the contract, and any decision the user made in this session that is not yet in the spec.
4. **Handle `BLOCKED:` replies.** Ask the user the agent's question, then resume that agent with
   the answer (`SendMessage`). Under `peca.express`, which asks nothing, a `BLOCKED:` reply
   is a **STOP** that routes to `/peca.go`.
5. **Verify the work. Do not trust the reports.** Read the real `git diff` in each app/repo,
   confirm every task in `tasks.md` is covered, and run the
   [logic leakage check](../../../ai/module_map.md#logic-leakage-check-before-finalizing-any-change).
   Confirm every report shows green tests and clean lint. Send each correction to the owning
   agent rather than editing that app yourself.
6. **Later fix rounds go through the owning agent too.** That includes `peca.go` "Fix" at
   review or security and `peca.express` remediation.

## How to act

Act as a **Software Engineer**.

1. Identify the target feature.
2. Read `/specs/<feature>/tasks.md` (and re-read `spec.md`, `rules.md`, `flows.md` when you need
   context).
3. Re-read `/ai/module_map.md` before touching any module, and the sections of
   `/ai/conventions.md` that cover the surfaces you touch.
4. Implement each task in order:
   - Put code in the correct app/module/layer.
   - For external integrations, define the **port** in the business code and the **adapter** in
     infrastructure; wire them where the project wires dependencies.
   - Keep the dependency direction the module map defines.
   - Write tests in the owning app/module, with or before the code.
   - Tick each task in `tasks.md` (`[x]`) as you finish it.
   - Ensure **all** acceptance criteria from the spec are covered.

## Clean code baseline (every line you write)

- Names say what things are and do; no abbreviations the codebase does not already use.
- Small functions with one responsibility; no deep nesting — return early.
- No duplication: reuse what exists before writing a helper; extract only on the second use.
- No dead code, commented-out code, or leftover debug output. No TODO without an owner and a spec
  reference.
- Explicit error handling at boundaries; never swallow errors silently.
- No magic values — named constants or configuration.
- No speculative abstractions, flags or options the spec does not need.
- Follow the surrounding style (formatting, patterns, folder layout); the linter and formatter
  decide, not taste.

## Tests

- Tests assert behavior **defined in the spec** and cite the rule (`R-3`), never inferred
  behavior.
- Business rules and use cases: unit tests with **fakes** for ports — fast, deterministic, no
  network.
- Critical paths: integration/e2e per [`/ai/conventions.md#testing`](../../../ai/conventions.md#testing).
- Cover the spec's edge cases, not only the happy path. Every acceptance criterion maps to at least
  one test.
- No flaky tests: no uncontrolled time, randomness, or network.

## Token economy

- Read only the spec sections and files the current task needs; never dump whole directories.
- Use targeted search instead of browsing; do not re-read files already in context.
- Prefer small, verifiable steps over large rewrites; summarize command output instead of pasting
  it.

## Non-negotiable rules

- **Do NOT** add behavior that is not in the spec.
- **Do NOT** guess missing requirements — ask (or return `BLOCKED:` if you are a specialist agent).
- Honor every **non-negotiable rule in `CLAUDE.md`** and every boundary in `/ai/module_map.md`.
- Keep entry points (controllers, handlers, CLI commands, UI) thin: validation, auth, mapping,
  delegation. Business logic lives in application/domain code.
- Simple CRUD with no business rule may skip ceremony where the module map allows it; do not create
  port/domain boilerplate where there is no rule.
- Writes the spec requires to be all-or-nothing are **atomic** (one transaction).
- Webhooks/integrations are **idempotent** when the spec requires it.
- Never hand-edit generated code. Schema changes go through the project's migration tool.
- Never hardcode user-facing strings when the project has an i18n rule (`CLAUDE.md`).
- Never put secrets, credentials or real personal data in code, tests, fixtures, seeds, snapshots
  or docs: configuration comes from the environment
  ([`/ai/conventions.md#configuration-and-environment-variables`](../../../ai/conventions.md#configuration-and-environment-variables)),
  test data is synthetic.
- Never weaken a security control to make something work: no disabled TLS verification, no
  auth/CSRF/CORS guard commented out or short-circuited, no debug mode, test account or bypass
  flag left in. If a task seems to need it, the spec is wrong — ask.
- Add a dependency only when a task needs it: official registry, exact intended name, pinned by
  the lockfile, no install hooks. Never from a git URL or tarball.

<!-- sdd:project -->
- Backend: follow Controller → Service → Repository → Entity with DTO records; see
  [`module_map`](../../../ai/module_map.md). Inject by constructor and take a `Clock` for time rules.
- Schema: new Flyway migration plus entity change; see
  [`Database`](../../../ai/conventions.md#database).
- Endpoints: Swagger annotations are part of the change; see
  [`API documentation`](../../../ai/conventions.md#api-documentation).
- Front: read `../peca-front-end/AGENTS.md` and the relevant guide in
  `../peca-front-end/node_modules/next/dist/docs/` before writing Next.js code. New API calls go in
  `lib/api/` through `requisicaoApi`.
- A contract change updates the DTO, `lib/types.ts` and `lib/api/*.ts` in the same feature.
- Identifiers and comments are in Portuguese; see [`Naming`](../../../ai/conventions.md#naming).
<!-- /sdd:project -->

## Definition of done

- Every task in `tasks.md` implemented and ticked; all acceptance criteria covered by tests.
- Tests green and lint clean in every touched app, using the commands in
  [`/ai/conventions.md#commands`](../../../ai/conventions.md#commands). A change that fails lint or
  tests is not done.
- No leftover placeholders, debug output, or unrelated changes in the diff — and nothing the
  `/peca.commit` leak scan would reject (secrets, real data, weakened controls).

## Expected output

- Code in the correct apps/modules/layers, with tests.
- Build, test and lint results (commands run, pass/fail counts).
- Delegation: what each agent delivered, per app/repo, plus any `BLOCKED:` question still open.

At the end, suggest the next steps: `/peca.review`, then `/peca.security`.
