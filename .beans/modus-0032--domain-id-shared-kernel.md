---
# modus-0032
title: Make DomainId shared kernel
status: in-progress
type: refactor
priority: high
order: AC
created_at: 2026-08-29T00:00:00Z
---

# Make DomainId shared kernel

`doc:10-architecture#bounded-contexts` §3.1 states two rules that cannot both hold once a
second context exists.

| # | rule | says |
|---|---|---|
| 1 | the allowlist table, also §3's `Consumes` column | `domainmgmt` MAY import the published language of `identity` |
| 2 | the leaf paragraph, implemented as `rule:archunit/publishedLanguageIsLeaf` | a type in `..domain.event..` or `..domain.published..` may reference only the Kotlin stdlib, `java.time`, and **its own context's** `..domain.published..` |

`domainmgmt`'s events carry a `DomainId`. `DomainId` lives in `identity.published`. Rule 1
permits it; rule 2 forbids it. Observed, not inferred — the minimal case planted while starting `bean:0012` and reverted:

```
cmd:      ./gradlew :architecture-tests:test
planted:  domainmgmt/event/Probe.kt — data class Probe(val domainId: identity.published.DomainId, ...)
observed: ArchitectureRulesTest > publishedLanguageIsLeaf FAILED
          Method <uk.m4xy.modus.core.domain.domainmgmt.event.Probe.equals(java.lang.Object)>
            calls method <uk.m4xy.modus.core.domain.identity.published.DomainId.equals-impl0(
            java.lang.String, java.lang.String)> in (Probe.kt:9)
          23 tests completed, 1 failed
```

The rule has been passing vacuously since it was written: `identity` is the only modelled
context and it imports nobody, so the set it quantifies over has been empty. Same vacuity
`bean:0023` was raised for, reached one bean earlier than expected.

## Decision

`DomainId` is shared kernel, beside the `DomainEvent` marker. `adr:0004-domain-id-shared-kernel`
carries the reasoning and the two rejected alternatives. Both §3.1 rules survive unchanged,
which is why this was preferred to relaxing rule 2.

It was never `identity`'s concept. Its own KDoc says so — *"the tenant identifier that scopes
every other bounded context"* — and it sits in `identity.published` only because `identity`
was the first context modelled and needed somewhere to put it.

## Scope

Owned: `core/core-domain/.../domain/DomainId.kt` (new), `identity/published/Identifiers.kt`,
every `identity` import of `DomainId`, `identity/README.md`,
`architecture-tests/.../ArchitectureRulesTest.kt`, `documentation/10-architecture.md` §3.1
and §4.2, `documentation/20-ddd-practices.md` §3.2 and §5.1,
`documentation/30-code-style.md` §5, `documentation/adr/0004-*`,
`config/coverage/baseline.tsv`, this bean and `bean:0033`.

`doc:30-code-style` §5 is in scope under the encoding rule (`doc:README#encoding-rule`),
not by drive-by: `sharedKernelIsLeaf` failed twice on classes Kotlin generates and the
source never names, and §5 is where a rule about writing ArchUnit rules belongs. One
adjacent falsehood was corrected in the same sentence rather than left standing — §5 placed
the ArchUnit tests in a `build-logic` convention plugin named `modus.archunit`, which does
not exist; they are in the `architecture-tests` module.

Not owned: `domainmgmt/**` and every other context — `bean:0012` is the first consumer and
does not land here. No behaviour changes; `DomainId`'s invariant, message and tests are
moved unaltered.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `DomainId` resides outside every bounded context, and no context declares one | `grep`, and the compiled package |
| 2 | Its invariant, its failure message and its KDoc reasoning survive the move unaltered, and its three tests move with it | diff of the extracted text; `git diff` on both files |
| 3 | `rule:archunit/publishedLanguageIsLeaf` exempts the shared kernel by **name**, not by package wildcard, so the exemption cannot silently widen | citation |
| 4 | A published type in one context still may not reach another context's published package | test-run: plant, observe the rule fail, revert |
| 5 | The shared kernel itself is leaf-checked — it may not grow a dependency the contexts importing it cannot see | test-run: plant, observe, revert |
| 6 | `identity`'s 43 tests pass unchanged in substance; only imports move | test-run |
| 7 | §3.1's leaf paragraph and §4.2's `PublishedLanguageIsLeaf` row name both exemptions, and neither is restated anywhere else | `grep`, `docs-lint` |
| 8 | `./gradlew qualityCheck` green, `config/coverage/baseline.tsv` moved by exactly the move | test-run |
| 9 | The learning that made the rule fail twice is encoded where the next agent writing an ArchUnit rule will read it, not only in the ADR | `doc:30-code-style#archunit-synthetic-classes` |
| 10 | The regression provenance `coverageBaselineWrite` erased is restored, and the erasure itself is raised rather than absorbed | `bean:0033`, and the restored comment in `config/coverage/baseline.tsv` |
