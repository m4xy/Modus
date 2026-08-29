---
# modus-0065
title: The Clock and IdGenerator ports
status: todo
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
---

# The Clock and IdGenerator ports

`doc:00-constitution` §1.3 forbids `Instant.now()` and `UUID.randomUUID()` inside
`core-domain` and names the replacement in the same sentence — "time enters through a
`Clock` port passed as a constructor argument", "identifiers come through an `IdGenerator`
port". `doc:20-ddd-practices` §5.3 restates it as a table of ports that "exist and are
injected".

**Neither port exists.** `rg` over the whole tree finds no `Clock`, no `IdGenerator`, no
`ClockPort`, no `IdGeneratorPort` and no `RandomPort` in any source file; every hit is in
`documentation/`. The two ports that do exist in `core-domain` are `ActorRepository`,
`PermissionGrantRepository` and `DomainRepository` — none of them ambient-capability ports.

The two aggregates written so far hide the gap rather than close it. `Domain.adoptProcess`,
`PermissionGrant.issue` and `Actor.register` each take `at: Instant` as a caller-supplied
parameter, and every identifier arrives already constructed. That works only because
nothing above the domain has yet had to *produce* an instant or an id: `core-application`
holds two files, `ListBoundedContexts` and `UseCase`, and no adapter implements any port.
The first aggregate that must mint its own is unimplementable today.

## The enforcement picture is weaker than `doc:15-repository-layout` §4.2 reads

Verified against the source rather than the table, because this bean's premise is that the
rules push code towards ports that are not there:

| documented rule | what is actually implemented |
|---|---|
| `NoAmbientTime` | Does not exist under that name. `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` (`architecture-tests/.../ArchitectureRulesTest.kt:258`) is the real rule. It is a `noClasses()` assertion — repository-wide, not scoped to `core-domain` — and it bans **only** `Instant.now`, `LocalDate.now` and `LocalDateTime.now`. `Clock.systemUTC`, `System.currentTimeMillis` and `System.nanoTime` are named in §4.2 and caught by nothing. |
| `NoAmbientRandom` | Does not exist at all. §4.2's own `Enforcement gap:` paragraph says so: "a call to `UUID.randomUUID()` in the domain merges green". |

So the ban on ambient time is real but narrower than documented, and the ban on ambient
randomness is a claim (`doc:00-constitution#observed-failing`). Either way the alternative
the rules exist to push code towards is missing, which is what this bean supplies. Closing
the rule gaps themselves is `bean:0027`'s audit, not this bean.

## Design constraints this bean must resolve, not discover in review

1. **Naming.** `doc:00-constitution` §1.3 says `Clock` and `IdGenerator`; `doc:20-ddd-practices`
   §5.3 says `ClockPort`, `IdGeneratorPort`, `RandomPort`. `doc:00-constitution` states its own
   precedence over every other file in the package, so the unsuffixed names win and §5.3 is the
   bug. Recorded here rather than fixed here: `documentation/` is not in this bean's scope, and
   the correction is a one-row edit somebody must make deliberately.
2. **`Clock` shadows `java.time.Clock`.** Any file that wants both must import one and qualify
   the other. This is tolerable — `Instant.now(clock)` against the wrong `Clock` does not
   compile — but it is the strongest argument the `ClockPort` spelling has, and the decision
   above overrules it on precedence, not on taste.
3. **`IdGenerator` cannot return a context's identifier type.** `ActorId`, `GrantId` and every
   identifier like them live in a context's `..published..` package, which
   `doc:10-architecture#bounded-contexts` §3.1 makes a leaf, and `rule:archunit/sharedKernelIsLeaf`
   holds the shared kernel to the Kotlin stdlib and `java.time`. A cross-context port returning
   `ActorId` would either drag `identity`'s published package into every context or force one
   `IdGenerator` per context. It therefore returns the raw `String` the value classes wrap, and
   each context wraps it — `ActorId(ids.newId())`.
4. **`RandomPort` is deliberately not in scope.** Nothing in the tree needs randomness that is
   not an identifier. Adding a port with no caller is how an unused abstraction gets frozen into
   the kernel.

## Scope

Owned: `core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/port/`,
`core/core-domain/src/test/kotlin/uk/m4xy/modus/core/domain/port/`,
`config/coverage/baseline.tsv`, this bean.

Not owned: `documentation/20-ddd-practices.md` §5.3's naming row (finding 1 above);
`bean:0027`'s audit of the two rule names; retro-fitting the ports into `Domain`,
`PermissionGrant` and `Actor`, which take a caller-supplied `Instant` today and are correct
as they stand — a port is for an aggregate that must mint a value, not for one handed one.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | `Clock` is an interface in `core-domain` whose only member returns a `java.time.Instant`, referencing nothing beyond the Kotlin stdlib and `java.time` | |
| 2 | `IdGenerator` is an interface in `core-domain` returning the raw `String` a context's identifier value class wraps, so no context's published package is reachable from it | |
| 3 | Both live in a context-free package under `core-domain`, placed by `doc:15-repository-layout#placement-table` §2.1's "outbound port interface (repository, clock, id generator, agent launcher)" row, and the placement breaks no existing ArchUnit rule — in particular `rule:archunit/sharedKernelIsLeaf`, `rule:archunit/thereAreNoPackageCycles` and `rule:archunit/publishedLanguageIsLeaf` | |
| 4 | A test double for each port lives where `doc:35-testing` puts it, is hand-written rather than mocked (`doc:30-code-style` §7 forbids mocks in `core/`), and is deterministic: the clock returns a fixed instant, the generator a stated sequence | |
| 5 | The doubles' own behaviour is asserted, not merely used: the id generator is observed returning **different** values on successive calls and values that satisfy the identifier invariant, and the clock is observed returning the instant it was constructed with | |
| 6 | The input surface is tested separately from the verdict: a test asserts what the double is *given* distinct from what a caller *concludes* from it, so a fixture that hands the double a well-formed value cannot stand in for testing the code that builds one (`doc:35-testing#fixture-variation`) | |
| 7 | `config/coverage/baseline.tsv` moves by exactly the rows this change earns, and any comment or provenance line the writer drops is restored by hand and reported against `bean:0033` | |
| 8 | `./gradlew qualityCheck` green | |

## Sequencing

**This lands before `bean:0014`.** `AgentRun` needs a start instant, an end instant and an
id of its own; `doc:10-architecture#bounded-contexts` §3 has `execution` publishing
`AgentRunStarted`, `AgentRunOutput`, `AgentRunCompleted` and `ContextBudgetExceeded`, and
every one of them carries a timestamp the aggregate must mint rather than be handed. There
is no way to write that context without these two ports, and no way to write it correctly
against `Instant.now()`, which `rule:archunit/timeIsInjectedNeverReadFromAStaticClock`
rejects repository-wide.

`bean:0013` does not depend on this — `work`'s `WorkItem` is read from a file that already
carries its `created_at`, so `work` can be built while these ports do not exist. The edge is
to `bean:0014` specifically, and to any later context that mints values.
