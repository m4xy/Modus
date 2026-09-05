---
# modus-0039
title: Repository topology — what becomes its own repository, and when
status: todo
type: epic
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0038, modus-0147, modus-0174, modus-0175, modus-0176, modus-0149, modus-0150, modus-0022, modus-0044, modus-0045]
---

# Repository topology — what becomes its own repository, and when

Three cut lines have been proposed and they are not the same line. This bean holds all three
with their measurements, so the next session inherits them rather than re-deriving.

| axis | separates | what it buys |
|---|---|---|
| SDLC vs production | records from code | independent review gates, throughput |
| framework vs tenant (`adr:0006-framework-boundary`) | what every tenant gets from what one domain chose | stops one domain's tooling shipping as product |
| **deployable component** | frontend from backend from store | parallel CI, independent release cadence |

The third is the classic polyrepo question and is orthogonal to the other two.

## The proposal

Separate what is productionised from what is SDLC: one product repository (`modus`) and N
work-store repositories, one per domain (`acme-modus`, and `modus-modus` for this repository's
own domain — the bootstrap case, and the reference implementation of what Modus offers every
tenant).

Not two repositories per domain. A domain has no product code: a Module is a Gradle module
under `modules/` in the product repository, installed *into* a domain
(`doc:10-architecture#module-system`). A domain gets a store.

## What is measured

Half this repository's pull-request traffic is already workflow-only. Of the six merged in
the session that produced this bean, three carried no product content at all — one bean, seven
beans, and four bean closures — and each ran the full Kotlin gate: 153 Gradle tasks, ArchUnit,
the coverage ratchet, branch protection and a review cycle, to change Markdown front-matter.

The coupling that a split would have to break, counted at the time of writing:

| direction | refs | assessment |
|---|---|---|
| `.beans/` → `doc:` / `adr:` / `rule:` | 222 | tolerable — the store pins a product commit, or the reference degrades to a warning |
| `documentation/` → `bean:` | 80 | **the real problem**, and a defect independent of the split |
| `.beans/` → `bean:` | 204 | unaffected; internal to the store |

The 80 downward references are almost all `Enforcement gap:` pointers. A constitution whose
own consistency check requires the current backlog to be present is coupled to transient
state. The split does not create that; it exposes it.

## Do the cheap thing first

Before any repository boundary, make the product gate not run for store-only changes —
`paths-ignore` plus a conditional job in `.github/workflows/ci.yml`, roughly five lines. That
captures most of the measured pain at none of the 302-reference cost, and it answers the
question the split is really asking: is the separation you want about **gates** or about
**repositories**? If the remaining friction after that is about review policy and who may
approve what, the split is justified and you will know precisely why.

## Open questions this bean must answer

1. Do `Enforcement gap:` lines keep naming a bean, or become self-contained — "not enforced;
   owner tracked in the work store"? The second decouples the constitution permanently and is
   worth doing whether or not the split happens.
2. Does the store hold only the Markdown records? `doc:00-constitution` §2.3's append-only
   NDJSON — run events, cost events, audit trail — wants git for nothing and pays a commit per
   append, and concurrent agents appending to one file produce conflicts git cannot resolve.
   The likely answer is that the store repository holds §2.2 Markdown and the event logs sit
   beside it on the filesystem, but that needs deciding rather than assuming.
3. How is the store addressed — configuration (`domains.<id>.store` → path or clone URL) or a
   git submodule? Submodules pin a commit, so every store write would dirty the parent, which
   reintroduces exactly the coupling being removed, and they are worst where the writers are
   many and concurrent. Configuration is the default answer unless something argues otherwise.
4. What is the store's own gate, and who reviews it? This is the question that motivated the
   proposal: a plan or a status change should not be gated by the product build, and should be
   reviewable by someone who cannot approve a production change.

## Why it is blocked on three things

`bean:0038` — until evidence lives in the bean and completed beans are final, the store holds
a mutable record and the product repository holds a second copy.

`bean:0017` — the flat-file adapter is the first thing that reads the store's on-disk format.
Splitting before it exists means hand-maintaining a format nothing enforces, in a second
repository, with no reader.

`bean:0022` — `adr:0005` puts the evidence in the bean, so once the bean is in another
repository a reviewer on GitHub cannot see it. The backoffice rendering the store is what
replaces that, and splitting before it exists moves the evidence away from the only place
anyone reads it.

## The component axis, and the one thing that gates it

Splitting the frontend out is the most obviously attractive of the three: `backoffice/` is a
self-contained Vite application with its own toolchain, its own tests and its own release
cadence, and `adr:0006-framework-boundary` classifies it tier 1 — it is product, not this
domain's tooling.

**The gate is the API contract, and it is hand-written.** `doc:30-code-style` §6 states the
rule already:

> | API types | Generated from the OpenAPI document | Hand-written API types are forbidden — they drift |

`backoffice/src/api/types.ts` carries 22 hand-written declarations, including
`export type DomainId = string`. The rule has been false since the backoffice was written and
carried no `Enforcement gap:`. `bean:0044` closes it.

Why that decides the order: in one repository, contract drift is caught by a person noticing
— `bean:0009` verified `Capability`'s vocabulary against `types.ts` by hand, once, and
recorded it as evidence. Across a repository boundary nothing notices, and the failure is
silent: a renamed field ships green on both sides and breaks in a tenant's browser. A
generated client makes the boundary a versioned artifact, which is what makes a split safe
rather than merely faster. **Generate first, split second.**

## The local layout is free and should be adopted regardless

Treating the working tree as a domain directory — `/domains/{id}/repositories/{…}` — is not a
consequence of splitting. It is the product's own model made concrete on disk: the
Repositories screen already renders the `modus` domain owning `Modus` and `modus-skills`,
each with a default branch and a sync status. `doc:00-constitution` §12 says to take every
decision as if Modus were already running this repository, and this is that decision at
roughly zero cost.

Note that `doc:10-architecture#domain-root-convention` §5.1's route list does **not** include
`/domains/{domainId}/repositories`, though the shipped backoffice serves it. The architecture
document lags both the UI and this proposal.

## Why the CI argument is real but does not settle it

| | duration | source |
|---|---|---|
| `main`, beans/docs only, before `bean:0029` | 47s, 50s | runs `33255099872`, `33247011196` |
| after wiring the backoffice in, before `bean:0045` | 134s | run `33256259515` (`.beans/` only) |
| after `bean:0045`'s per-path split | 67s | run `33261902606` (`.beans/` only) |

All four are run wall clock, `updatedAt` minus `createdAt`. This table originally carried
`~50–53s` and `~2m`, copied from `bean:0045`, with no run id and no unit behind either;
both were wrong and the second was 12s out. Corrected against the run history —
`gh run list --branch main --limit 40 --json databaseId,createdAt,updatedAt,headSha,conclusion`
— by the change that closed `bean:0045` (pull request #35). Twenty-one successful `main`
runs span 47s to 209s, so each cell is a single run matched on change shape, not a mean.

`bean:0029` closed a real hole and made this worse: every Kotlin-only change now runs
`npm ci`, `tsc`, ESLint and Prettier, and CI runs Playwright on top. That cost is genuine —
and it is a **CI topology** problem, which `bean:0045` fixes with per-path jobs at a fraction
of the effort a repository split takes. Try the cheap thing first.

**It has now been tried, and the table above is the result.** Per-path jobs took a
beans-only change from 134s to 67s, a 50% saving. That recovers most of what `bean:0029`
cost and does not reach the 47–50s this repository saw before it.

**What that does and does not settle, stated carefully, because the obvious reading of it is
wrong.** The tempting conclusion is that the topology fix has spent the minutes argument and
a split would only buy the remaining ~17–20s. It does not follow. The 47–50s floor is what a
beans-only change cost *inside this repository* while CI still ran the Kotlin gate against
it. A work-store split does not lower that floor — it removes the change from this
repository altogether. A beans-only change in a split world runs nothing here, and runs only
whatever the work store's own repository runs, which has no Gradle build, no Kotlin
compilation, no ArchUnit and no Playwright.

So on **this shape of change** the split's remaining benefit is close to the whole 67s, not
the difference between 67s and the in-repository floor. The minutes argument for a split is
**largely intact**, and `bean:0045` did not spend it. What `bean:0045` did spend is the
argument that `bean:0029` created an emergency: the 68s regression it added is recovered, so
nothing forces this bean's hand on schedule.

Three things this measurement does not establish, and none should be inferred:

- **What a work-store repository's own CI would cost.** Unmeasured. `docs-lint` and the bean
  graph checks would have to run somewhere, and they are Gradle tasks here. "Seconds" is a
  guess; treat the split's post-state as unknown until something measures it.
- **Anything about the other shapes.** A Kotlin change and a backoffice change are unaffected
  by a work-store split, so this figure speaks only to beans-only changes — which are frequent
  in this repository, and are not most of its CI minutes.
- **That minutes are the right axis at all.** `adr:0005-evidence-lives-in-the-work-item`
  already records a cost on the other side: once the store is a separate repository, a
  reviewer on GitHub cannot read the evidence, and the split therefore depends on `bean:0022`.
  A 67s saving does not outrank that, and this bean should not be decided on seconds alone.

The measurement is n=1 against n=1 (`bean:0045`'s evidence, criterion 4), inside a history
whose successful runs span 47s to 209s. If this bean wants to argue minutes, the way to do it
is more runs, not a better estimate — and the run it most needs does not exist yet, because
it is a run in a repository nobody has created.
