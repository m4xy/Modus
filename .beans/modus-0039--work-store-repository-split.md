---
# modus-0039
title: Decide whether the work store becomes its own repository
status: todo
type: epic
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0038, modus-0017, modus-0022]
---

# Decide whether the work store becomes its own repository

An open question with the analysis recorded, not a decision already taken. It exists so the
next session inherits the measurements rather than re-deriving them.

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
