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

**Everything a work item is born with arrives as one `WorkItemSpecification`, and its
criteria are mandatory.** This reverses a decision taken earlier in this bean and the
reversal is recorded rather than tidied away. `WorkItem.create` had seven parameters;
Detekt's `LongParameterList` refuses six; the first fix defaulted `criteria` to
`emptyList()`. That was wrong, and wrong in the specific way this bean exists to prevent:
an item with no criteria closes with no evidence — which is the correct rule — so the
default made the **shortest path to a `WorkItem`** the one that produces an item able to
close having proved nothing. A hole in the rule, reachable by writing less code.

The lint was reporting a missing concept rather than asking for a default.
`WorkItemSpecification` is that concept — identity, title, criteria, epic: everything fixed
for the item's life — and it also collapses the root's constructor from eight parameters to
four, because what remains is exactly the mutable half. The duplicate-criterion-id invariant
moves onto it, which is where a structural property of a value belongs
(`doc:20-ddd-practices#invariants` §7.1). `epicId` keeps its default: absence there is
meaningful, and omitting it cannot weaken a guard.

**Empty success criteria stay legal at the aggregate, and this is a decision rather than
an omission.** `WorkItemSpecification.of` requires the criteria argument, so a caller reaches
an item that owes nothing by writing `emptyList()` deliberately — but it may still write it,
and such an item closes freely. Requiring at least one criterion is a **definition-of-done
policy**, and `doc:00-constitution#domain-scoping` makes the definition of done per-domain
data. Hardcoding a minimum into the aggregate would be precisely the defect this bean exists
to avoid — a status enum one level up, a single process imposed on every domain by code.
`ProcessDefinition` has nowhere to carry the policy today; `bean:0156` gives it one. It also
spares a rewrite: nine of the ten state-machine tests run on `NO_CRITERIA`, and forcing
non-empty would obscure what those tests exist to prove.

**A per-context sealed exception root, not a repository-wide `DomainException`.**
`doc:20-ddd-practices#invariants` §7.2 names a sealed `DomainException` hierarchy; a root
spanning every context would be a third shared-kernel member and needs an ADR. `WorkException`
is sealed within this context, which is what makes the REST adapter's `when` exhaustive per
context. `bean:0154` carries the wider question.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `WorkItem` and `Epic` are aggregate roots under `..work.aggregate`: private constructor, named factory, no public mutable surface, `final` | citation + `rule:archunit/aggregatesAreSealedOrFinal` |
| 1b | A `WorkItem` cannot be created without stating its success criteria — no parameter of `WorkItem.create` or `WorkItemSpecification.of` defaults the criteria | citation + test-run |
| 2 | `WorkItemCreated`, `WorkItemTransitioned` and `WorkItemClosed` are raised by the root, never dispatched, and accumulate on `pendingEvents`; `drainEvents` hands them over exactly once | test-run |
| 3 | The state machine is data, not code: two processes with disjoint state vocabularies drive the same aggregate, a state terminal in one is a legal intermediate in the other, and `src/main` under `..domain.work` contains no enum and no state literal | test-run + `grep` over `src/main` |
| 4 | A transition the domain's process does not permit is refused with `WorkItemTransitionNotPermittedException`, and every transition it does permit is allowed | test-run, rejecting **and** accepting case |
| 5 | A transition into a terminal state **of the process supplied** is refused with `WorkItemNotClosableException` naming every criterion with no evidence; with an evidence record per criterion it succeeds. **Amended in review:** the refusal is not unconditional — a caller supplying a foreign process that declares the item's state can still reach a state its own domain calls terminal without it. `bean:0157` carries closing that; this criterion is met as now worded and was overstated as first worded | test-run, rejecting **and** accepting case |
| 6 | An item with no success criteria closes, and an item whose criteria are partly evidenced does not — the guard is per criterion, not "any evidence at all" | test-run |
| 7 | `WorkItemState` accepts exactly what `domainmgmt`'s `StateName` accepts, so the mapping `work` performs is total | test-run over one corpus, both types |
| 8 | Every published type in `..work.published` and `..work.event` is leaf-safe | `rule:archunit/publishedLanguageIsLeaf`, observed failing on a planted violation |
| 9 | `WorkItemRepository` and `EpicRepository` are interfaces in `..work.port` with no implementation | citation + `rule:archunit/portsAreInterfaces` |
| 10 | No domain type in this context hands out a collection it owns | `rule:archunit/noDomainTypePublishesACollectionItOwns` |
| 11 | 100% branch coverage over `..work.aggregate`, and `config/coverage/baseline.tsv` moved by exactly this bean's figures with its regression-provenance block intact | `./gradlew qualityCheck` + `git diff` |
| 12 | Each test is load-bearing: broken subject, observed assertion recorded verbatim, reverted | test-run, per `doc:35-testing#load-bearing-evidence` |
| 13 | Fixtures vary collection size across 0, 1 and 2-or-more wherever a collection is accepted | citation, per `doc:35-testing#fixture-variation` |
| 14 | The `WorkContext` marker is gone and `BoundedContexts` names `work` as a literal | `git diff`; `BoundedContextsTest` still asserts six |
| 15 | `WorkItem.create` reads `process.initial`, provably: a fixture whose process cycles back into its initial state kills a structural derivation | test-run |
| 16 | A process that does not declare the state an item is in is refused, and the residual bypass it does not cover is recorded rather than implied | test-run + `bean:0157` |
| 17 | `./gradlew qualityCheck` is green | test-run |

Eighteen criteria, counting 1b. The table is the authority, not this sentence.

Out of scope, explicitly: consuming `ProcessDefinitionChanged` and every use case
(`bean:0153`); implementing `RaisesDomainEvents` and adding a `DrainEventsTest` case, which
need `bean:0066` merged (`bean:0153`); persistence (`bean:0017`); any REST surface
(`bean:0018`); the two context-isolation ArchUnit rules (`bean:0023`).

## Evidence

`./gradlew qualityCheck` green on the head rebased onto `99212fc`. `:core-domain` goes
137 -> 191 tests; the coverage row moves `0 0 1573 130` -> `0 0 2714 240`, so nothing new is uncovered in either half. Both figures are re-derived by a `coverageBaselineWrite` run on the rebased head, not carried over from before it. Every figure
here is from `./gradlew :core-domain:test` unfiltered unless a `--tests` selector is named.

### Criterion 1b — criteria cannot be omitted

`WorkItemSpecification.of(id, title, criteria, epicId = null)` is the only route into a
`WorkItem`, and `criteria` carries no default. `grep -n "criteria" ` over
`WorkItemSpecification.kt` and `WorkItem.kt` shows one declaration of the parameter and no
`= emptyList()` anywhere in the context.

The reversal this criterion records was found in review, not by a gate, and no gate would
have found it: a defaulted parameter is legal Kotlin and the suite passed with it. What
made it visible was reading the default beside the rule it interacts with — an item with no
criteria closes freely, so defaulting the criteria makes the easiest call the unsafe one.
`bean:0026`'s Detekt gap is the nearest mechanism and it would not catch this either.

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

### Criteria 1b and 10 — the specification, planted after the reversal

The whole sweep was re-run against `WorkItemSpecification`, because the code changed under
the evidence and evidence that describes a previous shape is not evidence. Sixteen plants,
sixteen killed. The three new ones:

| planted | test that caught it | observed |
|---|---|---|
| `require(true)` for the duplicate-criterion-id invariant | `refuses to create a work item whose success criteria share an id` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `criteria` getter returns the backing list | `a caller cannot add a criterion through the specification's getter` | `expected:<3> but was:<0>` |
| `val held = criteria` — no copy on the way in | `a caller cannot add a criterion by mutating the list it built the specification from` | `AssertionFailedError: Unexpected elements from index 2`; `expected:<[SuccessCriterionId(value=c1), SuccessCriterionId(value=c2)]> but was:<[..., SuccessCriterionId(value=c3)]>` |

**Two findings from that re-run, both about the procedure rather than the code.**

*A plant that does not apply reports a pass.* Two of the sixteen came back `BUILD
SUCCESSFUL` and neither was a surviving mutant. One plant point had been rewrapped across
four lines by `ktlintFormat` between the sweep being written and being run, so the
replacement matched nothing; the script's `assert` wrote to stderr and the loop carried on,
running the suite against **unmodified source**. That is `doc:35-testing#load-bearing-evidence`'s
own warning — plant the enabling condition, not only the claim — arriving in the harness
rather than in a fixture. The sweep now runs under `set -eu` and prints `planted` per plant,
so a plant point that has moved stops the run instead of producing a green line.

*The copy-out test cannot reach copy-in.* The other survivor was `val held = criteria`, and
the test aimed at it asserted on the **getter**: with copy-out intact, mutating what the
getter returned changes nothing whether or not the constructor copied. A separate test now
mutates the list the caller built the specification from, which is the only shape that
fails. `doc:20-ddd-practices#value-objects` §3.1 states that the defensive-copy gate does
not read a named factory's body, so this invariant has no mechanical guard at all — it is
this test or it is nothing.

### The plant procedure destroyed uncommitted work, on a source file

`AGENTS.md` documents `git checkout -- .beans` discarding uncommitted bean edits
(`bean:0102`). The same thing happened here to `WorkItem.kt`: the sweep was launched while
the `WorkItemSpecification` refactor was still uncommitted, and the first plant's
`git checkout -- "$WI"` reverted the file to `HEAD`, taking the refactor with it. Every
subsequent plant then failed to compile against test sources that had moved on.

Two things made it recoverable and both were luck rather than design: the refactor was
scripted, so it could be replayed, and `WorkItemSpecification.kt` was **untracked**, so
`git checkout --` could not touch it — which meant it kept three cumulative plants instead,
in a file the revert silently skipped. The rule that would have prevented it is the one
already in `AGENTS.md` and already followed for the first sweep: **a clean tree before every
run, not once before the first.**

### Criterion 10 — the copy gate refused a delegating accessor, twice

`rule:archunit/noDomainTypePublishesACollectionItOwns` rejected `WorkItem` in the
`qualityCheck` run after the specification landed, and it was right to. Neither of these
passed:

```
observed: WorkItem.kt:92: WorkItem.successCriteria: List<SuccessCriterion> —
            the accessor is `specification.criteria`, which is not a copy chain
observed: WorkItem.kt:102: WorkItem.successCriteria: List<SuccessCriterion> —
            the accessor is `specification.criteria.toList()`, which is not a copy chain
```

The second is the interesting one: it **is** a copy, twice over, and the gate still refuses
it. `SINGLE_CALL` is `^(?:this\.)?[A-Za-z_]\w*\.[A-Za-z_]\w*\(\)$` — one receiver, one
call — so a chain through a delegate has one segment too many, and the gate cannot follow
the delegation to learn that `specification.criteria` already copied. It fails closed, which
is the documented design (`doc:20-ddd-practices#value-objects` §3.1: an allowlist binds only
over a set the tool enumerates exhaustively).

The fix is not a second copy but one less indirection: `WorkItem` exposes its
`WorkItemSpecification` whole, exactly as `Domain` exposes its `ProcessDefinition`, and the
spec's own gate-approved `declared.toList()` is the only accessor. The alternative — a
`private val criteria = specification.criteria` field on the root — was rejected because the
gate's copy-**in** arm refuses it for the same reason, and because it would hold the criteria
in two places with the spec as the only authority.

This is recorded as a **cost of the gate**, not a complaint about it. It removed a public
accessor and one covered instruction, and it is the shape `bean:0064` is about.

### Criterion 11, third half — a `-Pcoverage.regress` write, and five erasures

Deleting `successCriteria` shrank fully-covered production code: `2654 -> 2653` covered
instructions, with missed staying 0. `doc:35-testing#coverage` §8.1 names exactly this case,
and `coverageBaselineWrite` correctly refused until given a reason. That write then erased
**both** regression blocks then on `main` while recording the new one — the "it also erases a
PREVIOUS regression block when recording a new one" clause of `bean:0033`, reproduced
cleanly.

**That entry is no longer in the baseline, and the reason is worth stating rather than
quietly dropping.** The rebase onto `99212fc` moved `:core-domain`'s row from `0 0 1543 130`
to `0 0 1573 130`, so the comparison this branch regressed against no longer exists: measured
against the new `main`, both counts rise and no `-Pcoverage.regress` is needed. The figures
above are the observation as taken; the flag is not carried forward because it would be
recording a regression against a baseline nobody will ever hold again. Re-derived by
`coverageBaselineWrite` on the rebased head rather than hand-merged (`AGENTS.md`).

Five erasures in this bean, instances six to ten, the last on the post-rebase write. Between
them they reproduce every shape `bean:0033` describes — the no-op write, the upward write,
and the erasure of a previous regression block — so none of those clauses is now inferred.

### Criterion 15 — `process.initial` is load-bearing, which it was not

Independent review found that replacing the read of `process.initial` with a structural
derivation — *the state no transition points into* — passed the whole suite. `ENGINEERING`,
`RESEARCH` and `EDITORIAL` are each acyclic with exactly one source state, so **all three
fixtures were blind together on this point**: the same failure as `EDITORIAL` once starting
at `shipped`, one read of the process later, and the second time a fixture set sharing a
structural assumption hid a defect in this bean.

`WorkFixture.REWORK` adds `backlog -> doing -> backlog` with `doing -> shipped`, legal under
every `ProcessDefinition` invariant. Rework loops are not exotic — this repository's own bean
lifecycle has one, since a bean under review returns to `in-progress`.

```
planted:  val initial = WorkItemState(
              process.states.single { s -> process.transitions.none { it.to == s } }.value)
observed: WorkItemStateMachineTest > a work item starts wherever its own domain's process
            says work begins() FAILED
          java.util.NoSuchElementException: Collection contains no element matching the predicate.
          WorkItemStateMachineTest > a process may cycle back into the state it starts in() FAILED
            (same exception)
          180 tests completed, 2 failed
reverted: yes — the unmodified source passes
```

### Criterion 16 — the process guard, and the bypass it does **not** close

Review found a genuine bypass of the evidence guard and proposed
`require(currentState.asStateName() in process.states)`. The guard shipped. **It does not
close the bypass**, and that was established by running the probe against it rather than by
reasoning about it:

```
probe:    an ENGINEERING item at `doing`, three criteria, none evidenced,
          moved to `shipped` under a process declaring doing -> shipped with
          `shipped` an ordinary intermediate (WorkFixture.HANDOVER)
observed: SUCCEEDED state=shipped closed=false
```

Any process permitting a move out of `doing` must declare `doing` — `ProcessDefinition`
refuses a transition naming an undeclared state — so membership is **implied by the move
being permitted**, and the check can never fire on the shape it was meant to catch.

What the guard does close is a process that cannot describe the item at all, and it is kept
for that:

```
planted:  requireGoverning(process) removed from transitionTo
observed: WorkItemStateMachineTest > refuses a process that does not declare the state this
            item is in() FAILED
          Expected exception java.lang.IllegalArgumentException but a
            WorkItemTransitionNotPermittedException was thrown instead.
reverted: yes
```

`bean:0157` carries the real fix, which is not available inside the aggregate: binding an
item to its domain's process means caching another aggregate's state, stale the moment
`Domain.adoptProcess` runs. The obligation is the use case's. The surviving bypass is pinned
by `WorkItemEvidenceGuardTest > a foreign process declaring this item's state still bypasses
the close guard - bean 0157`, a characterisation test that must be rewritten when `bean:0157`
closes — so the gap is mechanically visible rather than only written down.

Criterion 5 is amended above to say what the guard actually promises. As first written it
stated the refusal unconditionally, and that was overstated.

### Criterion 12 — a KDoc that overclaimed

`drainEvents leaves the root carrying none` claimed "nothing else in this file fails when it
is removed". Removing `events.clear()` also fails `a mutation of the drained list puts
nothing back into the root` in the same file and `reaching a terminal state raises the
transition and then the close` in `WorkItemStateMachineTest`. Corrected: the test is the one
whose **name** describes the emptying, not the only one that notices.

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

- **`RaisesDomainEvents` is not implemented, and the reason is no longer that it is
  absent.** It arrived on `main` with `bean:0066` (PR #83, merged at `99212fc`) while this
  branch was in review. `WorkItem.drainEvents` has the contract's exact signature and the
  three-line body the other roots use, and both halves are proved independently above.
  Adopting the interface, and adding the `DrainEventsTest` case, is `bean:0153`'s: the
  consumer of `ProcessDefinitionChanged` is a use case, and widening this bean now to take on
  an interface that landed after the split would undo the split for a reason that is not the
  split's. This item is a **deferral to a named bean**, not a blocked one.
- **Nothing forces a write through `WriteThenDispatch`,** and `pendingEvents` stays public
  here as it is on the other three roots. `bean:0066`'s review established that
  `RaisesDomainEvents` does not make a drain correct; that is why the drain has three
  independent plants above rather than a claim of conformance.
- **`Epic` is thin** — identity, domain and title, with no state and no event. The reason is
  in its KDoc and in `bean:0013`: it is a deferral with a named question, not an oversight.
