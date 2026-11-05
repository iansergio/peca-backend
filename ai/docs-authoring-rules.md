# Docs & Skills Authoring Rules — token hygiene

Authoritative rules for writing/editing `CLAUDE.md`, `/ai/`, and `.claude/skills/**`.
Goal: keep token cost low **without losing rules or context**. Read before editing any doc or
skill.

These rules exist because duplicated rules across `CLAUDE.md` and `ai/*.md`, long explanations in
the always-loaded `CLAUDE.md`, and per-skill "Fixed context"/"When to use" boilerplate waste
tokens on every turn. Don't introduce that.

## 1. One fact, one home

- Each rule/convention/table is written in **exactly one** file. Everything else **links** to it.
- Never copy a rule into a second file — link to the owner with a relative path (+ anchor).
- Ownership map:
  - Workflow + non-negotiable rules (one line each) → `CLAUDE.md`
  - Module boundaries table + "Logic leakage check" → `ai/module_map.md`
  - Cross-cutting mechanics (commands, testing, naming, i18n, DB, env, commits) →
    `ai/conventions.md`
  - Product context (actors, scope, roadmap) → `docs/Product.md`
  - Business rules → `/specs/<feature>/`
- If you find the same content in two places, **delete the copy** and leave a link.

## 2. Mind what loads every turn

Loaded on **every session/turn** → keep ruthlessly short:
- `CLAUDE.md` body
- each skill's frontmatter `description`

Loaded **on demand** (cheap) → detail belongs here:
- skill bodies (`SKILL.md`), `ai/*.md`, `/specs/**`, `/docs/**`

Rule: in always-loaded files, write the **imperative in one line + a link**. The "why", examples,
and mechanics go in an on-demand file. Never inline a multi-sentence explanation in `CLAUDE.md`.

## 3. Trimming must never drop a rule

- Moving detail out of `CLAUDE.md` is fine **only if** it lands in a linked on-demand file. Move,
  don't delete.
- Never weaken or remove: STOP/prerequisite gates, the `review` checklist, the `security`
  checklist and severity gate, the `code` "Non-negotiable rules" and "Clean code baseline", the
  `commit` gate and "Leak scan", and the `go` check-point protocol. These guard quality, not
  tokens.

## 4. Skill authoring

- Skills, `CLAUDE.md` and `/ai/**` are written in **English**. Specs and docs follow the project
  language policy in `CLAUDE.md`.
- Frontmatter: only supported keys (`name`, `description`, `metadata`, `license`,
  `compatibility`, `model`). Do **not** use `version` or a top-level `depends_on` — they are
  unsupported and raise diagnostics; put dependency hints under `metadata:` and document the
  chain in the body.
- `description`: one sentence = step number + invocation triggers (`/cmd` + 1–2 natural-language
  synonyms) + what it outputs. Triggers drive routing — keep them; cut only prose.
- Do **not** add a "When to use" section that restates the `description`.
- Do **not** add a "Fixed context" section restating SDD/specs/module_map — link instead:
  `> Project context and rules: see CLAUDE.md and ai/module_map.md.`
- Merge "Dependency" + "Prerequisite" into one short block; keep the STOP.
- Don't restate a parameter/feature-identification block that another skill already owns — link
  to it.
- Project-specific content inside a managed skill goes **inside its `<!-- sdd:project -->`
  block**; generic improvements go to the template repository (`.sdd/sdd diff` shows the delta).

## 5. Before committing a doc/skill edit

- [ ] No rule appears in two files (search for duplicated phrasing).
- [ ] Every detail removed from an always-loaded file landed in a linked on-demand file.
- [ ] No STOP gate, checklist (review/security), or non-negotiable rule was lost.
- [ ] Skill `description`s still contain their routing triggers.
- [ ] Links/anchors resolve.
