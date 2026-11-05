---
name: peca.start
description: SDD step 1. Invoked by /peca.start or "create spec / start feature / begin new feature" in the PEÇA project. Creates spec.md, flows.md and rules.md under /specs/<feature>/, without writing any code.
---

# peca.start — Create the SPEC (SDD step 1)

Mandatory first step of the PEÇA Spec-Driven Development workflow. No code is written
in this step.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Specs are written in
> Português (Brasil).

## Dependency

None. This is the entry point of the workflow — no prerequisites.

## Parameter: feature description

This skill receives the **feature description** as an argument (free text after
`/peca.start`).

- If a description is provided (e.g. `/peca.start let a user reset their password by
  e-mail`), use it as the main input to generate the SPEC.
- If **no** description is provided (skill invoked empty), **ask the user before doing anything
  else**:
  > "Which feature should I specify? Describe the objective, the actors/roles involved, and any
  > relevant rule or constraint."
  Wait for the answer. Do not infer the feature from history nor start writing files without the
  description in hand.

From the description, also derive a **kebab-case slug** (English) to name the `/specs/<feature>/`
folder. Confirm the slug with the user if it is ambiguous. If `/specs/<feature>/spec.md` already
exists, say so and ask whether to extend it (then read it) or pick another slug.

## How to act

Act as a **Spec Architect**.

1. Make sure you have the feature description (see "Parameter" above). If not, ask and wait.
2. Read `/specs/_template/spec.md`, `rules.md` and `flows.md` to follow the templates strictly.
   Read `docs/Product.md` only when the description needs product context (actors, scope).
3. Generate the three artifacts under `/specs/<feature>/`:
   - `spec.md` — objective, requirements, acceptance criteria, edge cases
   - `flows.md` — end-to-end behavioral flows
   - `rules.md` — authoritative business rules

## SPEC requirements

- Follow the templates strictly.
- Be explicit about business rules; number them (`R-1`, `R-2`, …) so code, tests and reviews can
  cite them. Number acceptance criteria too (`AC-1`, …).
- Define clear, verifiable acceptance criteria — each one must be testable.
- Include edge cases (invalid input, concurrency, retries, idempotency, duplicate webhooks,
  unavailable integrations, when applicable).
- For flows that write several related records (and any money flow), state the
  atomicity/consistency expectation.
- Classify every new personal-data field when `CLAUDE.md` defines a data-protection rule.
- Examples use synthetic data only (`example.com`, fictitious names) — never real customers,
  credentials or internal hostnames; the spec is committed too.
- Ask when a requirement is ambiguous — never invent a business rule to fill a gap.
- **Do not inspect unrelated specs or source code unless the user explicitly asks to extend an
  existing feature.** Then read only that feature's spec.
- **Do NOT write any code.**
- **Do NOT generate `tasks.md`** — that is step 2's responsibility (`/peca.tasks`).

## Expected output

Three files created/updated:
- `/specs/<feature>/spec.md`
- `/specs/<feature>/flows.md`
- `/specs/<feature>/rules.md`

At the end, report the file paths and suggest the next step: `/peca.tasks`.
