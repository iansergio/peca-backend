---
name: peca.tasks
description: SDD step 2. Invoked by /peca.tasks or "generate tasks / break down the feature / create tasks.md". Generates /specs/<feature>/tasks.md from the existing spec, without implementing code.
metadata:
  depends_on: peca.start
---

# peca.tasks — Generate TASKS (SDD step 2)

Technical breakdown of the SPEC into implementable steps. No code is written in this step.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Write in Português (Brasil).

## Dependency and prerequisite (check before anything else)

Depends on `peca.start`. Confirm these exist under `/specs/<feature>/`: `spec.md`,
`flows.md`, `rules.md`.

If **any** is missing: **STOP immediately** and instruct the user to run `/peca.start`
first. Do not generate tasks from a partial spec.

## How to act

Act as a **Software Engineer**.

1. Identify the target feature (ask if unclear).
2. Read **all** spec files for that feature: `spec.md`, `flows.md`, `rules.md`.
3. Read `/ai/module_map.md` (where each type of code belongs, rigor levels) and the
   [testing](../../../ai/conventions.md#testing) and [commands](../../../ai/conventions.md#commands)
   sections of `/ai/conventions.md`.
4. Generate `/specs/<feature>/tasks.md` following `/specs/_template/tasks.md`.

## Task requirements

- Step by step, no ambiguity. Each task has an id (`T1`, `T2`, …), a checkbox, and cites the
  rules/criteria it implements (`R-3`, `AC-2`).
- Mapped to the correct app/module/layer per `/ai/module_map.md`; state the rigor level where the
  map defines levels.
- External integrations (payment, email, storage, third-party APIs): plan the **port** (interface
  owned by the business code) and the **adapter** (implementation) as separate tasks.
- When tasks span more than one app or repository, add a **`## Contract`** section per interface
  between them: method + path (or message/event name), request/response fields, error codes. It
  lets `peca.code` run the specialist agents in parallel
  ([Delegation](../peca.code/SKILL.md#delegation-to-specialist-agents)).
- Include test tasks with the project's stacks and commands: unit tests with fakes for every
  business rule; integration/e2e for the critical path. A rule without a test task is a gap.
- Order tasks so each step leaves the project building and tests green (schema/contract →
  business code → adapters/UI → tests → docs).
- Token hygiene: the spec is the input — read source code only where needed to place a task.
<!-- sdd:project -->
- Group tasks by repository: `peca-backend`, then `peca-front-end`. The API contract table is required
  when both are touched, because the backend is the provider.
- A schema change is a backend task with its own migration, and the entity change goes with it.
- Front behavior gets manual validation steps instead of test tasks: route, what to click, mobile size.
- End each repository's tasks with its `ROADMAP.md` update.
<!-- /sdd:project -->
- **Do NOT implement code yet.**

## Expected output

File created/updated: `/specs/<feature>/tasks.md`.

At the end, report the file path and suggest the next step: `/peca.code`.
