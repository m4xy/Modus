# 00 — The Modus Constitution

Non-negotiable rules. These are not preferences. Where a rule can be checked by a
machine, the enforcing tool is named. Where it cannot yet be, the enforcement gap is
stated explicitly so it can be closed rather than forgotten.

**Precedence:** this file > every other file in `documentation/` > code comments >
anything a person or agent says in conversation.

---

## 1. Strict DDD layering

Modus is hexagonal (ports and adapters) with a strict, acyclic dependency direction.
Dependencies point **inwards**. Nothing inside knows anything about outside.

```
        backoffice/  e2e/
             |
        app/modus-server        (wiring only — no logic)
             |
   +---------+---------+
   |                   |
adapters/            modules/       (Spring, HTTP, files, git, claude)
   |                   |
   +---------+---------+
             |
core/core-application            (use cases, orchestration over the domain)
             |
core/core-domain                 (aggregates, VOs, events, ports — ZERO frameworks)
```

### 1.1 The dependency rules

| Module | MAY depend on | MUST NOT depend on |
|---|---|---|
| `core/core-domain` | Kotlin stdlib, `java.time` **types** only | Spring, Jackson, JPA, HTTP, file IO, SLF4J, any `adapters/*`, any `modules/*`, `core-application` |
| `core/core-application` | `core-domain`, Kotlin stdlib, coroutines | Spring, Jackson, any `adapters/*`, any `modules/*`, `app/*` |
| `adapters/adapter-*` | `core-domain`, `core-application`, their own third-party libs | Any other `adapters/*`, `app/*`, `modules/*` |
| `modules/module-*` | `core-domain`, `core-application`, adapter **ports** only | Another module's internals, `app/*`, any `adapters/*` implementation |
| `app/modus-server` | Everything | Nothing (it is the top) |
| `backoffice/` | The REST API contract | Any Kotlin source |
| `e2e/` | The running system over HTTP | Any Kotlin source |

**Enforced by:** ArchUnit (`build-logic` convention plugin `modus.archunit`), plus
Gradle `api`/`implementation` module boundaries. The table in `10-architecture.md` §4
is the machine-readable form; keep the two in sync.

### 1.2 Ports live inside, adapters implement them

An outbound port is an interface declared in `core-domain` (or in `core-application`
for use-case-shaped ports). The adapter implements it. The domain **never** imports the
adapter. Wiring happens exactly once, in `app/modus-server`.

### 1.3 `core-domain` is framework-free — absolutely

No Spring. No Jackson. No JPA. No SLF4J. No `java.io.File`, no `java.nio.file`.
No `System.currentTimeMillis()`, no `Instant.now()`, no `LocalDate.now()` — time enters
through a `Clock` port passed as a constructor argument. No `UUID.randomUUID()` —
identifiers come through an `IdGenerator` port. No static singletons, no service
locators, no reflection, no coroutine dispatchers.

**Rationale:** `core-domain` must be testable with zero setup in under a second, and it
must survive a change of persistence or transport without editing a single line.

**Enforced by:** ArchUnit package-dependency rules plus the custom Detekt rule
`ForbiddenDomainApi` (see `30-code-style.md` §4).

---

## 2. Flat-file first

> **Modus stores its durable state as files on disk. There is no database.**

| # | Rule |
|---|---|
| 2.1 | The source of truth for domains, work items, memories, permissions and run records is the filesystem. |
| 2.2 | Human-facing entities (work items, memories, decisions) are stored as **Markdown with YAML frontmatter**. The file *is* the record; it is not a rendering of a record held elsewhere. |
| 2.3 | Machine-facing, high-volume, append-only data (agent run events, cost events, audit trail) is stored as **newline-delimited JSON append-only logs**. |
| 2.4 | Every write is atomic: temp file in the same directory → `fsync` → `rename` → `fsync` the directory. Never write in place. See `40-durability.md`. |
| 2.5 | Indexes, caches and projections are **derived**. They may be deleted at any moment and rebuilt from the files. Nothing may live only in an index. |
| 2.6 | No SQL, no embedded database, no ORM, no key-value server. Adding one requires an ADR that supersedes `adr/0002`. |

**Rationale and alternatives considered:** `adr/0002-flat-file-over-database.md`.

**Enforced by:** ArchUnit (no `java.sql`, `javax.sql`, `jakarta.persistence`,
`org.hibernate`, `org.jooq` types anywhere in the repository) plus a Gradle
dependency-verification rule in `build-logic` that fails the build if a database driver
appears on any configuration.

---

## 3. The evidence rule

> **No assertion is recorded as true without evidence attached.**

Applies to every durable memory, every work-item transition to `done`, and every claim
in a pull-request body.

- "The tests pass" MUST carry the command, its exit code, and its output tail.
- "This endpoint returns 404" MUST carry the request and the response.
- "The library does X" MUST carry a `file:line` citation or a fetched URL with a quote.
- "I fixed it" MUST carry a diff reference and a validating command.

An unevidenced statement is a **hypothesis** and MUST be labelled as one. Hypotheses may
be written down; they may not be stored as memories, and they may not close a work item.

The evidence record shape, the accepted evidence kinds, and invalidation rules are in
`50-memory-and-evidence.md`.

**Enforced by:** schema validation on memory files at write time in
`adapters/adapter-persistence-flatfile`; a transition guard in the `work` context that
refuses `done` without at least one evidence record per success criterion.
**Enforcement gap:** PR-body evidence is currently a review responsibility; a CI check
on PR body structure is a follow-up work item.

---

## 4. Investigate; do not ask

> **If a question can be answered by investigation, you MUST investigate. Asking a human
> is a last resort, not a first move.**

Before you may ask a human anything, you must have:

1. Searched the repository (`rg`, `git log`, `git blame`).
2. Read the file in `documentation/` that the index says covers it.
3. Read the memories for the relevant domain / epic / story.
4. Run the thing and observed what it actually does.
5. Read the upstream library source, or its official documentation.

You **may** ask a human when, and only when, the answer is a genuine preference or an
external fact that cannot be discovered:

- A product or priority decision with more than one defensible answer.
- A credential, an access grant, or approval to spend money.
- A destructive or irreversible action (force-push, data deletion, production change).
- A conflict between two ratified rules that needs a ruling.

When you do ask, ask **once**, and include: what you tried, what you found, the options
you see, and your recommended default. Never ask an open question ("what should I do?").

**Anti-pattern:** blocking on a human for something a five-second `rg` would answer.
That is the most expensive failure mode in the system — it costs a human context switch,
which is worth more than any token budget it saves.

**Enforced by:** review, and the SOP in `80-agent-operating-procedure.md`.
**Enforcement gap:** "questions asked per work item" should be recorded by the
`execution` context; not yet implemented.

---

## 5. Prefer skills over improvisation

If you are doing something for the second time, you extract a skill. If a skill exists
for what you are about to do, you use it rather than reinventing the approach.

Modus prefers **celebrity skills** — a small number of well-known, well-named, heavily
reused skills — over a long tail of one-off scripts. See `70-skills.md`.

---

## 6. The agent context budget

> **An agent's context window MUST stay under 300,000 tokens for the entire lifetime of
> a work item.**

A hard ceiling, not a target. Past 300k, judgement degrades, cost per useful action
rises superlinearly, and self-consistency across the task breaks down.

### 6.1 Concrete tactics — apply in this order

| # | Tactic | How |
|---|---|---|
| 1 | Read the index, not the corpus | Use the "read this when" table in `documentation/README.md`. Never read the whole package per task. |
| 2 | Search, then read a range | `rg -n 'pattern'`, then read only the lines around the hit. Never `cat` a 2,000-line file to find one symbol. |
| 3 | Delegate fan-out to subagents | A broad search that would dump 20 files into your window goes to a subagent, which returns the conclusion. Their tokens are not your tokens. |
| 4 | Cap tool output | Pipe through `head`, `tail`, `wc -l`, `--stat`. Always `git diff --stat` before `git diff`. Run tests, then read the failure tail only. |
| 5 | Never restate large files in your own messages | Reference `path:line`. The file is on disk; quoting it doubles its cost. |
| 6 | Externalise state | Write findings into the work item or a memory, not into your context. Reading one back later is cheaper than carrying it throughout. |
| 7 | One work item, one agent, one window | Do not batch unrelated work items into one run. Finish, commit, start fresh. |
| 8 | Budget checkpoints | At ~100k and ~200k, stop; re-read your own success criteria; summarise progress into the work item; discard everything else. |
| 9 | Cheap shapes first | `--stat`, `--name-only`, `rg -l`, `wc -l` before full content. |
| 10 | Escalate rather than crawl | At 250k and not done, the work item is mis-sized. Split it, record what you learned as evidence-backed memories, hand off. |

### 6.2 Work-item sizing corollary

A work item that a competent agent cannot complete inside 300k tokens is mis-sized and
MUST be split before work starts. Restating the success criteria (SOP step 2) is where
you catch this — before you have spent anything.

**Enforced by:** the `execution` context records peak context per agent run and flags
runs over 240k (80% of budget) as at-risk. **Enforcement gap:** the recorder is not yet
implemented; until it is, self-report peak context in the PR body.

---

## 7. The workflow: branch → work item → PR → review → merge

### 7.1 No direct commits to `main`. Ever.

`main` is protected. Every change — including documentation, including a one-character
typo fix — arrives through a pull request.

**Enforced by:** GitHub branch protection on `main` (owned by the CI work package), plus
a local `pre-push` hook that refuses a push to `main`.

### 7.2 The sequence

1. **Work item first.** Every branch has exactly one work item in `beans/`. If none
   exists, create it before you create the branch. The work item states the success
   criteria **before** the work starts. On-disk schema: `documentation/90-work-items.md`
   (owned separately).
2. **Branch.** Named `<kind>/<slug>`, `<kind>` ∈ {`feat`, `fix`, `docs`, `chore`,
   `refactor`, `test`, `perf`, `build`}. Example: `docs/foundation-documentation-package`.
3. **Work.** Follow `80-agent-operating-procedure.md`.
4. **Self-validate before opening the PR.** The full local gate must pass:
   `./gradlew check` (compile + ktlint + Detekt + ArchUnit + unit + integration tests),
   plus Playwright for any change touching `backoffice/`.
5. **Pull request.** Conventional-commit title. The body states what changed, the success
   criteria from the work item, and the **evidence** each was met by. A PR body with a
   claim and no evidence is rejected without further review (§3).
6. **Review.** See §7.4.
7. **Merge.** Squash merge; the squashed message is the PR title plus body. The work item
   transitions to `done` with the merge commit as evidence.

### 7.3 Commit messages

Conventional Commits: `<type>(<scope>): <subject>` — imperative mood, no trailing period,
subject ≤ 72 characters. Scope is the module or bounded context (`core-domain`,
`adapter-rest`, `work`, `cost`, `docs`).

Agent-authored commits MUST end with a `Co-Authored-By:` trailer naming the model that
produced them, so cost and quality can be attributed after the fact.

**Enforced by:** a commit-message check in CI.

### 7.4 Review

- **Review reviews design, correctness, and evidence. Review never reviews style.**
  Style is the build's job (`30-code-style.md`). A style comment in review is a defect in
  the toolchain: fix the tool in a follow-up work item, and say so in the thread.
- Every review comment resolves in exactly one of three ways: a code change; a rule
  encoded into `documentation/` or into a tool; or an explicit "won't do" with a reason.
- Review is itself cost-attributed. See `60-cost-model.md` §6 — review runs at the
  cheapest model and effort that reliably catches the class of defect in play, and that
  choice is recorded against the work item.

---

## 8. Domain scoping and modularity

- The root of every API resource is `/domains/{domainId}`. There are no global resources
  except identity and bootstrap. See `10-architecture.md` §5.
- A **Module** is installed *into* a domain. A module installed in domain A MUST NOT be
  visible — in the API, in the backoffice, in search results, or in error messages — to
  an actor whose permissions do not cover domain A. Absence is rendered as `404`, never
  `403`, so that existence is never leaked.
- Every domain defines its own process: its own work-item states, its own definition of
  done, its own required evidence kinds, its own model and effort policy. Modus supplies
  defaults; a domain may override any of them. Code MUST NOT hardcode a single process.

**Enforced by:** an ArchUnit rule that every REST controller mapping begins with
`/domains/{domainId}` (with a named allowlist for identity/bootstrap routes), plus an
integration suite asserting the 404-not-403 property for every cross-domain access path.

---

## 9. Mechanical enforcement over discipline

Any rule that a human or an agent has to *remember* will eventually be broken. If a rule
matters, it gets a tool: ktlint, Detekt (including custom rules), ArchUnit, a Gradle
check, a schema validator, or a Playwright assertion.

Corollary: **the build is the definition of correct.** A green build with a bad outcome
means the build is wrong. Fix the build.

See `30-code-style.md`.

---

## 10. The UI is a deliverable, not an afterthought

The backoffice must be genuinely beautiful, and it must be verified. Every user-facing
flow has a Playwright test in `e2e/`. Visual regression is checked, not eyeballed. A
backoffice change without a corresponding `e2e/` change is incomplete unless the PR body
states why no user-visible behaviour changed.

---

## 11. Cost consciousness

Every stage of every workflow carries an attributed spend figure, in dollars, recorded
against the work item. Model and effort selection is a deliberate, recorded decision,
never a default that nobody chose. Repeated expensive tasks are converted into cheaper
defined skills. See `60-cost-model.md`.

---

## 12. Self-hosting is the destination

Modus will eventually manage its own development. Take every decision as if Modus were
already running this repository. When a design that is convenient for a human operator
conflicts with one that is legible to an agent, choose the agent — then render it for
the human at the edge.
