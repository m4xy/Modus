---
# modus-0152
title: The WorkItem aggregate, its per-domain state machine and the evidence guard
status: in-progress
type: feature
priority: high
order: AN
created_at: 2026-09-05T00:00:00Z
parent: modus-0013
blocked_by: [modus-0030]
---

# The WorkItem aggregate, its per-domain state machine and the evidence guard

First child of `bean:0013`. `domainmgmt` now creates a domain and gives it the process it
imposes (`bean:0030`); nothing yet moves through one. This bean is the thing that moves.

The shape is `bean:0009`'s, most recently exhibited by `bean:0030`, and is not restated:
placement by kind, published language as a leaf, invariants in `init`, private constructor
plus named factory, time as a parameter, ports declared and unimplemented, evidence per
`doc:35-testing#load-bearing-evidence` with fixtures varied per
`doc:35-testing#fixture-variation`.

## Scope

Owned: `core/core-domain/src/{main,test}/kotlin/uk/m4xy/modus/core/domain/work/**`,
the `WorkContext` marker with `BoundedContexts`' reference to it, and
`config/coverage/baseline.tsv`. The marker's own KDoc says *"delete it as soon as the
context has a real aggregate root"*, and this bean is where that root arrives;
`bean:0030` set the precedent.

Not owned: `identity/**` and `domainmgmt/**` except as imports; the shared kernel —
`DomainId` is imported, never copied; `core-application/**`, which is `bean:0153`;
`adapters/**` (`bean:0017` owns the store and is in flight); `modules/**`, `app/**`,
`backoffice/**`, `e2e/**`, `documentation/**`.

## Decisions

**The state machine is `domainmgmt`'s `ProcessDefinition`, passed in at the call, and the
aggregate holds no states of its own.** `WorkItem.transitionTo` takes the definition as a
parameter and asks it three questions — `allows(from, to)`, `isTerminal(to)` — and there is
no fourth source of truth. There is no enum anywhere in the context, no `OPEN`/`CLOSED`
constant, and no state literal in `src/main`: `WorkItemState` is a validated *name*, exactly
as `StateName` is, and its membership is per-domain data
(`doc:00-constitution#domain-scoping`). Criterion 3 proves this by construction rather than
by assertion — two processes with disjoint state vocabularies drive the same aggregate, and
a state that is terminal in one is a legal intermediate state in the other.

**A close is a transition to a terminal state, and there is no second method for it.**
`transitionTo` is the only writer of the state. When the target is terminal it applies the
evidence guard before mutating, and raises `WorkItemClosed` after `WorkItemTransitioned`.
The alternative — a separate `close()` beside a `transitionTo()` that permits any legal
move — was rejected because it makes the evidence guard bypassable by calling the other
method, and `doc:00-constitution#evidence-rule` is not a rule a caller may opt out of. What
"done" means is still per-domain: the guard fires on whatever states *this domain's*
process declares terminal.

**`work` declares its own evidence types and imports none of `memory`'s.**
`doc:10-architecture#bounded-contexts` §3.1 forbids `work` from importing `memory` in any
form, so `EvidenceRecord`, `EvidenceKind` and `EvidenceReference` are this context's
internals. `EvidenceKind` is an opaque validated name, never an enum, for the same reason
`StateName` is one: `doc:00-constitution#domain-scoping` lists required evidence kinds
beside states and definition of done as things a domain defines for itself. Per-domain
*required* kinds stay deferred exactly as `bean:0030` deferred them, and for the same
reason — the closed set they would override is `memory`'s and does not exist yet
(`bean:0015`).

**No domain event in this context names an actor, and `doc:20-ddd-practices` §4.1's own
snippet cannot compile.** That snippet puts `val actorId: ActorId` into
`work.event.WorkItemTransitioned`, importing `identity.published`. `rule:archunit/publishedLanguageIsLeaf`
refuses it: a published package may reference only the Kotlin stdlib, `java.time`, its own
context's published language and the shared kernel. The allowlist in
`doc:10-architecture#bounded-contexts` §3.1 permits `work` to import `identity`'s published
language from its *internals*, not from its own published packages. Attribution's whole
value is in the event, so it is left out entirely rather than modelled where it cannot be
published. `bean:0154` carries correcting the document and deciding whether `ActorId`
becomes a third shared-kernel member (which needs an ADR,
`adr:0004-domain-id-shared-kernel#shared-kernel-membership`).

**A per-context sealed exception root, not a repository-wide `DomainException`.**
`doc:20-ddd-practices#invariants` §7.2 names a sealed `DomainException` hierarchy; a root
spanning every context would be a third shared-kernel member and needs an ADR. `WorkException`
is sealed within this context, which is what makes the REST adapter's `when` exhaustive per
context. `bean:0154` carries the wider question.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `WorkItem` and `Epic` are aggregate roots under `..work.aggregate`: private constructor, named factory, no public mutable surface, `final` | citation + `rule:archunit/aggregatesAreSealedOrFinal` |
| 2 | `WorkItemCreated`, `WorkItemTransitioned` and `WorkItemClosed` are raised by the root, never dispatched, and accumulate on `pendingEvents`; `drainEvents` hands them over exactly once | test-run |
| 3 | The state machine is data, not code: two processes with disjoint state vocabularies drive the same aggregate, a state terminal in one is a legal intermediate in the other, and `src/main` under `..domain.work` contains no enum and no state literal | test-run + `grep` over `src/main` |
| 4 | A transition the domain's process does not permit is refused with `WorkItemTransitionNotPermittedException`, and every transition it does permit is allowed | test-run, rejecting **and** accepting case |
| 5 | A transition into a terminal state is refused with `WorkItemNotClosableException` naming every criterion with no evidence record; with an evidence record per criterion it succeeds | test-run, rejecting **and** accepting case |
| 6 | An item with no success criteria closes, and an item whose criteria are partly evidenced does not — the guard is per criterion, not "any evidence at all" | test-run |
| 7 | `WorkItemState` accepts exactly what `domainmgmt`'s `StateName` accepts, so the mapping `work` performs is total | test-run over one corpus, both types |
| 8 | Every published type in `..work.published` and `..work.event` is leaf-safe | `rule:archunit/publishedLanguageIsLeaf`, observed failing on a planted violation |
| 9 | `WorkItemRepository` and `EpicRepository` are interfaces in `..work.port` with no implementation | citation + `rule:archunit/portsAreInterfaces` |
| 10 | No domain type in this context hands out a collection it owns | `rule:archunit/noDomainTypePublishesACollectionItOwns` |
| 11 | 100% branch coverage over `..work.aggregate`, and `config/coverage/baseline.tsv` moved by exactly this bean's figures with its regression-provenance block intact | `./gradlew qualityCheck` + `git diff` |
| 12 | Each test is load-bearing: broken subject, observed assertion recorded verbatim, reverted | test-run, per `doc:35-testing#load-bearing-evidence` |
| 13 | Fixtures vary collection size across 0, 1 and 2-or-more wherever a collection is accepted | citation, per `doc:35-testing#fixture-variation` |
| 14 | The `WorkContext` marker is gone and `BoundedContexts` names `work` as a literal | `git diff`; `BoundedContextsTest` still asserts six |
| 15 | `./gradlew qualityCheck` is green | test-run |

Out of scope, explicitly: consuming `ProcessDefinitionChanged` and every use case
(`bean:0153`); implementing `RaisesDomainEvents` and adding a `DrainEventsTest` case, which
need `bean:0066` merged (`bean:0153`); persistence (`bean:0017`); any REST surface
(`bean:0018`); the two context-isolation ArchUnit rules (`bean:0023`).

## Evidence

`./gradlew qualityCheck` green. `:core-domain` goes 130 -> 173 tests; the coverage row moves
`0 0 1543 130` -> `0 0 2505 216`, so nothing new is uncovered in either half.

### Criterion 3 — the state machine is data, not code

Three fixture processes (`WorkFixture`). `ENGINEERING` and `RESEARCH` share no state name.
`EDITORIAL` moves **into** `shipped`, which `ENGINEERING` ends at.

`grep -rn --include='*.kt' 'enum class'` over
`core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/work` matches nothing. Every
string literal in that tree is a regex, an exception message, or KDoc prose; none reaches a
state or evidence-kind decision.

**The first version of this criterion's evidence was wrong, and that is recorded rather than
deleted.** The test named `one state name is terminal in one process and a legal intermediate
in another` did **not** discriminate a hardcoded terminal set. `EDITORIAL` had `shipped` as
its *initial* state, so nothing ever moved into it, and the only move the test made under
that process was `shipped -> subedit` — which a planted
`target.value == "shipped" || target.value == "abandoned"` agrees with. The plant passed:

```
planted:  val closing = target.value == "shipped" || target.value == "abandoned"
observed: BUILD SUCCESSFUL in 4s          <-- the test that names this property PASSED
```

`EDITORIAL` is now `draft -> shipped -> subedit -> printed`, so the same move `-> shipped`
is a close under one process and an ordinary move under the other, and the editorial half
carries an unevidenced criterion. The same plant now dies in four tests:

```
planted:  val closing = target.value == "shipped" || target.value == "abandoned"
observed: WorkItemStateMachineTest > one state name is terminal in one process and a legal
            intermediate in another() FAILED
          uk.m4xy.modus.core.domain.work.WorkItemNotClosableException: work item
            'modus-0152' cannot close: no evidence recorded for c1
            at ...WorkItem.transitionTo-82iMEy8(WorkItem.kt:173)
          whole suite: 173 tests completed, 4 failed — also
            `every terminal state a process declares closes the work`
            (IllegalStateException: expected WorkItemClosed but was WorkItemTransitioned),
            `a move to a non-terminal state raises no close`,
            `accepts evidence in a state another process would call closed`
reverted: yes
```

### Criteria 2, 4, 5, 6, 7, 10, 12 — targeted mutation

Thirteen planted, thirteen killed, each by the test whose name describes the behaviour, each
reverted. The assertion each produced, verbatim:

| planted | test that caught it | observed |
|---|---|---|
| `drainEvents` drops `events.clear()` | `drainEvents leaves the root carrying none` | `AssertionFailedError: Unexpected elements from index 1`; `expected:<[]> but was:<[WorkItemCreated(...), WorkItemTransitioned(...)]>` |
| `drainEvents` returns the live list, still clears | `drainEvents hands over everything the root had raised, oldest first` | `AssertionFailedError: Missing elements from index 0`; `expected:<[WorkItemCreated(...), WorkItemTransitioned(...)]> but was:<[]>` |
| `drainEvents` returns the live list, no clear | `a mutation of the drained list puts nothing back into the root` | `AssertionFailedError: Element differ at index: [0, 1]`; `expected:<["WorkItemTransitioned", "WorkItemClosed"]> but was:<["WorkItemCreated", "WorkItemTransitioned", "WorkItemCreated", "WorkItemTransitioned", "WorkItemClosed"]>` |
| `create` hardcodes the initial state | `a work item starts wherever its own domain's process says work begins` | `expected:<WorkItemState(value=question)> but was:<WorkItemState(value=backlog)>` |
| terminal states hardcoded | `one state name is terminal in one process and a legal intermediate in another` | `WorkItemNotClosableException: work item 'modus-0152' cannot close: no evidence recorded for c1` |
| transition guard dropped | `refuses a move this domain's process does not declare` | `Expected exception uk.m4xy.modus.core.domain.work.WorkItemTransitionNotPermittedException but no exception was thrown.` |
| close guard dropped | `refuses a close when no success criterion carries evidence` | `Expected exception uk.m4xy.modus.core.domain.work.WorkItemNotClosableException but no exception was thrown.` |
| guard counts records, not criteria | `refuses a close when three records all evidence one criterion` | `Expected exception uk.m4xy.modus.core.domain.work.WorkItemNotClosableException but no exception was thrown.` |
| state written before `from` is read | `permits exactly the moves this domain's process declares` | `expected:<WorkItemState(value=backlog)> but was:<WorkItemState(value=doing)>` |
| unknown-criterion check dropped | `refuses evidence for a criterion this work item does not have` | `Expected exception uk.m4xy.modus.core.domain.work.UnknownSuccessCriterionException but no exception was thrown.` |
| closed-item check dropped | `refuses evidence for a work item that has already closed` | `Expected exception uk.m4xy.modus.core.domain.work.WorkItemAlreadyClosedException but no exception was thrown.` |
| `evidenceRecords` hands out the backing list | `a caller cannot evidence a criterion through the evidence getter` | `AssertionFailedError: Unexpected elements from index 2`; `expected:<[SuccessCriterionId(value=c1), SuccessCriterionId(value=c2)]> but was:<[..., SuccessCriterionId(value=c3)]>` |
| `WorkItemState` accepts upper case | `WorkItemState and StateName reach the same verdict on every value in the corpus` | `AssertionFailedError: Unexpected elements from index 1`; `expected:<[]> but was:<["Todo", "TODO"]>` |

The negative half sits beside each refusal, per `doc:00-constitution#observed-failing`:
`permits a close when every success criterion carries evidence`, `an item with no success
criteria closes with no evidence`, `a move to a non-terminal state needs no evidence`,
`accepts evidence for a criterion this work item has`, `accepts evidence in a state another
process would call closed`, `permits exactly the moves this domain's process declares`,
`accepts success criteria with distinct ids`. A guard that fired on every input fails all
seven.

### Criterion 8 — `rule:archunit/publishedLanguageIsLeaf`

Planted on the real event type, not on a probe class.

```
planted:  WorkItemCreated gains `val probe: ActorId get() = ActorId("agent")`
observed: ArchitectureRulesTest > publishedLanguageIsLeaf FAILED
          Rule '... should depend on nothing beyond the Kotlin stdlib, java.time, their own
            context's published language and the shared kernel ...' was violated (1 times):
          Method <uk.m4xy.modus.core.domain.work.event.WorkItemCreated.getProbe-TPKKjuw()>
            calls method <uk.m4xy.modus.core.domain.identity.published.ActorId.constructor-impl(
            java.lang.String)> in (WorkEvents.kt:44)
reverted: yes
```

This is also the observation behind `bean:0154`: the event `doc:20-ddd-practices` §4.1
prints as this context's worked example cannot be built.

### Criterion 9 — `rule:archunit/portsAreInterfaces`

`work.port` had to be added to `PORT_PACKAGES` in `ArchitectureRulesTest`, and the guard on
the guard is what said so, unprompted, on the first run:

```
observed: ArchitectureRulesTest > everyPortPackageIsSeenByPortsAreInterfaces FAILED
          (ArchitectureRulesTest.kt:381)
```

That is the rule's own KDoc working as written: *"a new context's port package is a
deliberate edit here"*.

### Criterion 11 — the 100% aggregate branch floor, shown non-vacuous over the new package

```
planted:  WorkItem gains `fun probe(): String = if (title.value.isEmpty()) "empty" else "present"`
observed: Rule violated for package uk.m4xy.modus.core.domain.work.aggregate:
            branches covered ratio is 0.8, but expected minimum is 1.0
          Rule violated for bundle core-domain: instructions missed count is 13, but expected maximum is 0
          Rule violated for bundle core-domain: branches missed count is 4, but expected maximum is 0
reverted: yes
```

`coverageBaselineWrite` refused the first two runs, both correctly: 60 then 1 missed
instruction, from data-class accessors no test read individually and from the synthetic
`create$default` bridge that every call site bypassed by passing both optional arguments.
Both are now covered — by tests that read those accessors, and by one that takes the
defaults. The last missed branch was `filterValues { it.size > 1 }` inside the duplicate-id
message, reachable only when *some* id is distinct, which no fixture supplied; `a
duplicate-id refusal names only the ids that repeat` supplies it.

### Criterion 11, second half — the baseline's regression-provenance block

`coverageBaselineWrite` erased all eleven comment lines again, on a write in which **no
figure regressed** (`0 0 1543 130` -> `0 0 2505 216`, both counts upward). Restored by hand
from `git show origin/main:config/coverage/baseline.tsv`, with a line recording this
instance. Sixth occurrence, and the second on a non-regressing write; `bean:0033` carries
the fix.

### Criterion 14 — the marker

`git diff` shows `WorkContext.kt` deleted and `BoundedContexts` naming `work` as a literal
beside `identity` and `domainmgmt`. `BoundedContextsTest` still asserts six contexts and
passes unchanged.

### On filtered runs

Every figure above names its selector. `173 tests completed` is `./gradlew :core-domain:test`
unfiltered; the single-assertion transcripts are `--tests "*<Class>.<test name>*"` and are
labelled as such rather than reported as suite runs.

## Not met, stated plainly

- **`RaisesDomainEvents` is not implemented.** `WorkItem.drainEvents` has the contract's
  exact signature and the three-line body the other roots use, and both halves are proved
  independently above — but the interface does not exist on `main`, and this branch is cut
  from `main`. No `DrainEventsTest` case is added, for the same reason. `bean:0153` carries
  both, `blocked_by: [modus-0066]`.
- **Nothing forces a write through `WriteThenDispatch`,** and `pendingEvents` stays public
  here as it is on the other three roots. `bean:0066`'s review established that
  `RaisesDomainEvents` does not make a drain correct; that is why the drain has three
  independent plants above rather than a claim of conformance.
- **`Epic` is thin** — identity, domain and title, with no state and no event. The reason is
  in its KDoc and in `bean:0013`: it is a deferral with a named question, not an oversight.
