---
name: peca.go
description: Full SDD workflow. Invoked by /peca.go or "run the full workflow / do the feature end-to-end / run SDD". Orchestrates peca.start → peca.tasks → peca.code → peca.review → peca.security, with an approval check-point between each step.
---

# peca.go — Full SDD workflow with check-points

Runs the five Spec-Driven Development steps in sequence, **stopping after each step** for the user
to review and approve before continuing.

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Talk to the user in
> Português (Brasil).

## Parameter: feature description

Receives the **feature description** as an argument and passes it in full to `peca.start`
in step 1. If empty, **ask before starting** (same rule as `peca.start`) and wait for the
answer.

Derive a **kebab-case slug** for `<feature>` and use the **same** identifier across all 5 steps
(`/specs/<feature>/`). Confirm with the user if ambiguous.

## Execution order

1. **Step 1 — SPEC**: invoke `peca.start`
2. **Step 2 — TASKS**: invoke `peca.tasks`
3. **Step 3 — CODE**: invoke `peca.code`
4. **Step 4 — REVIEW**: invoke `peca.review`
5. **Step 5 — SECURITY**: invoke `peca.security`

## Check-point protocol (mandatory between every step)

After **each** step completes, **STOP** and do exactly this:

1. Summarize in up to 5 lines what was produced (files created/changed, main decisions).
2. List the artifact paths for the user to review.
3. Use `AskUserQuestion` with 3 options:
   - **Continue** — advance to the next step
   - **Fix** — user points out what to adjust; redo the current step with the correction
   - **Abort** — end the workflow

**Do not start the next step without an explicit answer.**

If the user chooses "Fix":
- Apply the corrections in the current step. At the SECURITY step this means applying the
  report's remediation per
  [`peca.code` non-negotiable rules](../peca.code/SKILL.md#non-negotiable-rules)
  (with tests) and re-running `peca.security`. In a multi-app feature, corrections go
  through the owning specialist agent
  ([Delegation](../peca.code/SKILL.md#delegation-to-specialist-agents)).
- After fixing, redo the check-point (summary + `AskUserQuestion`).
- Only advance when the user chooses "Continue".

If the user chooses "Abort":
- End immediately, reporting which step you stopped at and the state of the artifacts.

At the SECURITY check-point, if any **Critical** or **High** finding is open, say so explicitly
and recommend **Fix** — the feature is not done while one remains. When the user chooses
"Continue" with none open, suggest `/peca.commit`.

## Rules

- **Never** skip a step, even if it seems trivial — the review and the security analysis are
  especially essential.
- **Never** combine two steps in a single round — each has its own check-point.
- Keep context between steps (same feature, same folder). Do not re-read artifacts a previous step
  just produced unless the user changed them.
- Respect all rules of each individual skill (`peca.start`, `peca.tasks`,
  `peca.code`, `peca.review`, `peca.security`).
