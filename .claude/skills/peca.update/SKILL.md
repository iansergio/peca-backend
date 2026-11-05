---
name: peca.update
description: Template sync. Invoked by /peca.update or "update the SDD skills / sync with the template / pull template changes". Runs `.sdd/sdd update`, resolves merge conflicts preserving project customizations, re-verifies the skills, and reports; writes no application code.
metadata:
  role: template-sync
---

# peca.update — Sync with the SDD template

The project's skills (`.claude/skills/peca.*`), the `techlead` agent and the `.sdd/sdd`
script are rendered from the template repository recorded in `.sdd/config`
(`SDD_TEMPLATE_REPO`). This skill brings in template changes without losing what the project
customized.

> Talk to the user in Português (Brasil).

## How to act

1. **Status first**: run `bash .sdd/sdd status`. Show the current and latest template versions and
   which managed files carry local customizations.
2. **Run the update**: `bash .sdd/sdd update` (add `--ref <tag-or-sha>` to pin a version,
   `--template <local clone>` to use a local checkout instead of cloning). The script does a
   3-way merge per managed file: pristine previous render (`template-base`) vs the project's
   file (`project`) vs the new render (`template-new`). Exit code 2 means conflicts.
3. **Resolve conflicts** (`<<<<<<< project` … `>>>>>>> template-new` markers): keep the project's
   customizations and apply the template's improvement on top. When both sides changed the same
   gate or checklist, keep the stricter one. Never drop a STOP gate, a checklist item or a
   non-negotiable rule to resolve a conflict. Remove every marker.
4. **Scaffold changes**: if the script reports `.sdd/last-scaffold.diff` (changes to the
   template's `CLAUDE.md`, `ai/`, `specs/_template`, `docs/Product.md`, the dev-agent template or
   the setup docs), read it and propose the equivalent edits to the project's own files — those
   are project-owned and are never merged automatically. Apply what the user accepts, then delete
   the diff file.
5. **Verify**: no `{{...}}` placeholders and no conflict markers left in `.claude/`, `CLAUDE.md`,
   `ai/` (`bash .sdd/sdd status` lists leftovers); relative links in the updated skills resolve;
   every skill's frontmatter `description` kept its routing triggers; each `<!-- sdd:project -->`
   block still holds the project's content.
6. **Report** per file (updated / merged / conflict resolved / kept / new), the version change,
   and suggest `/peca.commit`.

## Contributing back

When a local customization is generic (it would help every project), say so:
`bash .sdd/sdd diff <managed-file>` prints the project's delta against the pristine render, ready
to be applied to the template repository as a pull request.

## Rules

- Touch only `.claude/skills/peca.*`, `.claude/agents/`, `.sdd/` and — for scaffold
  proposals the user accepts — `CLAUDE.md`, `ai/`, `specs/_template`, `docs/Product.md`.
- No application code, no feature specs.
