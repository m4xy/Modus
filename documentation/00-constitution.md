---
id: doc:00-constitution
title: The Modus constitution
status: active
superseded_by: null
read_when: always
provides:
  - doc:00-constitution#layering
  - doc:00-constitution#flat-file-first
  - doc:00-constitution#evidence-rule
  - doc:00-constitution#context-budget
  - doc:00-constitution#workflow
  - doc:00-constitution#bean-lifecycle
  - doc:00-constitution#domain-scoping
  - doc:00-constitution#mechanical-enforcement
  - doc:00-constitution#observed-failing
  - doc:00-constitution#orchestrator
depends_on: [doc:10-architecture, doc:30-code-style, doc:40-durability, doc:50-memory-and-evidence, doc:60-cost-model, doc:70-skills, doc:80-agent-operating-procedure]
---

# 00 — The Modus Constitution

Non-negotiable rules. These are not preferences. Where a rule can be checked by a
machine, the enforcing tool is named. Where it cannot yet be, the enforcement gap is
stated explicitly so it can be closed rather than forgotten.

**Precedence:** this file > every other file in `documentation/` > code comments >
anything a person or agent says in conversation.

---

## 1. Strict DDD layering <a id="layering"></a>

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
| `modules/module-*` | `core-domain`, `core-application`, Spring, their own third-party libs | Any `adapters/*`, another `modules/*`, `app/*` |
| `app/modus-server` | Everything | Nothing (it is the top) |
| `backoffice/` | The REST API contract | Any Kotlin source |
| `e2e/` | The running system over HTTP | Any Kotlin source |

A module is wired like an adapter, so Spring is permitted in one; it is the **core** that
is framework-free (§1.3). A module never depends on an adapter: ports are declared in
`core` (§1.2), so "adapter ports" is not a thing that exists to depend on
(`10-architecture.md` §7.2).

**Enforced by:** ArchUnit (`build-logic` convention plugin `modus.archunit`), plus
Gradle `api`/`implementation` module boundaries. The table in `10-architecture.md` §4.1
is the machine-readable form and the one an ArchUnit test is generated from; this table
is its prose rendering. If they disagree, §4.1 wins and this table is the bug.

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

**Enforced by:** ArchUnit package-dependency rules.
**Enforcement gap:** the custom Detekt rule `ForbiddenDomainApi` this section relied on
does not exist; see `30-code-style.md` §4 and `bean:0026`.

---

## 2. Flat-file first <a id="flat-file-first"></a>

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

**Enforcement gap:** neither exists yet — no ArchUnit rule scans for `java.sql`,
`javax.sql`, `jakarta.persistence`, `org.hibernate` or `org.jooq` types (`domainIsFrameworkFree`
covers `jakarta..`/`javax..` for `core-domain` only, not `java.sql` and not the rest of the
repository), and `build-logic` has no Gradle dependency-verification rule for database
drivers. `bean:0027` carries the audit.

---

## 3. The evidence rule <a id="evidence-rule"></a>

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

**Enforcement gap:** neither exists — schema validation on memory files at write time
(`adapters/adapter-persistence-flatfile` is an empty placeholder with no tests, `bean:0017`)
nor the transition guard in the `work` context refusing `done` without evidence (`work`
is not built, `bean:0013`). PR-body evidence is currently a review responsibility. A CI check on
PR body structure is owned by `bean:0001`, which lists it under "Follow-up work items to
raise" and is accountable for raising it.

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
`execution` context; not yet implemented. Owned by `bean:0001`, under "Follow-up work
items to raise".

---

## 5. Prefer skills over improvisation

**The third time you do something, you extract a skill.** The second time, you notice and
record it; the third time you act. If a skill exists for what you are about to do, you use
it rather than reinventing the approach.

The threshold is three, not two, because a task done twice may never happen again, and a
skill written for a task that does not recur is a maintenance cost with no payback
(`70-skills.md` §2.2). The **single normative statement** of the extraction thresholds is
the trigger table in `60-cost-model.md` §5.3 — it is the one `module-cost` measures. This
section states the principle; it does not restate the numbers, and where it appeared to
disagree with §5.3 it was this file that was wrong.

Modus prefers **celebrity skills** — a small number of well-known, well-named, heavily
reused skills — over a long tail of one-off scripts. See `70-skills.md`.

---

## 6. The agent context budget <a id="context-budget"></a>

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
implemented; until it is, self-report peak context in the PR body. Owned by `bean:0001`,
under "Follow-up work items to raise".

---

## 7. The workflow: branch → work item → PR → review → merge <a id="workflow"></a>

### 7.1 No direct commits to `main`. Ever.

`main` is protected. Every change — including documentation, including a one-character
typo fix — arrives through a pull request.

**Enforced by:** repository ruleset `main-protected` (id `21765196`, `enforcement: active`)
carrying the `pull_request`, `non_fast_forward` and `deletion` rules, plus
`required_review_thread_resolution`, so an unresolved review thread blocks merge. Verify with
`gh api repos/m4xy/Modus/rulesets`. Note the classic
`gh api repos/m4xy/Modus/branches/main/protection` endpoint returns `404 Branch not protected`
for a repository that uses rulesets — that 404 is not evidence of an unprotected branch, and
reading it as such once produced a false `Enforcement gap:` here.

### 7.2 The sequence

1. **Work item first.** Every branch has exactly one work item in `beans/`. If none
   exists, create it before you create the branch. The work item states the success
   criteria **before** the work starts. On-disk schema: the upstream `hmans/beans`
   convention, `.beans/<prefix><id>--<slug>.md`. That store **is** a work store in the sense
   of `doc:40-durability` §3 —
   specifically the one belonging to the `modus` domain, this repository. See
   `40-durability.md` §3.1: there is one work-item concept, not two.
2. **Branch.** Named `<kind>/<slug>`, `<kind>` ∈ {`feat`, `fix`, `docs`, `chore`,
   `refactor`, `test`, `perf`, `build`}. Example: `docs/foundation-documentation-package`.
3. **Work.** Follow `80-agent-operating-procedure.md`.
4. **Self-validate before opening the PR.** This is **the gate**, stated once, here.
   `30-code-style.md` §6 and `80-agent-operating-procedure.md` step 6 cite this block;
   neither carries its own command list.

   ```
   ./gradlew ktlintFormat    # fix formatting first, so the gate never fails on it
   ./gradlew qualityCheck    # compile, ktlint, Detekt, ArchUnit, docs-lint, the coverage
                             # ratchet, and both test suites in every module — build-logic
                             # included
   ```

   `qualityCheck` is one command by design: a gate you have to remember the halves of is a
   gate people run some of. It is every module's `check`, the backoffice's own checks, and
   the tasks an aggregating root has to ask for by name — the included build's own gates and
   `docsLint` (`build.gradle.kts`).

   **Enforced by:** `qualityCheck` reaches `backoffice/` and `e2e/` through the npm scripts
   they already declare, as `backofficeTypecheck`, `backofficeLint` and
   `backofficeFormatCheck` (`bean:0029`). Each was observed rejecting a planted violation:
   `error TS2322: Type 'string' is not assignable to type 'number'`, `error 'unused' is
   assigned a value but never used`, and `[warn] Code style issues found`. `backoffice/` and
   `e2e/` are still not Gradle projects — one tool per language, each configured where its
   ecosystem expects. `doc:30-code-style` §6 carries what those checks do **not** cover.

   **Playwright stays outside the gate**, as `./gradlew e2eTest`. It needs a built and
   running system and takes minutes; inside `check` it would make the fast gate slow enough
   that agents stop running it. It is required only when user-visible behaviour changed.

   **CI runs a subset of this per change, so the promise is one-directional**
   (`.github/workflows/ci.yml`, `bean:0045`). A Kotlin-only change runs `qualityCheck`
   without the backoffice checks; a backoffice-only change runs those and `e2eTest` and
   nothing else; a change touching both, or `.editorconfig`, or the workflow, runs
   everything. So: a green local `qualityCheck` **plus** `e2eTest` implies a green CI run,
   and the reverse does not hold. The local command stays the superset deliberately — the
   moment CI can run something local cannot, this promise is gone.

   Run `e2eTest` before opening a pull request that touches `backoffice/` or `e2e/`. CI is
   not the place to discover that Playwright is red.

   **Enforcement gap:** the `main-protected` ruleset carries `pull_request`,
   `non_fast_forward` and `deletion`, and **no `required_status_checks` rule at all** —
   verified with `gh api repos/m4xy/Modus/rulesets/21765196`. A red CI run has never blocked
   a merge. The `gate` job exists to be that required check, since a skipped half reports
   neither success nor failure and a ruleset naming `build` directly would block every
   change that legitimately skips one. Turning the requirement on is `bean:0047`, held back
   one step so the check is observed green on a real pull request before it can block
   anything.

5. **Pull request.** Conventional-commit title. The body states what changed and the
   judgement calls a reviewer should check. The **evidence lives in the work item**, beside
   the criterion it satisfies, and the body names the bean rather than restating it
   (`adr:0005-evidence-lives-in-the-work-item`). Evidence written twice is evidence that can
   disagree with itself, and it did. A claim with no evidence anywhere is rejected without
   further review (§3).

   A bean with `status: completed` is **final**: it may gain entries under a trailing
   `## Amendments` section and may change in no other way. An observation is amended, never
   edited — a reader must see both what was believed and what was found.
   **Enforced by:** `docs-lint` check 11, which classifies each changed bean by the
   `status:` it has on the merge base and was observed rejecting an in-place edit, a
   non-amendment append, a malformed amendment heading and an amendment with no evidence.
6. **Review.** See §7.4.
7. **Merge.** Squash merge; the squashed message is the PR title plus body.

### 7.2.1 A bean's status through its own pull request <a id="bean-lifecycle"></a>

`todo` → `in-progress` when the branch is cut → `completed` **after** the merge, in a
separate change.

The bean stays `in-progress` for the whole life of its own pull request, including through
review. It is not set to `completed` in the change under review, for two reasons that
compound:

- A bean cannot close itself. Its evidence includes the merge, and the merge is the thing
  the pull request is asking for.
- `docs-lint` check 11 makes a `completed` bean append-only. Setting it `completed` in its
  own branch would freeze it against the author's own review fixes — every finding would
  then need an `## Amendments` entry to correct a bean that had not yet landed.

So closing a bean is always the *next* change, and is the first act of the session after a
merge. This was convention rather than rule until `bean:0035` found it undocumented and
load-bearing.

### 7.3 Commit messages

Conventional Commits: `<type>(<scope>): <subject>` — imperative mood, no trailing period,
subject ≤ 72 characters. Scope is the module or bounded context (`core-domain`,
`adapter-rest`, `work`, `cost`, `docs`).

Agent-authored commits MUST end with a `Co-Authored-By:` trailer naming the model that
produced them, so cost and quality can be attributed after the fact.

**Enforcement gap:** no commit-message check exists in `.github/workflows/ci.yml`, and the
history disagrees with this rule for every commit before it was stated. `bean:0024`
carries reconciling the rule with the history.

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

## 8. Domain scoping and modularity <a id="domain-scoping"></a>

- The root of every API resource is `/domains/{domainId}`. There are no global resources
  except identity and bootstrap. See `10-architecture.md` §5.
- A **Module** is installed *into* a domain. A module installed in domain A MUST NOT be
  visible — in the API, in the backoffice, in search results, or in error messages — to
  an actor whose permissions do not cover domain A. Absence is rendered as `404`, never
  `403`, so that existence is never leaked.
- Every domain defines its own process: its own work-item states, its own definition of
  done, its own required evidence kinds, its own model and effort policy. Modus supplies
  defaults; a domain may override any of them. Code MUST NOT hardcode a single process.

**Enforcement gap:** neither `ControllersAreDomainScoped` (ArchUnit) nor `DomainScopedRoute`
(Detekt) exists — `adapters/adapter-rest` is a placeholder with no controllers to check —
nor does the integration suite asserting 404-not-403 for every cross-domain access path.
`bean:0018` carries all three. Once they exist, the **non-domain-scoped route allowlist**
has one normative copy, `10-architecture.md` §5.1; neither rule, nor this section, carries
a second.

---

## 9. Mechanical enforcement over discipline <a id="mechanical-enforcement"></a>

Any rule that a human or an agent has to *remember* will eventually be broken. If a rule
matters, it gets a tool: ktlint, Detekt (including custom rules), ArchUnit, a Gradle
check, a schema validator, or a Playwright assertion.

Corollary: **the build is the definition of correct.** A green build with a bad outcome
means the build is wrong. Fix the build.

See `30-code-style.md`.

### 9.1 A gate is unverified until it has been observed failing <a id="observed-failing"></a>

> **A mechanism nobody has watched reject a real violation is not enforcement. It is a
> claim.**

- Every `Enforced by:` line MUST name a mechanism that has been observed rejecting a
  planted violation of the rule it claims to enforce. The observation is recorded
  verbatim (§3), in the work item and in the pull-request body.
- The procedure is `35-testing.md` §6, applied to gates rather than to tests: plant,
  observe the named mechanism fail, revert.
- A mechanism that cannot be made to fail MUST be demoted to an `Enforcement gap:` naming
  the work item that closes it. An unfalsifiable gate is worse than an admitted gap,
  because it also stops anyone looking.

**A gate can be real, correct, observed failing — and still not run.** `docs-lint` check 11
was watched rejecting four planted violations and shipped with an `Enforced by:` line. It
then never ran in CI once, for its entire life: the job used `actions/checkout@v4` at the
default `fetch-depth: 1`, which creates no `refs/remotes/origin/main`, so every diff-shaped
check silently skipped itself and `docs-lint` exited 0 (`bean:0051`). The observation was
made locally and quietly generalised to CI, which was never part of it.

**An `Enforced by:` line about a diff-shaped check is also a claim about the checkout
configuration.** Observe it where it is claimed to run, not only where it is convenient to
run it — and make the run say what it examined, because a check that examines nothing and a
check that passes both print `OK`. Check 11's inert CI runs were distinguishable from real
ones by exactly one character: `- introduced` rather than `0 introduced`.

Reading a tool's own configuration is not verification either, and this is the sharpest
form of the rule. `eslint-plugin-import`'s `no-cycle` was installed, registered, resolved,
and reported by `eslint --print-config` as `[2]` — and passed a planted two-file cycle,
because in flat config the plugin takes its parser from `settings['import/parsers']` and
with none set it parses no TypeScript file, follows nothing, and reports nothing
(`bean:0046`). Every artefact a reader would consult said the rule was on. Only the plant
said otherwise.

Mechanisms in this repository that reported success while enforcing less than they claimed.
Each was found by trying to make it fail, and none by reading it.

| mechanism | what it actually enforced |
|---|---|
| 33 enabled Detekt rules | nothing — the CLI analyses the PSI only, so every type-resolution rule was skipped in silence |
| a passing test | nothing — it still passed with the feature it named deleted |
| a `PIPE_BUF` atomic-append size threshold | nothing — the threshold was wrong, and is removed |
| two `Enforced by:` lines | nothing — the rules they named did not exist |
| `ContextInternalsAreSealed`, `PublishedLanguageAllowlist` | nothing — documented as enforcing, never implemented |
| the coverage baseline | nothing — it was resettable to zero coverage with a green build |
| the downward-write guard on that baseline | half of it — it checked the missed columns and not the covered ones |
| `docs-lint` check 11 | nothing — twice. First it compared the merge base to `HEAD`, equal on a fresh branch, so four plants "passed". Then, fixed and proven locally, it never ran in CI at all: `fetch-depth: 1` leaves no `origin/main` to diff against |
| `import/no-cycle`, installed and reported `[2]` | nothing — no parser, so no file was followed |
| the Playwright suite, as evidence a refactor was safe | nothing — it passed identically before and after four component rewrites, because it never drove the paths they changed |

**Enforcement gap:** the `Enforced by:` lines already in this package predate this rule
and have not been audited against it. `bean:0027` carries the audit; `bean:0026` carries
the Detekt entries it has already reached.

---

## 10. The UI is a deliverable, not an afterthought

The backoffice must be genuinely beautiful, and verified. Every user-facing flow has a
Playwright test in `e2e/`; a backoffice change without a corresponding `e2e/` change is
incomplete unless the PR body says why no user-visible behaviour changed.

---

## 11. Cost consciousness

Every stage carries an attributed spend figure, in dollars, against the work item. Model and
effort selection is a recorded decision, never a default nobody chose. See `60-cost-model.md`.

---

## 12. The orchestrator does not implement <a id="orchestrator"></a>

> **An orchestrator's job is to decide what happens next and spawn the agent that does it.
> Executing the work itself is a failure of the role, not diligence.**

It prioritises — selects by §7.2's rule, splits what is too large, sequences what depends on
what — and delegates everything else to a briefed agent, including a whole work package to
another orchestrator.

**Its context is the scarcest resource in the system**, being the only one that spans the
whole programme: a subagent's is discarded on return, this one is not. Read conclusions, not
corpora. Encoding what agents return is the one duty it may never delegate (`README.md`, the
encoding rule) — their findings die with their context otherwise.

Operating rules, brief contents and the measurement: `80-agent-operating-procedure.md` §0.

---

## 13. Self-hosting is the destination

Modus will eventually manage its own development. Take every decision as if Modus were
already running this repository. When a design that is convenient for a human operator
conflicts with one that is legible to an agent, choose the agent — then render it for
the human at the edge.
