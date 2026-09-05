---
# modus-0134
title: Test doubles are copied between modules because nothing shares them
status: todo
type: refactor
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# Test doubles are copied between modules because nothing shares them

`bean:0066` moved `InProcessDomainEventDispatch` into `adapters/adapter-events-inprocess`
and the end-to-end test with it. The adapter's tests need the same in-memory repositories,
the same recording handlers and the same fixture constants that
`core/core-application/src/test` already holds, so
`adapters/adapter-events-inprocess/src/test/.../EventsAdapterDoubles.kt` is a near-copy of
`ApplicationDoubles.kt` and `ApplicationFixture.kt`.

Two copies of a fixture is two places a defect can hide separately — and the specific defect
these fixtures were shaped against is `bean:0009`'s privilege escalation, which survived 32
tests and 30 verified mutations because every fixture carried exactly one capability. A
second copy is a second chance to get that wrong, in a module whose reviewer may not know
why the first one carries two.

`core-domain`'s `IdentityFixture` and `DomainMgmtFixture` are a third and fourth copy of part
of it, for the same reason.

## Why it was copied rather than shared

A test source set is not published between Gradle modules. The two ways out both cost more
than `bean:0066` could spend on the way past:

- **`java-test-fixtures`**, which publishes a `testFixtures` variant. It is a change to
  `modus.kotlin-base` — no module may declare a test dependency of its own
  (`doc:35-testing#source-sets`) — and the published variant lands on consumers' unit-test
  classpaths, which `assertUnitTestClasspathIsSpringFree` polices as an allowlist
  (`doc:35-testing#unit-classpath`). Whether a fixtures variant is admissible there is the
  question this bean has to answer first.
- **A dedicated `test-fixtures` Gradle module** that production code never depends on. Simpler
  to reason about, but it adds a module to `doc:10-architecture#module-dependencies` §4.1's
  table, whose `ALLOW` rows are exhaustive, and a row to `config/coverage/baseline.tsv`.

Neither is obviously right, which is why this is a bean and not a fix.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | One home for the shared doubles and fixtures, reached by `core-application`, `adapter-events-inprocess` and `core-domain`, with the duplicates deleted | |
| 2 | The unit-test classpath allowlist still holds, observed by planting a Spring type in the shared source set and watching `assertUnitTestClasspathIsSpringFree` reject it | |
| 3 | The `doc:35-testing#fixture-variation` rationale — collections at two-or-more by default — lives in exactly one KDoc afterwards | |
| 4 | `./gradlew qualityCheck` green, coverage baseline moved by `coverageBaselineWrite` rather than by hand | |
