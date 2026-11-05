---
name: peca.security
description: SDD step 5 (essential, do not skip). Invoked by /peca.security or "security review / security analysis / AppSec check / check for vulnerabilities". Runs an AppSec analysis of the feature's changed code and produces a risk-rated report (OWASP-mapped findings + remediation); writes no source files.
metadata:
  depends_on: peca.code
  role: appsec-reviewer
  related: peca.security-audit
---

# peca.security — Security analysis (SDD step 5, ESSENTIAL)

Final gate before code is considered done: a **senior Application Security (AppSec) engineer**
reviews the feature's code for vulnerabilities, security-relevant logic flaws, and violations of
secure-coding practice **before it reaches production**. Produces a report only — it writes no
source files; remediation is applied by the calling workflow (see
[Applying remediation](#applying-remediation)).

> Project context and rules: see `CLAUDE.md` and `ai/module_map.md`. Report in Português (Brasil).

## Dependency and prerequisite (check before anything else)

Depends on `peca.code` (chain: `start` → `tasks` → `code` → `review` → `security`). Confirm
that an implementation exists for the target feature — files referenced in
`/specs/<feature>/tasks.md`, or a non-empty diff (`git status` / `git diff` / `git log`) in the
touched apps/repos.

If no implementation is detectable: **STOP** and ask for `/peca.code` — with no code, there
is nothing to analyze.

## Optional deep audit (`peca.security-audit`)

Applies only when the project ships the vendored `peca.security-audit` skill (Cloudflare's
security-audit) — check that `.claude/skills/peca.security-audit/SKILL.md` exists; it is not
linked here because the skill is optional. It is **opt-in and
occasional** — a periodic deeper pass, never a replacement for this skill's checklist and severity
gate, which always run.

**Ask first.** Right after the prerequisite check, ask the user with `AskUserQuestion` whether to
run it this time. Put the date of the last deep audit in the question (newest
`~/security-audit-skill/<repo>/run-*` directory for the touched repos, or "never"). Options:

1. **No — checklist only** (Recommended; the default)
2. **Yes — scoped to this feature**: scoped run over the feature's changed paths/commits, `quick`
   profile.
3. **Yes — full repo audit**: whole touched repo(s), `standard` profile — the periodic check;
   warn that it spawns many agents and takes long.

Do **not** ask — treat as **No** — when the skill is not installed, when invoked by
`peca.express` or `peca.fix` (no questions in those lanes), when the user already
stated a choice in the invocation, or on a re-run after remediation within the same workflow
(reuse the earlier answer).

**When chosen:** after the checklist analysis, invoke `peca.security-audit` in **full audit
mode** once per touched repo, with that repo's absolute root as target, its default output
directory (outside the repo — never inside it), and the chosen profile/scope. Follow its own
execution-safety rules; it writes no source files. Then merge its results into this report:

- `confirmed` records → [Findings](#3-findings) with the same severity name (`informational` →
  **Low**); a confirmed **critical/high** blocks exactly like a checklist finding. Merge
  duplicates of checklist findings into one entry.
- `needs_validation` records → follow-ups (no severity), with the missing fact.
- Report section [6. Deep audit](#6-deep-audit-only-when-run) summarizes the run.

## Scope

1. Identify the target feature (ask if unclear) and read its `spec.md` and `rules.md` — they define
   who may do what (actors, ownership, permissions), which is the baseline for access-control and
   business-logic findings.
2. Collect the **changed code** in every touched app/repo: uncommitted changes plus the commits of
   this feature (`git diff`, `git diff <base>...HEAD`, or the files listed in `tasks.md`). Include
   the tests — they reveal which abuse cases are (not) covered — and every non-code file in the
   diff (configuration, CI, Dockerfiles, fixtures, seeds, docs): the repository-hygiene items apply
   to them.
3. Read **the surrounding code each change depends on** (guards, validators, persistence adapters,
   session helpers) — a vulnerability often sits at the seam, not in the diff line itself.
4. Do not audit unrelated code. If you spot a serious pre-existing issue outside scope, list it
   separately under "Out-of-scope observations".

## Mandatory checklist

Analyze each item against the changed code; mark it ✅ (ok), ❌ (finding), or N/A.

**Access control & business logic** (highest-impact class in any system with roles or ownership)
- [ ] Every non-public entry point enforces authentication **and** authorization server-side
      (role + **resource ownership**) — no IDOR: a user cannot read/modify another user's data
      by changing an id.
- [ ] Authorization decisions live in the business layer, not only in the UI or an aggregation
      layer.
- [ ] State transitions follow the spec; they cannot be skipped or replayed by calling the API
      directly.
- [ ] No mass assignment: inputs whitelist fields; clients never set roles, statuses, owners,
      prices or flags they don't own.

**Injection & input handling**
- [ ] No injection (SQL/NoSQL/command/template/LDAP): queries parameterized; no string-built
      queries or shell commands from user input.
- [ ] Every input validated (type, length, format, range) at the trust boundary; client-side or
      gateway re-validation is a convenience, never the only check.
- [ ] No XSS: user-generated content rendered as text; no raw HTML injection with user content;
      user-supplied URLs validated (`https:` only, no `javascript:`).
- [ ] No SSRF / open redirect: the server never fetches a user-supplied URL; redirects only to
      allow-listed paths.
- [ ] File uploads (if any): type/size validated server-side, stored outside the app, never
      executed or served with a user-controlled content type; no path traversal.

**Authentication, session & secrets**
- [ ] Session tokens in `httpOnly`, `Secure`, `SameSite` cookies (web) or secure storage
      (native) — never in `localStorage`, never returned in a JSON body unless the spec requires
      a bearer flow.
- [ ] State-changing routes protected against CSRF (SameSite + origin check or token).
- [ ] Passwords hashed with a slow hash (bcrypt/argon2/scrypt); auth errors don't reveal whether
      an account exists; login/reset/verification endpoints are rate-limited or otherwise
      brute-force resistant.
- [ ] No secrets, keys, or credentials in code, tests, fixtures, logs, or `*.example` templates;
      server-only config never exposed to clients. A secret that reached any commit — pushed or
      not — is **compromised**: Critical, and rotating it is part of the remediation (removing it
      later is not enough).

**Sensitive data & privacy** (LGPD/GDPR or the project's data-protection rule)
- [ ] Responses expose only the fields the consumer needs — no password hashes, tokens, internal
      ids/flags, other users' personal data.
- [ ] Personal data stored, logged and exposed at the level the spec allows; new personal-data
      fields classified per the project rule.
- [ ] No PII, tokens, or request bodies with credentials in logs or error messages.

**Errors, configuration & dependencies**
- [ ] Errors return generic messages/codes — no stack traces, SQL, or internal paths to clients;
      failures are handled (no unhandled rejection that crashes the process or leaks state).
- [ ] Debug/introspection surfaces (API docs UI, metrics, admin tools) not exposed in production;
      CORS (if added) is an explicit allow-list; security headers not weakened.
- [ ] Webhooks/callbacks (if any) verify signature/shared secret and are idempotent.
- [ ] New dependencies are justified, come from the official registry under the exact intended
      name (no typosquat; no `git+`, tarball URL or unknown registry in the lockfile; no new
      install hook) and are pinned via the lockfile; run the project's audit command
      ([`/ai/conventions.md#commands`](../../../ai/conventions.md#commands)) and report
      High/Critical advisories that affect the change.

**Repository hygiene & data exfiltration** (what must never reach the repository or leave the
system)
- [ ] No forbidden file in the diff — `.env*` (except documented `*.example` templates), private
      keys and other key material, credential stores, cloud/service-account files, infrastructure
      state (`*.tfstate`), database dumps, exports, HAR or log files.
- [ ] Fixtures, seeds, snapshots, test data and spec/doc examples hold synthetic data only — no
      real people, production identifiers or exported records.
- [ ] No weakened control: TLS verification on; no guard commented out or short-circuited; no
      hardcoded test account, bypass header/parameter or backdoor; no debug/dev mode or
      permissive setting (`CORS *`, schema auto-sync) defaulting on in production configuration.
- [ ] No data leaves the system outside the spec: no new outbound call, telemetry, analytics or
      error reporter sending user or business data to a destination the spec does not name.
- [ ] CI workflows, Dockerfiles, compose files and deployment manifests carry no inline secret,
      never copy `.env*` into an image, and pin third-party actions and base images to a version
      or digest; no `curl … | sh` in scripts or build steps.

<!-- sdd:project -->
- Spring: routes stay denied by default; each `permitAll` in `SecurityConfig` is justified by the spec.
- Ownership: every write loads the resource and compares it to the JWT `sub` in the service.
- JPA: no string-concatenated JPQL or native queries; `LIKE` terms come through Specifications.
- OTP: no response, status or timing difference reveals whether an email has an account.
- Client IP: `X-Forwarded-For` is trusted only behind a proxy that overwrites it.
- Uploads: type detected from content, generated file names, path patterns checked before disk access.
- Errors: `ProblemDetail` carries no exception message that can expose paths or internals.
- Front: no `NEXT_PUBLIC_` secret, JWT stays in the httpOnly cookie, `destinoSeguro` guards redirects, and
  Server Actions re-check the session.
- Compose: no new published port or default credential without a spec.
<!-- /sdd:project -->

## Severity scale

| Level | Meaning | Blocking? |
|---|---|---|
| **Critical** | Remotely exploitable without special conditions; account takeover, mass data exposure, auth bypass | Yes |
| **High** | Exploitable with low effort; another user's data/actions, privilege escalation, stored XSS | Yes |
| **Medium** | Needs specific conditions or limited impact; missing rate limit, verbose errors, weak validation | No — fix or record a tracked follow-up |
| **Low** | Defense-in-depth / hardening | No |

A secret or credential file in any commit is always **Critical**; real personal data, a weakened
control or an untrusted dependency in the diff is at least **High**.

## Expected output

A report with these sections:

### 1. Risk summary
Overall risk level (**Critical / High / Medium / Low / None found**), one paragraph explaining why,
and the scope analyzed (apps/repos, files, commit range).

### 2. Checklist
Item-by-item status of the [mandatory checklist](#mandatory-checklist).

### 3. Findings
For each finding, ordered by severity:
- **Vulnerability** — name (e.g. IDOR, SQL injection, stored XSS, broken authentication,
  sensitive data exposure).
- **OWASP reference** — the matching OWASP Top 10 category (current edition), and the OWASP API
  Security Top 10 category for API endpoints when more precise.
- **Location** — `app/path/file:line` and the exact problematic snippet.
- **Impact** — what an attacker could concretely do, and which actor can exploit it.
- **Severity** — per the scale above.

### 4. Remediation
For each finding: the corrected, secure version of the snippet, placed in the correct
app/module/layer per `ai/module_map.md`, plus the test that would have caught it. If the fix
requires a business-rule decision, say so and point to the spec change — the project rule is
**fix the spec first**.

### 5. Additional good practices
Short, project-specific advice to prevent this class of issue from recurring (e.g. a shared
ownership guard, a lint rule, a test helper for abuse cases).

### 6. Deep audit (only when run)
Per repo: output directory, profile, scope, coverage statement (partial for `quick`/scoped runs),
counts of `confirmed` / `needs_validation` / `rejected`, and where each confirmed record landed in
[Findings](#3-findings). When not run, one line: "Deep audit: not run (last: <date>)".

### Out-of-scope observations (optional)
Serious pre-existing issues seen outside the feature's scope.

## Applying remediation

This skill only reports. The workflow that called it decides how fixes are applied:

- `peca.go` — the user picks **Fix** at the check-point; apply the remediation following
  [`peca.code` non-negotiable rules](../peca.code/SKILL.md#non-negotiable-rules), then
  re-run this skill.
- `peca.express` / `peca.fix` — apply **Critical/High** remediation in the same run
  and re-run this skill once (see each skill).
- Standalone `/peca.security` — report only; suggest `/peca.fix` to apply remediation.

**Done means:** no open **Critical** or **High** finding. Medium/Low findings are either fixed or
listed by the caller as explicit follow-ups.
