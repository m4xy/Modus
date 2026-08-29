---
id: adr:0006-framework-boundary
title: The framework boundary, and where a domain's own tooling lives
status: active
superseded_by: null
read_when:
  - path: documentation/**
  - path: modules/**
  - path: tools/**
  - task: framework|tenant|third-party|boundary|which tier|pollut|reusable|extension point
provides:
  - adr:0006-framework-boundary#the-three-tiers
  - adr:0006-framework-boundary#the-test
  - adr:0006-framework-boundary#classification
  - adr:0006-framework-boundary#domain-tooling
depends_on: [doc:00-constitution, doc:10-architecture, doc:20-ddd-practices]
---

# ADR 0006 — The framework boundary, and where a domain's own tooling lives

- **Date:** 2026-08-29
- **Deciders:** Modus core
- **Supersedes:** —

## Context

Modus is a framework other domains adopt, and it is also itself a domain — the one that
develops Modus. Those two roles currently share one undifferentiated repository, and nothing
marks which artifact belongs to which.

Measured: every one of the twelve documents in `documentation/` addresses someone developing
Modus *in this repository*. The phrase "this repository" appears 18 times. There is no
document addressed to someone using Modus, and no marker distinguishing the two audiences.

```
cmd:      grep -ro "this repository" documentation/ | wc -l
observed: 18

cmd:      for f in documentation/*.md; do sed -n '/^# /,$p' "$f" | sed -n '3p'; done
observed: "Read this before writing anything in core/", "Read this before adding a module,
          a class, a package, or an endpoint", "If you are an agent working in this
          repository, this is your loop", … — every one, developer-facing
```

The risk this creates is not hypothetical. `tools/docs-lint.sh` is 306 lines of policy about
how *this* domain writes documents. `.beans/`, `AGENTS.md`'s work-item selection rule and
`.github/pull_request_template.md` are this domain's workflow. All sit in the framework's
tree with nothing saying they are not part of what a tenant adopts.

The sharpest instance: **`doc:00-constitution` is the `modus` domain's process definition,
written as prose.** Its own §8 requires every domain to define its own work-item states, its
own definition of done, its own required evidence kinds and its own model and effort policy,
and forbids code from hardcoding a single process. The constitution is precisely that content
for precisely one domain. `bean:0030` has since built `ProcessDefinition` as the *data* form
of the same thing, so this domain's process is on course to exist twice.

## Decision

### Three tiers, not two <a id="the-three-tiers"></a>

| tier | what it is | who receives it |
|---|---|---|
| **1 — framework** | the running system and its extension contract: `core/`, `adapters/`, `modules/`, `app/`, and the documents a third-party Module author must obey | every tenant |
| **2 — framework build discipline** | how Modus's own source is written and gated: `build-logic/`, `architecture-tests/`, `config/`, and the style, DDD and testing documents | nobody outside this repository |
| **3 — the `modus` domain's SDLC** | how one domain chose to work: `.beans/`, `tools/`, `AGENTS.md`, the pull-request template, the constitution, the agent operating procedure | one domain, which happens to be this one |

Two tiers would not have been enough. Tier 2 is neither shipped nor domain-specific — it
governs a repository, not a product and not a process — and collapsing it into either makes
one of the other two wrong.

### The test <a id="the-test"></a>

> **A thing is tier 1 if a tenant's running system still needs it once Modus's own repository
> is deleted.**
>
> **A thing is tier 3 if another domain adopting Modus would plausibly want it different.**
>
> Everything else is tier 2.

Both are decidable without judgement about intent, which is what makes them usable on a file
nobody anticipated. A new artifact carries its tier or it is misfiled by default.

### Classification, including the debatable rows <a id="classification"></a>

| artifact | tier | why |
|---|---|---|
| `core/`, `adapters/`, `modules/`, `app/` | 1 | the running system |
| `doc:10-architecture` §3.1, §4.1, §5, §7 | 1 | the extension contract: published-language rules, what a `modules/*` may depend on, the `/domains/{domainId}` route convention, and the Module system itself |
| `doc:10-architecture` §2, §4.2, §4.3, §6, §8 | 2 | this repository's tree, the package rules inside `core-domain`, adapter rules, testing architecture |
| `doc:20-ddd-practices`, `doc:30-code-style`, `doc:35-testing` | 2 | **not** tier 1 — see below |
| `doc:40-durability` | 1 | the on-disk format is what a tenant's data is stored in |
| `doc:50-memory-and-evidence` | 1 | the memory model is a shipped bounded context (`bean:0015`); its authoring conventions are tier 3 and are extracted |
| `doc:60-cost-model`, `doc:70-skills` | 1 | both describe shipped product behaviour |
| `doc:00-constitution`, `doc:05-authoring-for-agents`, `doc:80-agent-operating-procedure` | 3 | this domain's process, authoring standard and agent loop |
| `build-logic/`, `architecture-tests/`, `config/` | 2 | how this source is built and gated |
| `.beans/`, `tools/`, `AGENTS.md`, the pull-request template | 3 | this domain's work store and workflow |

**`doc:20`, `doc:30` and `doc:35` are tier 2, not tier 1**, and this is the row most likely
to be argued. The reasoning is `doc:20-ddd-practices` §1's own layer table: a third-party
Module is a `modules/*`, which the table places in the **infrastructure** layer and says
"never contains business rules". A Module author therefore never writes an aggregate, a value
object or a domain event — those live in `core/core-domain`, which is Modus's own. The DDD
practices are read before writing anything in `core/`, so they cannot bind someone who by
construction never writes there. The same holds for the ktlint/Detekt configuration and the
unit/integration split, both of which are enforced by convention plugins a third-party build
does not apply.

**`doc:10-architecture` is two documents.** Rather than split a 464-line file in this
decision, the sections are classified above and the split is deferred to the work item that
writes the tier-1 surface, which is where the seam becomes concrete rather than notional.

### A domain's own tooling is a Module <a id="domain-tooling"></a>

The non-pollution property is already specified and is not a repository boundary.
`doc:10-architecture#module-system` §7.3: a Module "does nothing at all in a domain where it
is not installed. A module MUST NOT register global state, global routes, or global beans
that are observable from an uninstalled domain."

So tier-3 tooling becomes a Module installed into the domain that wants it, not a script in
the framework's `tools/`. `docs-lint` is the `modus` domain's document policy and belongs to
the `modus` domain, in the same way another tenant's bespoke check would belong to theirs.

**The gap this leaves, stated rather than glossed:** `docs-lint` runs at build time in CI and
a Module is a runtime concern, so the Module boundary does not reach it today. Under
`doc:00-constitution` §12 the gap closes — a self-hosting Modus runs the `modus` domain's
checks as part of that domain's process — but until then tier-3 build-time tooling lives in
`tools/` and is marked tier 3 rather than mechanically isolated.

## Consequences

### Positive

- The pollution risk becomes decidable rather than a matter of taste, on artifacts nobody has
  written yet.
- It gives the work-store split (`bean:0039`) its cut line. Splitting along code-versus-records
  alone would leave the constitution in the framework repository and this domain's process
  still framework-shaped.
- It names the missing deliverable: there is no tier-1 documentation, so Modus currently has
  no documented extension surface at all.

### Negative

- Three tiers is more structure than a young repository needs, and every new file now carries
  a classification question. The test exists to make that question cheap, not to remove it.
- `doc:10-architecture` and `doc:50-memory-and-evidence` are each classified at section
  granularity, so until they are split the tier is a property of a passage rather than a file.
  That is weaker than a per-file rule and cannot be checked mechanically yet.

### Neutral

- `modules/module-beans` (the product's per-domain work tracking) and `.beans/` (this domain's
  store, which happens to use that schema) remain the same word for two things. Renaming was
  considered and declined: the module's name is accurate — it implements the `hmans/beans`
  schema — and the collision resolves itself when the store becomes `modus-modus`. Recorded so
  the next reader does not have to re-derive it.

## Alternatives considered

| alternative | rejected because |
|---|---|
| Two tiers — framework and everything else | build discipline is neither shipped nor domain-specific. Folding it into tier 1 tells a tenant to adopt Modus's Detekt configuration; folding it into tier 3 implies another domain might want Modus's own ArchUnit rules different, which is not a thing they can have |
| Enforce the boundary by repository split alone | a split along code-versus-records leaves the constitution in the framework repository. The boundary has to be drawn before the split can be cut along it, and drawing it may show the split is unnecessary |
| Leave it until a second tenant exists | the cost is paid on every document written before then, and the artifacts most likely to be misfiled — a tenant-facing guide, a bespoke check — are exactly the ones a second tenant would arrive needing |
