---
# modus-0095
title: Documented package expressions that match no package this repository has
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Documented package expressions that match no package this repository has

`bean:0068`'s base branch corrected every package expression in `doc:20-ddd-practices` §5.1
and `doc:10-architecture` §3, §3.1 and §4.2: the root was `com.modus`, which this repository
has never used, and the context segment sat before `domain` rather than after it. Two of the
corrected occurrences were `package` declarations inside code fences — a copy-paste source in
the most-copied snippet in the package.

Three occurrences of the same class are out of that branch's reach. Each is stated with what
was checked.

## 1. `doc:15-repository-layout` §4.2 renders three rule scopes as globs that match nothing

`doc:15-repository-layout#core-package-rules` writes each rule's scope in ArchUnit's own
package-matcher syntax. Three of them use a shape that cannot match the live tree, and a
fourth row in the same table uses the shape that can:

| line | rule | documented scope | matches a live package? |
|---|---|---|---|
| 115 | `AggregatesAreSealedOrFinal` | `..domain.aggregate..` | **no** |
| 117 | `PublishedLanguageIsLeaf` | `..domain.event..`, `..domain.published..` | **no** |
| 120 | `PortsAreInterfaces` | `..domain.port..` | **no** |
| `adr:0004`:85 | `AggregatesAreSealedOrFinal`, quoted | `..domain.aggregate` | **no** |
| 118 | `PublishedLanguageSourceIsLeaf` | `..domain.<ctx>.published..` | yes |

`..` matches any number of packages, so `..domain.aggregate..` requires `domain` and
`aggregate` to be consecutive. Every live package puts the context between them:

```
cmd:      grep -rh "^package " core/ | sort -u | grep aggregate
observed: package uk.m4xy.modus.core.domain.domainmgmt.aggregate
          package uk.m4xy.modus.core.domain.identity.aggregate
exit:     0

cmd:      sed -n '309,311p' architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt
observed: private const val PUBLISHED_LANGUAGE = "$DOMAIN_ROOT.*.published.."
          private const val DOMAIN_EVENTS = "$DOMAIN_ROOT.*.event.."
          private const val AGGREGATES = "$DOMAIN_ROOT.*.aggregate.."
exit:     0
```

The implementations are correct — they use `*` for the context segment. **Two of the three
mis-rendered rules are implemented and passing**, so the defect is in the document alone
today, and nothing is red. Its cost is the next rule: an agent implementing
`PortsAreInterfaces` from row 120 writes `..domain.port..`, the rule matches no class, and a
`noClasses(...)` assertion over an empty set passes. `rule:archunit/everyUnitTestPackageIsAnalysed`
exists because that failure mode has happened here before
(`doc:35-testing#purity-rules`). That row 118 already writes `<ctx>` explicitly is what
makes this a slip rather than a convention.

`documentation/adr/0004-domain-id-shared-kernel.md:85` carries the same glob outside
`doc:15-repository-layout`, and cites `doc:20-ddd-practices` §5.1 for it:

```
cmd:      sed -n '84,86p' documentation/adr/0004-domain-id-shared-kernel.md
observed: things get added quietly. At three, move them to `..domain.kernel` and scope both
          rules structurally, the way `..domain.aggregate` scopes `AggregatesAreSealedOrFinal`
          (`doc:20-ddd-practices` §5.1).
exit:     0
```

`..domain.kernel` has the same defect prospectively: it is the shape an implementer would
write for a package that does not exist yet, and it would match nothing when it did. This
matters more than the other rows because it is the **instruction** for what to do on the
kernel's third member, so the glob is not describing a rule but prescribing one. An ADR is
amendable in principle, unlike a completed bean, so the instrument here is not the obstacle.

## 2. `.beans/modus-0009` names a `com.modus.*` package and cannot be edited

`.beans/modus-0009--identity-bounded-context.md:350` reads "a `com.modus.*` package that does
not either; both corrected". It is `status: completed`, so it may gain `## Amendments`
entries and change in no other way (`adr:0005-evidence-lives-in-the-work-item#finalisation`,
`docs-lint` check 11). The sentence is a record of what was corrected at the time and is not
a live instruction, so an amendment may be the wrong instrument; deciding that is this bean's
work, not `bean:0068`'s.

## 3. The general form

A package expression in a document is a claim about the tree, and no check reads it. Every
instance above sat in a document for as long as it existed, in sections whose own subject is
that placement decides which rules apply — `doc:20-ddd-practices` §5.1 opens "a type in the
wrong package silently removes it from a rule".

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Every package expression in `documentation/**` names a package shape the tree has, or is marked as an intended shape for packages with no members | command |
| 2 | The three `doc:15-repository-layout` §4.2 scopes match the constants the implementations use, checked against `ArchitectureRulesTest` rather than against each other | citation |
| 3 | A decision is recorded on `.beans/modus-0009`: amended, or left with the reason | citation |
| 4 | Whether a mechanical check can compare a documented package expression against the tree is answered either way, and if not, this is stated as a review obligation rather than left implied (`doc:00-constitution#observed-failing`) | citation |
