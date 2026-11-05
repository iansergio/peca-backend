---
name: peca.reverse
description: Spec from existing code. Invoked by /peca.reverse or "spec an existing feature / reverse-engineer this feature / document what the code does". Reads an already-implemented feature and writes spec.md, rules.md and flows.md under /specs/<feature>/ marked as reverse-engineered; writes no code.
metadata:
  role: spec-archaeologist
  related: peca.start, peca.fix
---

# peca.reverse — Spec from existing code

Brings an existing, unspecified feature under SDD: reads the code and tests and writes the spec
that describes what the system **does today**, so `/peca.fix` and `/peca.review` can
operate on it. Writes no code and changes no behavior.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Specs are written in
> Português (Brasil).

## Parameter: feature

Receives the feature name or area as free text (e.g. "password reset", "the orders module", a
route, a screen, a command). If empty, list the candidates from `/specs/_inventory.md` (when it
exists) and ask which one. Derive the **kebab-case slug** (English); confirm if ambiguous.

If `/specs/<feature>/spec.md` already exists: **STOP** — extend it with `/peca.start` or
change it with `/peca.fix`; this skill never overwrites a spec.

## How to act

Act as a **Spec Archaeologist**: describe observed behavior, never invent intent.

1. **Locate the feature.** Entry points (routes, handlers, screens, commands, jobs), the business
   code they call, persistence/schema, integrations, and the tests. Use targeted search; read only
   the files on the feature's path. For a large area, delegate exploration to a read-only subagent
   per layer and keep only the summaries.
2. **Extract behavior.** Inputs and validation, actors and permissions, state transitions, side
   effects, error cases, invariants enforced in code or schema, and what the tests assert — tests
   are the strongest evidence of intended behavior.
3. **Separate facts from doubts.** A rule is `observed` when the code enforces it; `inferred` when
   it only follows from naming, comments or docs; `open` when the code is ambiguous or looks
   defective. Never promote an inference to a rule silently.
4. **Confirm with the user** the `open` items and any inferred rule that matters
   (`AskUserQuestion`, at most two rounds). Items left unanswered stay in "Open questions".
5. **Write** `/specs/<feature>/spec.md`, `rules.md` and `flows.md` from `/specs/_template/`, with a
   header line `Status: reverse-engineered from code on <date>`. In `rules.md`, tag each rule
   `(observed)`, `(inferred)` or `(confirmed by the user on <date>)`. Add an **Open questions**
   section and an **Observed defects** section (behavior that looks wrong — report, do not fix).
6. **Update `/specs/_inventory.md`** (create it if missing): mark the feature as specified, with
   the date.

## Rules

- **Do NOT** write or change code, tests, or schema. Defects go to "Observed defects"; fixing
  them is `/peca.fix` after the user decides.
- **Do NOT** generate `tasks.md` — nothing is to be built.
- **Do NOT** describe desired behavior; describe current behavior. Desired changes are a new
  feature (`/peca.start`) or a fix (`/peca.fix`).
- **Do NOT** copy real data, credentials or internal hostnames from code, fixtures or history into
  the spec — replace examples with synthetic values. A secret found in the code or its history is
  compromised: report it at once under "Observed defects" and recommend rotating it.
- Token hygiene: no whole-directory dumps; summarize files instead of quoting them; stop when the
  behavior is covered.

## Expected output

- `/specs/<feature>/spec.md`, `rules.md`, `flows.md` (status: reverse-engineered).
- `/specs/_inventory.md` updated.
- A short report: what was covered, open questions, observed defects, and the next step
  (`/peca.fix` for defects, `/peca.start` to extend the feature).
