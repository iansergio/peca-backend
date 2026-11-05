---
name: peca.commit
description: Commit helper. Invoked by /peca.commit or "commit / commit this / create the commits". Runs a leak scan on the staged changes (secrets, forbidden files, real data, weakened controls, untrusted dependencies), then creates one well-formed commit per touched repository following the project's commit convention; then asks whether to push.
metadata:
  role: committer
  related: peca.go, peca.express, peca.fix, peca.security
---

# peca.commit — One clean commit per repository

Turns the current working tree into clean commits — one per touched repository (one commit for a
single-repo project) — following the
[commit convention](../../../ai/conventions.md#commit-convention) in `ai/conventions.md`. It is
the last gate before anything reaches the repository: nothing is committed until the
[leak scan](#leak-scan-mandatory-before-every-commit) passes.

> Project context: see `CLAUDE.md`. Commit messages in the language the convention states
> (default: Português (Brasil)).

## Gate

- Commit only work that is **done** per the lane that produced it: tests green, lint clean, no
  open Critical/High security finding. If the last report says otherwise, **STOP** and say so.
- Never commit secrets, `.env*` files, credentials, real personal data, a weakened security
  control or an untrusted dependency: the
  [leak scan](#leak-scan-mandatory-before-every-commit) runs on the staged changes of every
  repository before the message is written. A hit is a **STOP** — unstage and report — never a
  warning and never something to "clean up later".
- **Push only after the user confirms it** in this run (step 8), and only commits that passed the
  leak scan. Never force-push (`--force`, `--force-with-lease`), never push tags or other branches
  the run did not create, never pull or rebase to make a rejected push go through. Never amend or
  rewrite history unless the user explicitly asks for it.
- Never bypass the repository's own hooks or checks (`--no-verify`); a failing hook is reported,
  not skipped.

## How to act

1. Run `git status` in each repository of the project (the list is in `ai/module_map.md`; a
   single-repo project has one). Identify which repositories have changes.
2. For each touched repository, group the changes that belong to the current feature/fix. If the
   tree also holds **unrelated** changes, ask the user (`AskUserQuestion`) whether to include,
   leave or stash them — never commit unrelated work silently.
3. Run the [leak scan](#leak-scan-mandatory-before-every-commit) on the staged changes. Stop on
   any hit.
4. Review the staged diff once (`git diff --staged --stat`, then the hunks you have not seen):
   no debug output, no leftover placeholders or conflict markers, no generated or ignored files.
5. Write the message per the convention: subject in the imperative, what and why, feature
   slug/scope when the convention asks for it, ≤ 72 characters; a body only when the subject
   cannot carry the why; keep the attribution trailer the environment adds (e.g.
   `Co-Authored-By`).
6. Commit each repository separately, provider before consumer (e.g. API before UI), and the
   repository holding `/specs` last so its message can reference the others.
7. Report: repository, short hash, subject, number of files; the leak-scan result; and anything
   left uncommitted with the reason.
8. **Push — ask first.** For each repository with new commits, list the branch, its upstream (or
   "none") and the commits that would go out (`git log --oneline @{u}..HEAD`, or the commits of
   this run when there is no upstream). If that list holds commits this run did not create, run
   the [leak scan](#leak-scan-mandatory-before-every-commit) on their diff (`git diff @{u}..HEAD`)
   first; a hit is a **STOP** for that repository. Then ask once (`AskUserQuestion`): push all
   listed repositories, choose which ones, or don't push — flag in the question any branch that is
   the default/protected branch. On yes, push in the commit order with `git push` (`git push -u
   origin <branch>` when there is no upstream). A rejected push (non-fast-forward, protected
   branch, failing hook) is reported with its message and left as is — no retry with force, no
   pull or rebase unless the user asks.

## Leak scan (mandatory before every commit)

What must never reach the repository. Run it on the **staged** changes of each repository
(`git diff --staged`), after grouping and before writing the message. Any hit is a **STOP**:
unstage the file (`git restore --staged <path>`), leave the working tree as it is, and report the
path, what matched and what to do instead. Never commit around a hit — no `--no-verify`, no
splitting or encoding the value, no "temporary" commit. A secret that already reached a commit,
pushed or not, is **compromised**: say so, recommend rotating it at once, and rewrite history only
if the user asks.

1. **Forbidden files** — `git diff --staged --name-only --diff-filter=AMR` lists none of:
   - environment and local config: `.env`, `.env.*`, `*.local` config files — except committed
     templates (`*.example`, `*.sample`, `*.template`) holding placeholders only;
   - key material and credential stores: `*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`,
     `*.keystore`, `id_rsa*`, `id_ed25519*`, `*.ppk`, `credentials.json`,
     `service-account*.json`, `.npmrc` / `.pypirc` / `.netrc` / `.htpasswd` carrying tokens,
     kubeconfigs, `*.tfstate`, `*.tfvars` with real values;
   - data: database dumps and local databases (`*.dump`, `*.sqlite*`, `*.db`, `*.bak`, `*.sql`
     dumps), exports (`*.csv`, `*.xlsx`, `*.json`) holding real records, `*.har`, `*.log`, heap or
     core dumps, screenshots of production data.
   Migrations and seeds with synthetic data are fine. When a hit is not git-ignored, add the
   pattern to `.gitignore` in this commit.
2. **Secret literals** — no added line (`git diff --staged -U0 | grep '^+' | grep -niE '<pattern>'`)
   matches any of:
   - private keys: `-----BEGIN [A-Z ]*PRIVATE KEY`;
   - provider keys: `(AKIA|ASIA)[0-9A-Z]{16}` (AWS), `AIza[0-9A-Za-z_-]{35}` (Google),
     `gh[pousr]_[A-Za-z0-9]{36}` and `github_pat_[A-Za-z0-9_]{22,}` (GitHub),
     `glpat-[A-Za-z0-9_-]{20,}` (GitLab), `xox[abprs]-[A-Za-z0-9-]{10,}` (Slack),
     `[sr]k_(live|test)_[A-Za-z0-9]{24,}` (Stripe), `sk-[A-Za-z0-9_-]{20,}` (OpenAI-style);
   - tokens and credentials in URLs: `eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}` (JWT),
     `://[^/[:space:]:@]+:[^@[:space:]]{4,}@` (`user:password@host`);
   - assignments: `(password|passwd|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|client[_-]?secret)["']?[[:space:]]*[:=][[:space:]]*["'][^"'$<{]{8,}["']`;
   - any literal of 32+ hex or base64 characters bound to a name, unless it is a hash in a lockfile
     or a documented test vector.
   Obvious placeholders (`changeme`, `<your-key>`, `${VAR}`, `process.env.X`, `example`) are not
   hits; a real-looking value is a hit even in a test, fixture, comment or `*.example` file. If a
   secret scanner is installed (`gitleaks protect --staged`, `trufflehog`, `detect-secrets`), run it
   too — it complements the list, never replaces it.
3. **Real personal data** — new fixtures, seeds, snapshots, test data, migrations and spec/doc
   examples hold synthetic values only: no real people (name with e-mail, phone, document number
   or address), no production identifiers, no exported records. Use the project's factories or
   faker, reserved domains (`example.com`, `.test`), `555` phone numbers and documentation IP
   ranges.
4. **Weakened controls and bypasses** — added lines carry no disabled TLS verification
   (`rejectUnauthorized: false`, `verify=False`, `InsecureSkipVerify`, `--insecure`,
   `NODE_TLS_REJECT_UNAUTHORIZED=0`), no guard commented out or short-circuited (`if (true ||`,
   `return true // auth`), no hardcoded test account, "magic" bypass header or parameter, or
   backdoor, no debug/dev mode or permissive setting defaulting on (`DEBUG=true`, `CORS *`,
   schema auto-sync, "allow all"), no `eval`/dynamic code execution over input, and no logging of
   request bodies, headers, tokens or user records.
5. **Supply chain and infrastructure files** — a new dependency comes from the official registry
   under the exact intended name (no typosquat; no `git+`, tarball URL or unknown registry in the
   lockfile), is pinned by the lockfile and adds no install hook; no `curl … | sh` in scripts, CI
   or Dockerfiles; CI workflows, Dockerfiles, compose files and deployment manifests carry no
   inline secret and never copy `.env*`; third-party CI actions and base images are pinned to a
   version or digest.

<!-- sdd:project -->
- Check both repositories in a feature: `peca-backend` and `../peca-front-end`.
- Allowed: `.env.example` in both repositories, including the labelled development defaults in the
  backend's; `jwt.secret` in `src/test/resources/application.properties` (test-only value); the
  `JWT_SECRET` development default in `docker-compose.yml`, until P-4 in
  `docs/Architecture.md` is decided; the truncated `eyJhbGciOiJIUzI1NiJ9...` example in `TokenResponse`;
  Flyway files under `src/main/resources/db/migration/`.
- Never allowed: a real `MAIL_PASSWORD` (Gmail app password), or any `JWT_SECRET` from a local `.env`.
- Notify the repository owner when a secret leaks.
<!-- /sdd:project -->

## Expected output

The list of commits created (repository, short hash, subject), the leak-scan result per
repository (clean, or what was unstaged and why), what was intentionally left uncommitted, and the
push result per repository (pushed to `<remote>/<branch>`, not pushed by choice, or rejected with
the reason).
