---
# modus-0097
title: sharedKernelIsLeaf is blind to value classes, which is every identifier in the model
status: todo
type: fix
priority: high
order: CB
created_at: 2026-08-30T00:00:00Z
---

# sharedKernelIsLeaf is blind to value classes, which is every identifier in the model

`rule:archunit/sharedKernelIsLeaf` exists because *"every context imports the shared kernel,
so anything it drags behind it is imported unseen"* (`ArchitectureRulesTest.kt`, its
`because` clause). It reads bytecode. A `@JvmInline value class` erases to its underlying
type in every JVM signature, so the kernel can name one and leave no edge for the rule to
find.

Reproduced independently rather than restated (`doc:80-agent-operating-procedure#reports-are-evidence`):

```
planted:  core/core-domain/.../DomainEvent.kt
          import uk.m4xy.modus.core.domain.identity.published.ActorId
          public fun concerns(actor: ActorId): Boolean = false
cmd:      ./gradlew :architecture-tests:test
observed: BUILD SUCCESSFUL in 6s
```

`DomainEvent` is one of the two shared-kernel members. Every context's events implement it.
A parameter naming `identity`'s published language on that interface is the exact dependency
the rule exists to prevent, and the build is green.

## Why this is worse here than where it was found

`bean:0034` found this erasure in `rule:archunit/publishedLanguageIsLeaf` and fixed it with
`PublishedLanguageSourceTest`, which reads source. `bean:0065` then found the
identical blind spot in a new port rule and fixed it the same way. This is the **third**
occurrence, and it is on the surface with the widest blast radius:

- The shared kernel is imported by **every** context, by construction. `adr:0004`'s whole
  argument for promoting `DomainId` was that a per-context copy would give one tenant as many
  unequal types as there are contexts — so the kernel is the one place a dependency is
  guaranteed to reach everywhere.
- **The blind spot covers precisely the types the rule is about.** Every identifier in the
  model is a `@JvmInline value class`: `DomainId`, `ActorId`, `GrantId`, `Capability`,
  `StateName`, `DomainName`. A rule guarding a boundary against identifiers, implemented in
  bytecode, cannot see identifiers.
- `sharedKernelIsLeaf` is also what `bean:0065` leaned on to argue that a port package is not
  the shared kernel. That argument stands — it rests on `SHARED_KERNEL` being a **name set**,
  which is a fact about scoping and not about erasure — but a reader who takes
  `sharedKernelIsLeaf` to be a working leaf gate will over-trust it.

## The pattern this is the third instance of

Recorded because the next instance should be found by looking rather than by review:
**every bytecode rule whose subject is "does type A name type B" is blind wherever B is a
value class**, and this repository has decided that identifiers are value classes.

| rule | subject | status |
|---|---|---|
| `rule:archunit/publishedLanguageIsLeaf` | a published type naming another context | blind; paired with `PublishedLanguageSourceTest` in `bean:0034` |
| `rule:archunit/ambientCapabilityPortsAreLeaf` | a port naming any context | blind; paired with a source rule in `bean:0065` |
| `rule:archunit/sharedKernelIsLeaf` | the kernel naming anything outside stdlib and `java.time` | **blind, unpaired — this bean** |
| `rule:archunit/domainDependsOnNoOuterLayer` | the domain naming an outer layer | not audited; an outer-layer type is unlikely to be a value class, so probably sound |
| `rule:archunit/portsAreInterfaces` | a declaration's own shape | unaffected; `interface` is not erased |

The audit of the last two rows is this bean's second half, and it is why the bean is not
simply "add a source rule".

**That table is an observation, not a status board.** It records what was measured when this
bean was raised, and it is timestamped by nature: nothing later can falsify it, because the
moment it describes does not move. It is deliberately **not** maintained as a live list of
which rules are currently blind. A description of the tree claims currency and so has an
expiry date set by whoever next changes the tree — which is why `doc:20-ddd-practices` §5.1's
package enumeration was deleted rather than corrected. Criterion 5 therefore requires the
**audit to be completed**, never the table to be kept up to date.

## `bean:0034`'s gate is enforced and uncitable — a third state we have no word for

`PublishedLanguageSourceTest`'s test method is a backticked sentence, so there is no `val`
or `fun` of that name and **no `rule:archunit/` reference can resolve to it**. `docs-lint`
check 6 rejected an attempt to cite it while this bean was being written — the mechanism
refused a **true** statement, which is how one learns its vocabulary is too narrow.

The gate is real: observed rejecting a planted violation, running in `qualityCheck`, and
`bean:0034` closed on it. And no document can name it. So:

- `doc:15-repository-layout#core-package-rules` §4.2 gives it a full rule-table row headed
  `PublishedLanguageSourceIsLeaf`, beside rows whose names *are* citable — and that name
  resolves to nothing. Three prose mentions in `documentation/`, zero references.
- An `Enforced by:` line naming it cannot be written, because a typed reference
  (`doc:05-authoring-for-agents#reference-syntax`) is the only legal way to point at a rule
  and check 6 rejects this one.
- `doc:00-constitution#observed-failing` admits an enforcement claim once the mechanism has
  been watched rejecting a violation. This one has been. It still cannot be claimed, for a
  reason unrelated to whether it works.

**That is a third state between "enforced" and `Enforcement gap:` — enforced, observed and
unciteable.** It is worse than an admitted gap in one specific way: a gap names the bean that
closes it, so a reader knows where to look, whereas an unciteable rule reads as enforcement to
a human and as nothing at all to every tool.

### The fix is the rename, not a wider resolver

Different work and different owners, so it is decided here rather than left open.

| candidate | verdict |
|---|---|
| **Rename the test method to `publishedLanguageSourceIsLeaf`** | **Chosen.** One line in `architecture-tests/`. The convention exists and `DefensiveCopySourceTest` documents it, calling its own camelCase name deliberate for exactly this reason; `PublishedLanguageSourceTest` predates it rather than dissenting from it. Owner: `architecture-tests/`, small enough to ride with whichever bean next touches the file. |
| Widen what `rule:archunit/` resolves to | **Rejected.** `rule:archunit/<ident>` names an identifier; a backticked test name is a sentence with spaces, so there is no identifier to name. Widening means inventing a mangling convention between a sentence and a reference, in `tools/docs-lint.sh` (tier 3), to avoid a one-line rename in tier 2 — and it would hide the naming inconsistency rather than fix it. |

Not rejected, because it is not this bean's to decide: check 6 could *report* a rule-shaped
name appearing in a `documentation/` table that resolves to nothing, which is how this went
unnoticed. That is `bean:0027`'s audit territory.


## Scope

Owned: `architecture-tests/`, and this bean.

Not owned: `bean:0027`'s wider `Enforced by:` audit, which this feeds; `bean:0023`'s
context-isolation rules; the `documentation/` rows that describe these rules, which will need
their `Enforced by:` lines revisited once the pairing exists.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | A shared-kernel member naming a value class from any context fails the build. Observed on the plant above — `DomainEvent` with an `ActorId` parameter — before it is claimed (`doc:00-constitution#observed-failing`) | |
| 2 | The mechanism reads **source**, following `PublishedLanguageSourceTest` and `rule:archunit/ambientCapabilityPortSourceIsLeaf`, or states why bytecode suffices here when it did not there | |
| 3 | A control plant that is **not** a value class — an enum, say — is observed failing on the existing bytecode rule, so that erasure is isolated as the reason the value-class plant passed rather than inferred from a single green build. An observation consistent with two mechanisms establishes neither (`bean:0065`) | |
| 4 | The scan's **input surface** is asserted separately from its verdict: a test states what the analyser read out of the kernel sources, so a regex that stops matching fails loudly instead of returning a confident empty verdict | |
| 5 | The two unaudited rows in the table above are resolved — each either shown sound or paired — so the audit is complete rather than stopped at the first fix | |
| 6 | Whatever `documentation/` says these rules enforce is reconciled with what they do, or an `Enforcement gap:` is recorded naming this bean | |
| 7 | `./gradlew qualityCheck` green | |

## Sequencing

Nothing blocks this. It blocks nothing mechanically, but it is `priority: high` because
`bean:0023`'s context-isolation rules will be written against the same boundary and would
inherit the same blind spot if this is not fixed first. The cheapest moment to fix a rule
family is before there are more of them.
