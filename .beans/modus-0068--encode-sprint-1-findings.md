---
# modus-0068
title: Encode sprint 1's findings into the documentation package
status: in-progress
type: task
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Encode sprint 1's findings into the documentation package

Thirteen findings from sprint 1 exist only in a session transcript. A transcript is
discarded (`doc:50-memory-and-evidence` §1), so every one of them is a thing the next agent
pays to rediscover — which is what `doc:README#encoding-rule` exists to stop.

They are not a list of bullet points. They group into families, and each family ships as its
own pull request — the rows below are the stack, and the count is not restated in prose
because the stack grew twice while this bean was open
(`doc:05-authoring-for-agents#one-fact-one-place`):

Sections below are headed by **branch**, not by position: the stack was reordered once
already, and a position restated in five headings is five copies of one fact.

| # | branch | family | findings | documents |
|---|---|---|---|---|
| 2 | `docs/encode-sprint-1-agent-loop` | the agent loop — what an agent does, reports and pushes | report incrementally; read the diff before pushing; a closure that needs evidence is a work item; measure against this run; three orchestration anti-patterns | `doc:80-agent-operating-procedure` |
| 3 | `docs/encode-sprint-1-claims` | claims — the shapes in which something reads as verified and is not | unsourced numbers; unqualified counts; arithmetic offered as measurement; citing while restating; a fabricated justification for a declined fix; duplication reintroduced "for the reader" | `doc:50-memory-and-evidence`, `doc:05-authoring-for-agents`, `doc:80-agent-operating-procedure` |
| 4 | `docs/encode-sprint-1-gate-design` | gates — what makes a mechanism believable | the input surface is a separate subject from the decision; plant the enabling condition; allowlist versus requirement; a fix is a claim too | `doc:00-constitution`, `doc:35-testing`, `doc:15-repository-layout` |
| 1 | `docs/one-owner-for-ambient-port-names` | one owner for the ambient-capability port names | routed in mid-task by the orchestrator; the same family as finding 3(c) | `doc:00-constitution`, `doc:20-ddd-practices` |
| 5 | `docs/spend-record-behind-its-recorder` | a normative document behind the artefact it governs | routed in mid-task by the orchestrator | `doc:60-cost-model` |

## Provenance

The observations below were reported by the orchestrator of sprint 1 from its own session,
and are recorded here as reported. Where an observation has a durable artefact in this
repository — a bean, a merged pull request, a document — the artefact is cited beside it and
is the thing a reader should check. Where it has none, it is attributed to the sprint and to
nothing else. No command was reconstructed to stand in for one that was never run
(`doc:50-memory-and-evidence#unevidenced-assertions`).

| # | reported observation | durable artefact |
|---|---|---|
| 1 | Across seven review rounds of the defensive-copy gate every escape entered through the parser's input surface; the decision logic was correct each time, and the rule's own test passed each time because its fixture supplied an enabling condition the real code omitted | `bean:0036`, `bean:0064` |
| 2 | Three rounds of allowlists failed on the same gate; the requirement — a non-private function mentioning a backing field MUST declare a return type — held under direct attack | `bean:0036`; `doc:20-ddd-practices#value-objects` §3.1 |
| 3 | Four shapes in which a claim read as verified and was not: an `Enforced by:` line for a mechanism nobody had watched reject anything; a number with no command; a citation to an anchor that did not carry the claim made at it; a fabricated justification for a *declined* fix | (a) `doc:00-constitution#observed-failing`; (c) `bean:0058`, criterion 4's note; (b) and (d) the sprint |
| 4 | A bean whose closure needed evidence authored first was treated as bookkeeping and shipped as a closure | the sprint |
| 5 | Criteria were walked against a CI run from before the closing change | the sprint |
| 6 | A `122s` baseline figure was cited by three successive agents and had never been measured; the runs that existed were 133s and 134s | the sprint |
| 7 | A table of figures was checked by verifying that it summed, and reported as verified | the sprint |
| 8 | "51 references" was recorded with neither the command nor the tree it was counted on | the sprint |
| 9 | A fix shipped with nothing that could be observed to protect it | `bean:0064`, criteria 3 and 4 |
| 10 | A duplicated fact was removed and restated in the same change "for the reader's convenience", repeatedly | the sprint |
| 11 | Six agents finished their work and lost the report to an interruption, having been briefed to save it for one dense final message | the sprint |
| 12 | An agent ran `git reset --soft origin/main` and force-pushed a staged revert of main's entire history: three beans deleted, two documents rolled back, live approximately four minutes, self-reported | the sprint |
| 13 | Three orchestration failures: a merge order chosen so a gate would not fire, which froze two beans behind the append-only rule; an action queued to one agent stated to another as accomplished; a bean id allocated centrally and the branch then shipped with no work item | the sprint |

## Scope

Owned: `documentation/00-constitution.md`, `documentation/05-authoring-for-agents.md`,
`documentation/10-architecture.md`, `documentation/15-repository-layout.md`,
`documentation/20-ddd-practices.md`, `documentation/35-testing.md`,
`documentation/50-memory-and-evidence.md`, `documentation/60-cost-model.md`,
`documentation/80-agent-operating-procedure.md`, this bean, and the two beans it raises.

The first four of those were missing while branches were already editing them. The stack
table forty lines above named `doc:20-ddd-practices` and `doc:60-cost-model` as the
documents two entries change, and the Owned list did not — one fact in two places, disagreeing,
inside the bean that ships the rule against it (`doc:05-authoring-for-agents#one-fact-one-place`).
Found in review of the base branch.

Not owned, and not touched: `tools/docs-lint.sh`, `core/core-domain/`, `AGENTS.md`, any
build file, and every bean but this one.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `doc:80-agent-operating-procedure` states the report-as-you-go rule in a section addressed to every agent, and the orchestrator's brief-contents list cites it rather than restating it | citation |
| 2 | `doc:80-agent-operating-procedure` step 5 requires reading the branch's own name list against `origin/main` before every push, and names the command shapes that make it necessary | citation |
| 3 | `doc:80-agent-operating-procedure` step 6 states that a closure needing evidence authored is a work item, and that every criterion is walked against the run the closing change produced | citation |
| 4 | `doc:80-agent-operating-procedure` §0 carries the three orchestration anti-patterns, each with what it actually cost | citation |
| 5 | `doc:50-memory-and-evidence` §2.2 rejects an unsourced figure, an unqualified count, an arithmetic check offered as a measurement, and a citation that does not carry its claim | citation |
| 6 | `doc:50-memory-and-evidence` states the four shapes in which a claim reads as verified and is not, in one place, with the fabricated justification for a declined fix among them | citation |
| 7 | `doc:05-authoring-for-agents#one-fact-one-place` states that restating a removed fact in the same change reintroduces it, and that a pointer carrying content its target does not is an unowned rule | citation |
| 8 | `doc:00-constitution#observed-failing` binds a fix as it binds an `Enforced by:` line | citation |
| 9 | `doc:00-constitution` §9 states the allowlist-versus-requirement rule and the condition under which an allowlist is admissible | citation |
| 10 | `doc:35-testing#load-bearing-evidence` requires the plant to establish the claim's enabling condition, and separates a mechanism's input surface from its decision | citation |
| 11 | Every touched document is inside `docs-lint` check 8's budget, and `doc:00-constitution` is no longer than the 500 lines it had before this bean | command |
| 12 | `./gradlew qualityCheck` green on every branch in the stack | test-run |
| 13 | The ambient-capability port names have exactly one owning anchor, `doc:00-constitution` §1.3 defers to it, and the third port `doc:00` never named survives | citation |
| 14 | `doc:60-cost-model#spend-record` names the token kinds the recorder actually writes, and §2's model can price them | citation |
| 15 | `doc:20-ddd-practices` §5.1 has a row for the context-free port package, stating that it is not the shared kernel and does not inherit `adr:0004`'s membership gate | citation |
| 16 | Every package named in §5.1's tables carries the root and segment order this repository uses, and the rows with no members are named as having none | command |

## Evidence

Each pull request in the stack records its own criteria here, against its own runs. A
figure from an earlier branch is not evidence for a later one — criterion 3's rule, applied
to this bean.

### `docs/one-owner-for-ambient-port-names` — the base of the stack

Routed in by the orchestrator mid-task: `doc:00-constitution` §1.3 named `Clock` and
`IdGenerator`, `doc:20-ddd-practices` §5.3 names `ClockPort`, `IdGeneratorPort` and
`RandomPort`, and the ambient-ports bean had to pick. It picked `doc:00`'s names because
`doc:00-constitution`'s precedence line makes this file win.

**The ruling is `ClockPort`, `IdGeneratorPort`, `RandomPort`, and the argument below is not
the one this bean first gave.** The first version said precedence "settles two documents
stating the same rule differently" and so did not reach this case. That scoping is nowhere in
`doc:00-constitution`, whose line 28 reads flatly *"this file > every other file in
`documentation/`"*. It was a gloss stated as if it were the clause — the third of the four shapes stack entry 3
adds to `doc:50-memory-and-evidence` §2.5, a citation that resolves without carrying the
claim made at it, committed inside the bean that encodes it, and caught in review. It is replaced
here rather than deleted, because a bad argument left where a later reader can cite it as
settled is the failure `doc:50-memory-and-evidence#primary-sources` exists to stop.

Three textual grounds, each checked against `origin/main` at `8181726`:

| # | ground | verified |
|---|---|---|
| 1 | **`doc:00` already defers to an owning document against its own precedence line.** §1.1 ends: "The table in `10-architecture.md` §4.1 is the machine-readable form … If they disagree, §4.1 wins and this table is the bug." Ownership-over-precedence is a pattern the constitution already applies to itself | `git show origin/main:documentation/00-constitution.md` line 74 |
| 2 | **Precedence produces an incoherent result.** §1.3 covers time and identifiers and never mentions randomness, so it cannot name a third port. Reading precedence as decisive yields `{Clock, IdGenerator, RandomPort}` — two names from one document and one from another, for three ports of one kind | same file, lines 86–87 |
| 3 | **The naming rule pre-existed and `doc:00` states nothing contrary.** §5.2's outbound-port row already gives `<Noun>Port` with `ClockPort` as its own example, and §7's prohibitions table already names `ClockPort` as the replacement for `Instant.now()` — both on `main`, before this change | `git show origin/main:documentation/20-ddd-practices.md` lines 311 and 430 |

Ground 1 cites a clause that entry 4 of this stack deletes with §1.1's table. The deference
survives in stronger form — §1.1 becomes "`doc:10-architecture#module-dependencies` §4.1 is
the one dependency table" — so the precedent holds after the stack lands, but a reader
checking the citation on a later `main` should look there.

§1.3's own contribution to the confusion is unchanged by any of this: "a `Clock` port passed
as a constructor argument" is a noun phrase in prose that an implementer read as a type name,
which is the evidence that it reads as one.

| # | criterion | observed |
|---|---|---|
| 13 | one owner, deferred to, third port intact | `documentation/20-ddd-practices.md` §5.3 carries `<a id="ambient-ports">` and "This section **decides** the names"; `documentation/00-constitution.md` §1.3 reads "named by `doc:20-ddd-practices#ambient-ports` §5.3 and by nothing here"; `RandomPort` was never in `doc:00` and is untouched in §5.3. Re-derived against the finished files, not the intention: `grep -rn '\`Clock\`' documentation/ \| wc -l` → `1`, the one line in §5.3 that discusses the unsuffixed form on purpose. Before this correction it was 4 |
| 15 | the context-free port package has a row that denies the kernel gate | `documentation/20-ddd-practices.md:307`, the §5.1 row for `uk.m4xy.modus.core.domain.port`: "**A subpackage of the shared kernel's package, and not a member of the shared kernel.**", followed by the name-set argument, the membership test it fails and the condition that test stands under. Quote re-read off the row at certification, not carried from the row it replaced — see below |
| 16 | root and segment order correct on every row; rows with no members named as such | `grep -c "com\.modus"` → `0` in both `documentation/20-ddd-practices.md` and `documentation/10-architecture.md`. That grep proves the **root** and nothing more, which is why the criterion is now worded to what it evidences: the `core-domain` rows match `grep -rh "^package " core/` (15 live packages under `uk.m4xy.modus.core.domain`) and `ArchitectureRulesTest.kt:309-311`, and §5.1's note names the five rows that have no members — `…core.domain.port`, the `core-application` row and the three `adapter.*` rows |
| 11 | budgets held | `doc:00` 500 of 500, the §1.3 rewrite being line-for-line; `doc:20` 500 of 500, having reached the ceiling exactly during the review fixes; `doc:10` 288 of 500 and `doc:15` 233 of 500, each one line longer than `main` and otherwise substitutions in place |
| 12 | the gate | the run below, on this branch |

```
cmd:      wc -l documentation/00-constitution.md documentation/10-architecture.md documentation/15-repository-layout.md documentation/20-ddd-practices.md
observed: 500 documentation/00-constitution.md
          288 documentation/10-architecture.md
          233 documentation/15-repository-layout.md
          500 documentation/20-ddd-practices.md
exit:     0

cmd:      ./gradlew qualityCheck                      (re-run on the corrected head)
observed: docs-lint: OK — 19 documents, 107 anchors, 985 references, 68 beans, 28 graph
          edges, 24 selectable, 68 bean ids, 2 introduced, 66 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 14s
tree:     this branch rebased onto origin/main at 08936ee
exit:     0

cmd:      git diff --numstat origin/main -- documentation/
observed: 4    4   documentation/00-constitution.md
          7    6   documentation/10-architecture.md
          2    1   documentation/15-repository-layout.md
          34  21   documentation/20-ddd-practices.md
exit:     0
```

Two notes on that block, both about figures that cannot certify themselves.

The `--numstat` is scoped to `documentation/` deliberately. A row that counts the file the
row is written in changes every time the file is edited, so it is stale the moment it is
pasted. The whole-diff figure belongs in the pull-request body, derived at push time, where
nothing reads it back; the two bean files are what this scope excludes.

`BUILD SUCCESSFUL in 14s` is the observed run, and the duration is the one field here that
does not reproduce — an earlier head printed 15s and 20s for the same command. Every other figure in the
block was re-run against the head that contains it and printed identically, which is the
point: the numbers were re-derived at certification, not carried from when they were
believed.

Three classes of figure in that block do not behave alike, and only the third is genuinely
certifiable. `BUILD SUCCESSFUL in 14s` is **non-reproducible** — the same command on the same
head prints a different duration. `64 on origin/main` was **externally dependent**: it counts
a ref this branch does not contain, and it changed from 64 to 66 between two runs of the same
working tree, because `main` gained `bean:0091` and `bean:0098` while this bean was being
corrected. No amount of care makes that field reproducible for a later reader; it is quoted as
observed against a named `origin/main`, not as a property of this head. Everything else is
**reproducible**, and was re-run to prove it.

**Quoting a gate line inside a file the gate counts is self-referential, and a stable value
is not guaranteed.** `docs-lint`'s `references` figure counts the typed references in this
bean, so pasting a line that names one moves the number the line reports. A fixed point
exists only when the paste itself adds no counted token, and it was reached here in two
runs: the prose was finalised first, the gate run, then the figures pasted — a paste that
adds no reference — and the gate re-run on the committed head printed the same line back.
Where it does not converge, do not hunt it: name the run and stop quoting it, which is what
`adr:0005-evidence-lives-in-the-work-item#evidence-home` asks for in the pull-request body
already. That is the general resolution and it belongs to the bean raised for this class.

#### The context-free port package, and why it is not the shared kernel

Routed in later, and it belongs on this branch because it is a second §5.x edit to one file.
The reasoning was relayed; it was checked here against `adr:0004` and the rule source before
the row was written, and it **holds**.

| `adr:0004-domain-id-shared-kernel#shared-kernel-membership` test | a port at `uk.m4xy.modus.core.domain.port` |
|---|---|
| 1 — belongs to no bounded context | passes |
| 2 — appears in more than one context's **published language**, or must | **fails, and cannot pass while the leaf rule stands.** `publishedLanguageIsLeaf` restricts `$DOMAIN_ROOT.*.published..` and `$DOMAIN_ROOT.*.event..` to the stdlib, `java.time`, their own published language and the shared kernel (`ArchitectureRulesTest.kt:184`), so a published type or event naming a port is itself the violation. Not "can never pass", which was the first wording: `adr:0004-domain-id-shared-kernel#deferred-conflict` defers the leaf rule to `bean:0023` in terms, so citing `adr:0004` for an absolute cites it against itself |
| 3 — is a leaf | passes, vacuously |
| 4 — adding it is an ADR | **never reached.** Membership requires *every* test; test 2 having failed, the gate does not apply |

The "on the third member the kernel gets its own package" trigger does not fire either: it
counts kernel members, and `SHARED_KERNEL = setOf(SHARED_KERNEL_EVENT, SHARED_KERNEL_DOMAIN_ID)`
(`ArchitectureRulesTest.kt:334`) is a **name set**, not a package prefix. A type in the
subpackage `…core.domain.port` joins nothing by sitting there —
`rule:archunit/sharedKernelIsLeaf` cannot see it. So the placement is sound, and the risk is
purely one of reading: §5.1's shared-kernel row says `core.domain` holds the kernel "and
nothing else without an ADR", and a reader who does not parse `core.domain.port` as a
different package will conclude the gate applies. That is what the new row exists to deny.

#### §5.1's package tables named packages this repository does not have

Found while writing that row, and it had to be fixed for the row to be true rather than
consistent with its neighbours. Every package in both tables was wrong twice — the root, and
the position of the context segment:

```
cmd:      grep -rh "^package " core/ | sort -u
observed: uk.m4xy.modus.core.domain
          uk.m4xy.modus.core.domain.identity.aggregate
          uk.m4xy.modus.core.domain.identity.event
          uk.m4xy.modus.core.domain.identity.port
          uk.m4xy.modus.core.domain.identity.published        (and domainmgmt alike)
exit:     0

cmd:      sed -n '300p;309,311p' architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt
observed: const val ROOT: String = "uk.m4xy.modus"
          private const val PUBLISHED_LANGUAGE = "$DOMAIN_ROOT.*.published.."
          private const val DOMAIN_EVENTS = "$DOMAIN_ROOT.*.event.."
          private const val AGGREGATES = "$DOMAIN_ROOT.*.aggregate.."
exit:     0
```

§5.1 said `com.modus.core.<ctx>.domain.aggregate`. The live shape is
`uk.m4xy.modus.core.domain.<ctx>.aggregate`, and it is the shape the three ArchUnit constants
scope on — so the section that opens "a type in the wrong package silently removes it from a
rule" was itself naming the wrong packages. Corrected in the `core-domain` rows, which are
the evidenced ones. The `core-application` and adapter rows have no members yet, so only
their root is corrected and a line under the table now says which rows are instantiated and
which state an intention.

#### What review found, and what it cost to have written it early

Six findings on this branch, and three of them are this stack's own subject committed in the
change that ships it: a criterion evidenced by a proxy (`grep -c "com\.modus"` proves the
**root**, not "every package is one the repository uses"); a size line in the pull-request
body that no reading of `git diff --numstat` produces; and a normative sentence claiming a
completeness the same file refutes three times. None came from carelessness. Each was written
at the moment it was true of the intention and never re-derived against the finished artefact.

That is a distinct failure from the four shapes stack entry 3 encodes, and entry 3 now carries
it: a claim is re-derived against the artefact at the moment it is certified, not at the moment
it is believed. The three instances here are its evidence.

The other three findings: `can never pass` cited `adr:0004` for an absolute that
`adr:0004-domain-id-shared-kernel#deferred-conflict` defers in terms; "sibling" was wrong for a
subpackage and could have sent an implementer to `uk.m4xy.modus.core.port`; and this bean's own
Owned list did not name `doc:20-ddd-practices`, the file this branch mostly edits.

The blocking one was the fourth: three documents still named the port as an unsuffixed
`Clock` — `doc:20` §2 and §4.1.3, ninety and two hundred and fifty lines from the §5.3
sentence this branch adds, and `doc:15` §7. The branch that exists to give the name one owner
left the old name in two sections of the owning document. Fixed here; `grep -rn '\`Clock\`'
documentation/` now returns one line, the sentence that discusses the unsuffixed form on
purpose.

#### A citation can rot in both directions

Criterion 15's evidence quoted the port row as *"Not the shared kernel, and not governed by
it."* — the row's bold sentence when the criterion was written. Fixing the "sibling" wording
rewrote that sentence to *"A subpackage of the shared kernel's package, and not a member of
the shared kernel."*, and the quote certifying it was not re-read. The substantive claim
never changed, so the criterion was met throughout; the quote was not a quote of anything.
`grep -rn "Not the shared kernel, and not governed by it" documentation/` returned zero.
Caught in verification of the corrected head, and fixed above.

**This pull request now contains one instance of each direction a citation fails.** The
`doc:00` precedence gloss was a claim asserted beyond what its source said — the writer
overstating a stationary target. This one is the reverse: an accurate quote whose target
moved underneath it, in the same change, by the same author, three findings later. The first
is caught by re-reading the source; the second is caught only by re-reading it *again*, at
certification, after every edit is final. Both are `doc:05-authoring-for-agents#one-fact-one-place`,
and the second is the harder one, because nothing about the sentence looks wrong — it is
well-formed, it is specific, and it was true when written.

The rule stack entry 3 carries — re-derive a claim against the artefact at the moment it is
certified, not at the moment it is believed — covers both. This is its fourth instance in
this bean and the only one where the claim was correct and the evidence still false.

#### The suffix argument, at its full strength

Stated in review and stronger than this bean first put it. `core-domain` may reference
`java.time` types, and an existing gate actively drives one in:

```
cmd:      sed -n '250,256p' architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt
observed: /**
           * Time is injected, never read from a static clock. The no-argument
           * overloads are banned; `Instant.now(clock)` and friends are exactly the
           * shape this rule is pushing code towards, so they stay legal.
           */
exit:     0
```

So `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` pushes `Instant.now(clock)` into
domain code, where `clock` is a `java.time.Clock`. An unsuffixed `Clock` port would stand
beside it in the same file, in the same expression. The gate is manufacturing the collision
the suffix avoids. §5.3 now says so.

**This changes what the ambient-ports bean must implement.** It is a sibling's bean, it is
not on `origin/main` yet, and it is not touched here; the orchestrator carries the ruling to
it. Its id is deliberately not written as a typed reference: `docs-lint` check 6 resolves
`bean:NNNN` against this tree and a bean living only on a sibling's branch resolves to zero
files, so the reference fails the gate. Observed on the first draft of this paragraph:
`FAIL check 6 … resolves to 0 files, expected exactly 1`. The second draft named the id in
prose and still failed, because the failure message it quoted contained the reference —
check 6 reads its own output back, which is the shape `bean:0061` records for check 14.

That is finding 1 observed first-hand, in a second mechanism, by an author who was in the
middle of writing finding 1 down: the parse — what `docs-lint` treats as a reference site —
failed while the decision was correct, and no test of check 6's verdict could have caught it,
because the input never reached the verdict in the shape the test supplied. `doc:35-testing`
§6 states the rule and cites the defensive-copy gate; it does not cite this, because
`doc:35-testing` is at 500 of its 500 lines and a second instance is not worth what it would
displace. The instance is here instead.

Since corroborated independently by the analyser work, and a bean is being raised for check 6
by the agent that owns `tools/docs-lint.sh`. Its id is not written here for the reason the
paragraph above gives. Two agents hitting one defect from different directions, neither
looking for it, is stronger than a plant: a plant proves the mechanism can fail, and this
proves it does.

