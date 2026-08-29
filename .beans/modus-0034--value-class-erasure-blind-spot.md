---
# modus-0034
title: publishedLanguageIsLeaf is blind to value classes in erased positions
status: todo
type: fix
priority: high
order: AD
created_at: 2026-08-29T00:00:00Z
---

# `publishedLanguageIsLeaf` is blind to value classes in erased positions

`rule:archunit/publishedLanguageIsLeaf` enforces less than it claims, and
`doc:10-architecture#bounded-contexts` §4.2 states the guarantee unconditionally.

A `@JvmInline value class` erases to its underlying type. A published type that holds
another context's identifier in a plain field therefore leaves **no bytecode edge** for
ArchUnit to see: the field is a `java.lang.String`, and the reference the rule exists to
catch is invisible to it.

Found in review of `bean:0032`, not by reading the rule:

```
planted:  domainmgmt/published/Probe.kt —
            class Probe(val owner: uk.m4xy.modus.core.domain.identity.published.ActorId)
          (a plain class, not a data class)
expect:   publishedLanguageIsLeaf rejects the cross-context reference
observed: BUILD SUCCESSFUL — no violation
```

Every identifier in `identity.published` is a value class, so the blind spot covers the
whole of the published-language vocabulary this rule is about.

## Why the existing evidence did not catch it

`bean:0032`'s three planted violations all fired, and all of them fired for the same
incidental reason: they were **data classes**, whose synthesised `equals`/`hashCode`/
`toString` call `ActorId.equals-impl0` and friends, and *those* calls are real bytecode
edges. The rule was observed failing on a case that does not represent the class of defect
it is scoped to. `doc:35-testing#fixture-variation` is the same lesson one level up — a
mechanism verified against one shape proves nothing about the shapes it did not see.

## What holds it up today: nothing

The first draft of this bean said the `event` half was covered, because
`EventsAreDataClasses` (`doc:10-architecture` §4.2) forces every type in `..domain.event..`
to be a data class and a data class does produce the bytecode edge. `docs-lint` refused the
reference: **the rule does not exist.** Checked against the implemented set, §4.2 documents
thirteen package rules and `architecture-tests` implements nine of them —
`EventsAreDataClasses`, `PortsAreInterfaces`, `NoAmbientRandom`, `NoAmbientConcurrency` and
`NoReflection` are absent, and §4.2 carries no `Enforcement gap:` for any of them.

So convention is the only thing keeping every event a data class, and convention is what
`doc:00-constitution#observed-failing` says is not enforcement. Both halves of the published
language are exposed, not one.

## Success criteria

- The rule rejects a cross-context reference held in an erased position. Observed failing
  on the planted case above before the fix, and passing after.
- The fix does not rely on `EventsAreDataClasses` — which would be relying on a rule that
  does not exist. Coupling one rule's soundness to another's scope is what made this
  invisible in the first place.
- `doc:10-architecture` §4.2's `PublishedLanguageIsLeaf` row states what the rule actually
  decides. If some positions remain undecidable from bytecode, the row carries an
  `Enforcement gap:` naming them rather than the present unconditional claim
  (`doc:00-constitution#observed-failing`).
- Consider whether ArchUnit's source-set analysis, a Detekt rule with type resolution, or
  requiring published value objects to be data classes is the right mechanism. The third is
  cheapest and is checkable; it also removes the asymmetry with `..event..`.
