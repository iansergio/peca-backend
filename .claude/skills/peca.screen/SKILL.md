---
name: peca.screen
description: Screen-design prompt generator. Invoked by /peca.screen or "create a screen prompt / design a screen / prompt for Claude Design". Outputs a ready-to-paste prompt for the PEÇA Claude Design project plus the target `docs/Screens/` filename; writes no code and no files.
metadata:
  role: design-prompter
  related: peca.ask, peca.start, peca.code
---

# peca.screen — Claude Design prompt generator

Turns a short screen request (e.g. "the client's chat screen") into a **complete, screen-specific
prompt** for the PEÇA project in Claude Design. The user pastes it there, exports the
HTML and saves it under `docs/Screens/`, where it later serves as the visual reference for
implementation (consumption rules: `docs/Screens/README.md`).

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. The prompt is written in
> Português (Brasil).

## Hard boundaries

- **Writes no files.** The prompt goes in chat; the user saves the exported HTML.
- No code, no spec edits. If the screen implies behavior no spec covers, say so and point to
  `/peca.start` (the design may come first; implementation may not).
- Global look (palette, typography, a11y, tone, navigation) already lives in the Claude Design
  **project instructions** — do **not** repeat it in the prompt. Reference it with one line
  ("Follow the project instructions") and only add what is specific to this screen.

## Inputs to read (on demand, keep it quick)

- `docs/Product.md` — actors, environments, scope (reject or flag out-of-scope screens).
- `/specs/<feature>/` matching the screen, if one exists — requirements, states, edge cases and
  business rules **override** anything you would infer. Never invent rules a spec doesn't state;
  if none exists, mark assumptions explicitly in the prompt.
- `docs/Screens/*.html` names (and the closest neighbor's content when relevant) — keep
  navigation, headers, component patterns and sample data consistent across screens, and avoid
  duplicating a screen that already exists (propose a variation/update instead).
- The design-tokens doc (if the project has one) — token names only, when the screen needs a
  semantic color role (status, rating, danger).

## How to act

1. **Identify** actor/role, journey, the screen's place in the flow (what comes before/after) and
   its goal. Ask 1–3 sharp questions via `AskUserQuestion` **only** if the actor or the screen's
   purpose is genuinely ambiguous; otherwise state assumptions.
2. **Pick the filename**: `docs/Screens/<actor>-<screen>.html`, English kebab-case
   (`client-home`, `admin-users`). Variations get a suffix (`-empty`, `-v2`).
3. **Write the prompt** in Português (Brasil) using the template below — concrete, ordered top-to-bottom,
   with realistic **synthetic** sample data for the product's locale (never real customer data —
   the prompt leaves the project) and final UI copy.
4. **Report** in chat: the prompt in one fenced block, the target filename, the assumptions made,
   and — if no spec covers the screen — the note that `/peca.start` is needed before
   `/peca.code`.

## Prompt template (fill every section; drop a section only if truly N/A)

```
Follow the PEÇA project instructions.

Screen: <screen name> — <actor>, <journey>
Position in the flow: comes from <previous screen> → leads to <next screen>
Goal: <what the user must achieve on this screen, in one sentence>

Structure (top to bottom):
1. <header: title, back action/icons>
2. <section> — <content, components, hierarchy>
3. ...
N. <fixed primary action / bottom navigation with the active tab>

Components and behavior:
- <component>: <variants, interaction, what happens on tap/click>

States to design (frames side by side):
- Default (with data)
- <Empty | Loading (skeleton) | Error | Success | Disabled — the ones that apply>

Sample data: <realistic names, amounts, dates, distances, ratings for the product's locale>
Copy: <final UI copy for titles, buttons, placeholders, state messages>

Assumptions (no spec yet): <assumed rules, if any>

Deliverable: a single self-contained HTML file (inline CSS, no external dependencies besides
fonts), <viewport width>, semantic HTML (header/main/nav/button/label), colors only via CSS
variables named after the design tokens (--color-primary, --color-muted, …), real text (no lorem
ipsum).
```

The "Deliverable" block is fixed — it keeps the exported HTML mappable to the project's styling
tokens and i18n keys during implementation.

## Shared UI patterns

<!-- sdd:project -->
<!-- /sdd:project -->
