---
# modus-0092
title: Nine ArchUnit rules depend on the absence of an archunit.properties file
status: todo
type: fix
priority: high
order: CA
created_at: 2026-08-29T00:00:00Z
---

# Nine ArchUnit rules depend on the absence of an archunit.properties file

`ArchitectureRulesTest` holds nine `noClasses()` rules. Seven of them narrow their scope with
a `that()` clause, and a `that()` clause that matches nothing leaves the rule with no classes
to check — the vacuity `doc:00-constitution#observed-failing` was written about, and the shape
that has already shipped green-and-blind here five times.

What currently stops that is **an ArchUnit library default**, not anything this repository
states. Observed while building `rule:archunit/ambientCapabilityPortsAreLeaf`
(`bean:0065`), by scoping a rule at a package that does not exist:

```
cmd:      ./gradlew :architecture-tests:test --tests '*ArchitectureRulesTest*'
observed: Rule '…com.modus.core.domain.port..…' failed to check any classes. This means
          either that no classes have been passed to the rule at all, or that no classes
          passed to the rule matched the `that()` clause. To allow rules being evaluated
          without checking any classes you can either use `ArchRule.allowEmptyShould(true)`
          on a single rule or set the configuration property
          `archRule.failOnEmptyShould = false` to change the behavior globally.
```

The message names the switch. **`archRule.failOnEmptyShould = false`, set once in an
`archunit.properties` on the test classpath, would turn every one of those seven rules into a
rule that passes when its scope matches nothing — globally, silently, and with no test
failing to say so.** No such file exists today (`find . -name 'archunit*.properties'` returns
nothing), which is precisely why this is worth recording now rather than after someone adds
one: the repository depends on the *absence* of a file, and an absence is not a decision
anybody made or can see.

## The nine rules, and which are exposed

Exposure follows the `that()` clause: a rule with no `that()` quantifies over every imported
class, so its input is empty only if the whole import failed — which
`rule:archunit/everyModuleIsOnTheAnalysedClasspath` already catches.

| rule | `that()` clause | exposed |
|---|---|---|
| `rule:archunit/domainIsFrameworkFree` | `resideInAPackage(DOMAIN)` | yes |
| `rule:archunit/domainDependsOnNoOuterLayer` | `resideInAPackage(DOMAIN)` | yes |
| `rule:archunit/applicationDependsOnDomainOnly` | `resideInAPackage(APPLICATION)` | yes |
| `rule:archunit/applicationIsFreeOfDeliveryConcerns` | `resideInAPackage(APPLICATION)` | yes |
| `rule:archunit/adaptersDoNotDependOnModulesOrApp` | `resideInAPackage(ADAPTERS)` | yes |
| `rule:archunit/modulesDoNotDependOnAdaptersOrApp` | `resideInAPackage(MODULES)` | yes |
| `rule:archunit/nothingDependsOnTheApp` | `resideOutsideOfPackage(APP)` | yes |
| `rule:archunit/nothingWritesToTheStandardStreams` | none | no |
| `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` | none | no |

Seven exposed. Note what the seven are: they are the **layering rules** —
`doc:00-constitution#layering` §1.1's entire mechanical expression. A one-line properties file
would disable the enforcement of the constitution's first section without touching a single
rule, and the build would stay green.

`rule:archunit/ambientCapabilityPortsAreLeaf` is a `classes()` rule rather than a
`noClasses()` one and is exposed to the same switch. It is not counted above because
`bean:0065` already pairs it with
`rule:archunit/everyAmbientCapabilityPortIsSeenByItsOwnRule`, which asserts the perceived set
directly and does not consult the property. That pairing is the shape this bean generalises.

## Recommendation, with the confidence stated

**Assert the effective value at runtime; do not assert the file's absence.** Stated as a
recommendation rather than a conclusion, because it has not been built and
`doc:00-constitution#observed-failing` makes an unbuilt mechanism a claim.

| candidate | assessment |
|---|---|
| **Assert the effective configuration**, reading ArchUnit's own resolved value and failing if it is not `true` | **Recommended.** It is the only option that covers every route to the value — a properties file, a system property, and a future change to the library default. It states the dependency positively, which is what `doc:00-constitution#observed-failing` asks of a claim. Confidence: high on the shape; **not verified** that ArchUnit exposes the resolved value on a supported API surface. That is the first thing the implementer must check, and if it does not, this row is wrong and the next one wins. |
| **Pin it explicitly** in an `archunit.properties` carrying `archRule.failOnEmptyShould=true` | **Recommended alongside, not instead.** It makes the dependency visible where someone would look for it, and turns a silent default into a reviewable line. It does not *detect* anything: a later edit to that same file is exactly the change this bean is about. Good documentation, not a gate. |
| **Assert no `archunit.properties` exists** | **Rejected.** It forbids the file rather than the setting, so it fails on a file that sets something harmless and passes on a system property that sets this one. It also makes the pinning option above illegal. |

A companion perception assertion per exposed rule — the `bean:0065` shape, asserting each
rule's scope selects a non-empty set — would be strictly stronger than any of the above,
because it does not depend on the property at all. It is also seven more assertions to write
and maintain. Whether that is worth it is a judgement this bean does not make; it is recorded
so the implementer chooses deliberately rather than defaulting.

## Scope

Owned: the mechanism chosen above, in `architecture-tests/`, and this bean.

Not owned: `bean:0027`'s audit of `Enforced by:` claims, which this feeds but does not close;
the five previously recorded vacuity instances in `doc:00-constitution#observed-failing`'s
table, which are history and not this bean's to edit.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Setting `archRule.failOnEmptyShould=false` no longer makes an empty-scoped rule pass in silence: the build fails, naming the property. Observed by planting the properties file **and** a mis-scoped rule together, then reverting both | |
| 2 | The mechanism is verified against the route it does not obviously cover: a **system property** rather than a file, since ArchUnit resolves both | |
| 3 | The chosen mechanism does not itself depend on the property it guards — a guard switched off by the same switch guards nothing | |
| 4 | Whichever mechanism is chosen, the seven exposed rules are named in one place and that list is asserted against the rules actually present, so a rule added later is not silently outside the guarantee | |
| 5 | If the runtime-configuration route proves unavailable on a supported ArchUnit API, that is recorded in this bean as a finding with the API surface examined, and the fallback is taken deliberately rather than by default | |
| 6 | `./gradlew qualityCheck` green | |

## Sequencing

Nothing blocks this and it blocks nothing. It is `priority: high` because the exposure is to
`doc:00-constitution#layering` §1.1's whole mechanical expression, and because the cost of
recording it is small while the cost of discovering it after someone adds the file is a green
build that enforces seven fewer rules than it claims.
