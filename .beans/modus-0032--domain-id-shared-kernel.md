---
# modus-0032
title: Make DomainId shared kernel
status: completed
type: refactor
priority: high
order: AC
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T12:55:15Z
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

## Review cycle

Reviewed against the branch, every claim re-run rather than read. Nothing blocking; three
findings changed the code or the record.

| finding | resolution |
|---|---|
| membership by `name.substringBefore('$')` is textual and therefore forgeable — a top-level `` `DomainId$Evil` `` joined the kernel, build green | walked structurally via `JavaClass.getEnclosingClass()`. Re-planted the same forgery: 7 violations where there were none |
| `publishedLanguageIsLeaf` never sees a value class in an erased position, so criterion 4 asserted more than the rule delivers | `bean:0034` raised; §4.2's row carries the gap. Pre-existing, not introduced here — and the three original plants all fired incidentally, via `data class` synthetics |
| the ADR rejected "relax the leaf rule" on two grounds, one of which does not hold, and omitted the strongest alternative | the "weakens a ratified rule" ground is struck — §3.1's two statements contradict, so declining to weaken one does not choose between them. The omitted alternative (`DomainId` is `domainmgmt`'s, leaf rule consults the allowlist) is added, and `#deferred-conflict` says plainly that `bean:0023` settles what this ADR only defers |

Also corrected from review: `contextOf` returned `"uk"` for a kernel member and reached the
right answer by accident; it now returns an explicit `NO_CONTEXT`. `sharedKernelIsLeaf`
described a permitted set belonging to a different rule.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `DomainId` resides outside every bounded context, and no context declares one | `grep`, and the compiled package |
| 2 | Its invariant, its failure message and its KDoc reasoning survive the move unaltered, and its three tests move with it | diff of the extracted text; `git diff` on both files |
| 3 | `rule:archunit/publishedLanguageIsLeaf` exempts the shared kernel by **name**, and membership is walked structurally to the outermost enclosing class, so the exemption cannot silently widen | test-run: plant a forged member, observe rejection. Review disproved the first attempt — a textual split on `$` admitted a top-level `` `DomainId$Evil` `` with the build green |
| 4 | A published type in one context still may not reach another context's published package | test-run: plant, observe the rule fail, revert |
| 5 | The shared kernel itself is leaf-checked — it may not grow a dependency the contexts importing it cannot see | test-run: plant, observe, revert |
| 6 | `identity`'s 43 tests pass unchanged in substance; only imports move | test-run |
| 7 | §3.1's leaf paragraph and §4.2's `PublishedLanguageIsLeaf` row name both exemptions, and neither is restated anywhere else | `grep`, `docs-lint` |
| 8 | `./gradlew qualityCheck` green, `config/coverage/baseline.tsv` moved by exactly the move | test-run |
| 9 | The learning that made the rule fail twice is encoded where the next agent writing an ArchUnit rule will read it, not only in the ADR | `doc:30-code-style#archunit-synthetic-classes` |
| 10 | The regression provenance `coverageBaselineWrite` erased is restored, and the erasure itself is raised rather than absorbed | `bean:0033`, and the restored comment in `config/coverage/baseline.tsv` |
| 11 | Where the rule enforces less than §4.2 claims, the document says so rather than the bean quietly relying on it | `bean:0034` (value-class erasure), and §4.2's new `Enforcement gap:` naming the five rules of the thirteen that do not exist |
| 12 | The ADR states which question it settles and which it defers | `adr:0004-domain-id-shared-kernel#deferred-conflict` |

## Summary of Changes

Merged as PR #13 (`b25136f`). `DomainId` is shared kernel beside `DomainEvent`; both §3.1
rules survive unchanged. `sharedKernelIsLeaf` is new and holds the kernel to the standard it
lets others rely on — closing a hole older than this bean, since `DomainEvent` was exempt
from the leaf rule and checked by nothing.

Review disproved criterion 3 as first written. Membership by `name.substringBefore('$')` is
textual and forgeable — Kotlin permits `$` in a backticked type name, so a top-level
`` `DomainId$Evil` `` joined the kernel with the build green. It is now walked structurally
through `JavaClass.getEnclosingClass()`; re-planting the forgery produces seven violations.
Review also found `publishedLanguageIsLeaf` blind to value classes in erased positions
(`bean:0034`), and writing that bean surfaced that five of §4.2's thirteen rules do not exist
at all — now an `Enforcement gap:` naming `bean:0027`.
