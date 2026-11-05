---
name: peca.express
description: Token-lean SDD for simple features. Invoked by /peca.express or "quick feature / small end-to-end feature / express workflow". Runs peca.start → peca.tasks → peca.code → peca.security with no questions and no check-points; skips review. Complex features route out to peca.start / peca.go.
metadata:
  role: fast-orchestrator
  related: peca.go, peca.start, peca.tasks, peca.code, peca.security, peca.fix
---

# peca.express — Token-lean SDD for simple features

Same chain as [`peca.go`](../peca.go/SKILL.md) (`start → tasks → code → security`)
but optimized for **low token cost**: it runs the steps **back to back with no check-points and
asks nothing**. Use it only for **simple** features. It **skips the review step** on purpose — the
trade-off for speed — but **never the security analysis**. For anything non-trivial, use
`/peca.start` or `/peca.go` instead.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Report in Português (Brasil).

## Complexity gate (decide before writing anything)

This skill is only valid when the feature is **simple**. Judge it up front from the description;
if **any** of these hold, it is **not** simple:

- New or changed **critical business rule** (money, permissions, state machines, all-or-nothing
  writes).
- New **external integration** needing a port + adapter (payment, email, storage, third-party
  API), or a webhook that must be idempotent.
- Touches **many modules** or crosses several layers with non-obvious flows.
- **Ambiguous or under-specified** requirements — you'd have to infer or make product decisions.
- Any acceptance criterion you cannot state precisely from the description alone.
<!-- sdd:project -->
- Passwordless OTP and JWT issuance: code generation, validation, attempts, expiry, rate limits.
- Authorization: `SecurityConfig` public routes, ownership checks in `PecaService` and
  `UsuarioService`.
- Session handling in the front: `lib/sessao.ts`, `lib/jwt.ts`, `proxy.ts`, cookie options.
- Photo upload and serving: content detection, size limits, path validation.
- Public exposure of personal data: `VendedorResponse`, `UsuarioResponse`.
- Flyway migrations and any contract change between the API and the front.
<!-- /sdd:project -->

If the feature is simple (a bounded CRUD, a self-contained screen/endpoint, a clear small flow with
no critical rule/integration), **proceed**. Otherwise **STOP immediately** and tell the user:

> This feature is not simple enough for `/peca.express`. Use `/peca.go` for the full
> workflow (with check-points and review), or `/peca.start` to begin with just the spec.

Do not partially run and then bail — decide at the gate, before any file is written.

## Parameter: feature description

Receives the **feature description** as an argument and passes it in full to `peca.start`.

Unlike `peca.go`, this skill **asks nothing**. If the description is **empty**, do not
prompt for it — instead STOP and route to `/peca.start` (an empty invocation is not a
simple, ready-to-run feature). Derive a **kebab-case slug** for `<feature>` yourself and reuse the
**same** identifier across all steps (`/specs/<feature>/`); do not ask for confirmation.

## Execution order (no check-points, no questions)

Run these four in sequence, without stopping between them:

1. **SPEC** — invoke `peca.start`
2. **TASKS** — invoke `peca.tasks`
3. **CODE** — invoke `peca.code`
4. **SECURITY** — invoke `peca.security`. If it reports any **Critical** or **High**
   finding, apply the remediation (same non-negotiable rules, with tests; in a multi-app feature,
   through the owning agent per
   [Delegation](../peca.code/SKILL.md#delegation-to-specialist-agents)) and re-run it
   **once**. If a Critical/High finding is still open — or the fix needs a business decision —
   **STOP** and report it, recommending `/peca.go`.

**Do not** invoke `peca.review` — that is the deliberate omission of this lane.

## Rules

- **Override the check-point protocol**: `peca.go` stops after each step for approval —
  this skill does **not**. Chain the four steps automatically and only report at the end.
- **Ask nothing.** Each underlying skill has an "ask if unclear/empty" branch — in this lane, a
  case that would trigger a question means the feature isn't simple: **STOP and route to
  `/peca.go`** instead of asking. Never infer a missing business requirement to keep going.
- Respect every **non-negotiable rule** of each underlying skill (they are not waived): code lands
  in the correct app/module/layer, business code stays framework-free where required, thin entry
  points, no hardcoded user-facing strings, no hand-edited generated code, tests in the owning
  module, no secrets or real data in the diff, no weakened security control. See
  [`peca.code` non-negotiable rules](../peca.code/SKILL.md#non-negotiable-rules).
- Keep the same feature/folder across all four steps.
- If, mid-run, the work reveals hidden complexity (a critical rule, an integration, real
  ambiguity), **STOP** and recommend `/peca.go` rather than guessing — the gate can fail
  late.

## Expected output

A single final report (no intermediate check-points):

- Spec files created: `/specs/<feature>/{spec.md,flows.md,rules.md,tasks.md}`.
- Code + tests, by app/module/layer.
- Build/test/lint result when applicable.
- Security result: overall risk level, findings fixed, and any Medium/Low follow-ups left open.
- A one-line reminder that **review was skipped** — suggest `/peca.review` if the user
  wants the compliance pass, and `/peca.commit` to commit.
