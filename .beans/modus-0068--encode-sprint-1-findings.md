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
| 1 | Across the defensive-copy gate's review rounds the defect was repeatedly in what the parser could see rather than in what the rules decided, and the rule's own test passed each round because its fixture supplied an enabling condition the real code omitted | `bean:0036`, `bean:0064` — **as relayed this said "seven rounds" and "every escape"; both are corrected below and neither is what the cited beans say** |
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
| 6 | `doc:50-memory-and-evidence` states the shapes in which a claim reads as verified and is not, in one place, uncounted, with the fabricated justification for a declined fix among them | citation |
| 7 | `doc:05-authoring-for-agents#one-fact-one-place` states that restating a removed fact in the same change reintroduces it, and that a pointer carrying content its target does not is an unowned rule | citation |
| 8 | `doc:00-constitution#observed-failing` binds a fix as it binds an `Enforced by:` line | citation |
| 9 | `doc:00-constitution` §9 states the allowlist-versus-requirement rule and the condition under which an allowlist is admissible | citation |
| 10 | `doc:35-testing#load-bearing-evidence` requires the plant to establish the claim's enabling condition, and separates a mechanism's input surface from its decision | citation |
| 11 | Every touched document is inside `docs-lint` check 8's budget, and `doc:00-constitution` is no longer than the 500 lines it had before this bean | command |
| 12 | `./gradlew qualityCheck` green on every branch in the stack | test-run |
| 13 | The ambient-capability port names have exactly one owning anchor, `doc:00-constitution` §1.3 defers to it, and the third port `doc:00` never named survives | citation |
| 14 | `doc:60-cost-model#spend-record` names the token kinds the recorder actually writes, and §2's model can price them | citation |
| 15 | `doc:20-ddd-practices` §5.1 has a row for the context-free port package, stating that it is not the shared kernel and does not inherit `adr:0004`'s membership gate | citation |
| 16 | Every package named in §5.1's tables carries the root and segment order this repository uses, and the section names the command that says which of them have members rather than answering it | command |
| 17 | The negative half of `doc:00-constitution#observed-failing` is written down somewhere, and where it could not go is recorded | citation |

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
`documentation/`"*. It was a gloss stated as if it were the clause — one of the shapes stack entry 3
adds to `doc:50-memory-and-evidence` §2.5, a citation that resolves without carrying the
claim made at it, committed inside the bean that encodes it, and caught in review. It is replaced
here rather than deleted, because a bad argument left where a later reader can cite it as
settled is the failure `doc:50-memory-and-evidence#primary-sources` exists to stop.

Three textual grounds, each checked against `origin/main` at `8181726`. They are keyed by
name rather than numbered: `docs-lint` check 14 reads a **numbered** table inside the Evidence
region as criteria evidence, so numbering the grounds 1-3 had them answering criteria 1-3,
which they have nothing to do with. That credit was redundant — those three are answered by
their own rows in the agent-loop section, and unnumbering changed the audit not at all
(`17 criteria checked, 0 unnumbered` either way) — but a construct that can answer a criterion
it is not about should not be left standing because it currently answers one that was already
answered.

| ground | statement | observed |
|---|---|---|
| deference | **`doc:00` already defers to an owning document against its own precedence line.** §1.1 ends: "The table in `10-architecture.md` §4.1 is the machine-readable form … If they disagree, §4.1 wins and this table is the bug." Ownership-over-precedence is a pattern the constitution already applies to itself | `git show origin/main:documentation/00-constitution.md` line 74 |
| incoherence | **Precedence produces an incoherent result.** §1.3 covers time and identifiers and never mentions randomness, so it cannot name a third port. Reading precedence as decisive yields `{Clock, IdGenerator, RandomPort}` — two names from one document and one from another, for three ports of one kind | same file, lines 86–87 |
| pre-existing | **The naming rule pre-existed and `doc:00` states nothing contrary.** §5.2's outbound-port row already gives `<Noun>Port` with `ClockPort` as its own example, and §7's prohibitions table already names `ClockPort` as the replacement for `Instant.now()` — both on `main`, before this change | `git show origin/main:documentation/20-ddd-practices.md` lines 311 and 430 |

The deference ground cites a clause that entry 4 of this stack deletes with §1.1's table. The deference
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
| 16 | root and segment order correct on every row; the section names the command rather than answering it | `grep -c "com\.modus"` → `0` in both `documentation/20-ddd-practices.md` and `documentation/10-architecture.md`. That grep proves the **root** and nothing more, which is why the criterion is worded to what it evidences: the `core-domain` rows match `grep -rhE "^package uk\." core/ \| sort -u`, and §5.1 now sends a reader to that command for which packages have members and to `doc:15-repository-layout#core-package-rules` §4.2 for which rules scope them, instead of listing either |
| 11 | budgets held | `doc:00` 500 of 500, the §1.3 rewrite being line-for-line; `doc:20` 500 of 500, having reached the ceiling exactly during the review fixes; `doc:10` 288 of 500 and `doc:15` 233 of 500, each one line longer than `main` and otherwise substitutions in place |
| 12 | the gate | the run below, on this branch |

```
cmd:      wc -l documentation/00-constitution.md documentation/10-architecture.md documentation/15-repository-layout.md documentation/20-ddd-practices.md
observed: 500 documentation/00-constitution.md
          288 documentation/10-architecture.md
          233 documentation/15-repository-layout.md
          500 documentation/20-ddd-practices.md
exit:     0

cmd:      ./gradlew qualityCheck                      (superseded — see the §5.1 note correction below)
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
          note: this is the run as made. The unfiltered form also returns one prose row
          from a comment, which is why §5.1 sends readers to `grep -rhE "^package uk\."`
          instead; the row did not affect what this run established.

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

That is a distinct failure from the shapes stack entry 3 encodes, and entry 3 now carries
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

#### A claim about what does not exist yet has an expiry date, set by another branch

The note this branch added to §5.1 listed which package rows had members and which stated an
intention. It was true when written, it was the correction review asked for, and it was
falsified by a branch that had not merged: `feat/ambient-capability-ports` creates
`uk.m4xy.modus.core.domain.port` and adds two rules scoping it, so both "Not yet:
`uk.m4xy.modus.core.domain.port`" and "the port, kernel and default rows not at all" become
false the moment it lands.

This is the fourth variant of one failure this bean has now catalogued. The first was a claim
asserted beyond a stationary source; the second, an accurate quote whose target moved; the
third, a criterion written against a document that did not exist yet. **This one no review
could have caught**, because at review time the statement was true and the thing that
falsifies it was on another branch. Only sequencing defends against it, and the only sound
sequence is that the change which creates the thing also corrects the claim.

The correction is not to update the list. **A normative section MUST NOT carry a snapshot of
repository state that another branch can invalidate**; it names where the answer lives
instead. §5.1 now sends a reader to `grep -rhE "^package uk\." core/ | sort -u` for which
packages have members, and to `doc:15-repository-layout#core-package-rules` §4.2 for which
rows a rule scopes and which of those rules exist, and states that a row is a placement rule,
never a claim that its package exists yet. That has no expiry, needs no edit when the ports
land, and is a line shorter, which is how it fits a document that was at 500 of 500.

Two edits inside one paragraph, and the second was made only after review. The sentence above
first named the unfiltered `grep -rh "^package " core/` and `ArchitectureRulesTest`'s package
constants — both artefacts this branch's own later corrections had already replaced, the first
because it returns a prose row from a comment and the second because
`doc:15-repository-layout#core-package-rules` §4.2 owns that fact. Criterion 16's evidence row
was updated for both; this narrative was not.

So the paragraph asserting **"a criterion has two rows in this bean and amending one is
amending half of it"** was itself amended in one place and left stale in another, one screen
below where it says so. The rule generalises past the criteria table: **every restatement of a
change is a row of it.** The evidence cell, the narrative, the pull-request body and the commit
message are four copies of one fact, and `doc:05-authoring-for-agents#one-fact-one-place`
binds them exactly as it binds two documents — with the extra hazard that these four are
written in one sitting, which is what makes them feel like one act rather than four.

**The evidence cell went with it, and only on the second pass.** Criterion 16's row in the
Success-criteria table was amended in the same commit as the note; its row in the Evidence
table was not, so for one push the bean carried the old criterion text in one column and the
new one in another — one fact in two places, disagreeing — with an evidence cell certifying
"§5.1's note names the five rows that have no members", which is exactly what the change
deletes. That is this bean's instance #2, an accurate quote whose target moved, committed
inside the change that adds instance #4. A criterion has **two** rows in this bean and
amending one is amending half of it.

Criterion 16's second clause was rewritten with it. It required "the rows with no members are
named as having none", which is the snapshot this correction removes — a criterion that would
have certified the defect. The criterion now requires the section to name the command rather
than answer the question, and this bean is `in-progress`, so amending it is an edit and not an
`## Amendments` entry (`doc:00-constitution#bean-lifecycle`).

```
cmd:      wc -l documentation/20-ddd-practices.md
observed: 499 documentation/20-ddd-practices.md
exit:     0

cmd:      git diff --name-only origin/main
observed: .beans/modus-0068--encode-sprint-1-findings.md
          documentation/20-ddd-practices.md
exit:     0

cmd:      ./gradlew qualityCheck
observed: docs-lint: OK — 19 documents, 107 anchors, 1098 references, 77 beans, 37 graph
          edges, 25 selectable, 77 bean ids, 0 introduced, 77 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 17s
tree:     this branch rebased onto origin/main at 2c958e4
exit:     0
```

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

**This changes what the ambient-ports bean must implement.** It is a sibling's bean and is
not touched here; the orchestrator carries the ruling to it. It has since merged, so
`bean:0065` resolves today — but it did not when this was written, and the record of that is
the point. Its id was deliberately not written as a typed reference: `docs-lint` check 6 resolves
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

#### Check 6 and check 13 have opposite blind spots, and a bean id passes through both

The paragraph above turns on check 6 resolving `bean:NNNN` against **this tree**, so it cannot
see a bean that exists only on a sibling's branch. Check 13's id-uniqueness condition resolves
against **`origin/main`**, so it cannot see one either — for the mirror-image reason. Put
together, an id allocated on two concurrent branches is unique as far as every mechanism can
tell: both branches lint green, and they stay green until the second one merges. **The first
merge is what creates the collision, and nothing looks again after it.**

That happened here. This branch raised its KDoc bean as `0105`; `#45` had already pushed a
different `0105` to its own branch. Both were green. It is renumbered to `0108`, verified
absent from every ref and from all history rather than from `origin/main` alone:

```
cmd:      git log --all --diff-filter=A --name-only --pretty=format: -- '.beans/*010[5-9]*' | sort -u
tree:     before the rename, with `modus-0105--a-kdoc-…` still the path this branch added
observed: .beans/modus-0105--a-kdoc-asserts-two-domain-rules-that-do-not-exist.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          .beans/modus-0106--the-evidence-extractor-reads-only-table-cells.md
          .beans/modus-0107--bean-0103-states-a-count-where-its-own-paragraph-argues-for-a-quantifier.md
exit:     0

cmd:      git log --all --name-only --pretty=format: | grep -c "modus-0108"
tree:     the same moment, before any commit carrying `modus-0108` existed
observed: 0
exit:     0
```

**Neither fence reproduces on the tree that ships them, and both fail in ways this bean
already names.** The second is self-referential in the strict sense: the commit that carries
the assertion is the commit that puts `modus-0108` into `git log --all --name-only`, so
re-running it here returns a non-zero count *because the claim was acted on*. The first
asserts a history that no longer exists — the branch was rewritten by the rename, so no commit
on any ref now adds `modus-0105--a-kdoc-…`, and `0108`, `0109` and `0110` are present and
unlisted; read today it refutes the claim it is offered for.

They are stamped rather than re-quoted because **what they record is a moment, not a
property**: the question "was `0108` free" is only answerable before `0108` was taken, and a
re-quote against the shipped tree would print numbers that answer a different question. That
is the fourth class this bean separates — reproducible, non-reproducible, externally
dependent, and now **spent**: a check whose own success destroys the condition it verified.
An allocation check is spent by construction, and stamping the moment is the only honest form
it has.

`bean:0051` owns parallel id allocation and prescribes allocating against `origin/main`; that
is the right rule and it is not sufficient, because `origin/main` is precisely the set that
omits every unmerged sibling. **A bean id is allocated against a set no single agent can see.**

**Neither mechanism nor review caught it.** Both branches' reviewers verified their ids "across
all refs" and both were right when they looked; the id became non-unique only when a sibling
pushed. It was caught because one party held both branches in view at once, which is a property
of the orchestration and not of the repository. That is the honest status: today, concurrent id
allocation is safe only to the extent that someone is watching all of it, and `bean:0051`'s
detect-rather-than-prevent residual is what that costs.

Since corroborated independently by the analyser work, and a bean was raised for check 6 by
the agent that owns `tools/docs-lint.sh` — `bean:0086`, which has since merged and so resolves
here; when this was written it did not, for the reason the paragraph above gives. Two agents hitting one defect from different directions, neither
looking for it, is stronger than a plant: a plant proves the mechanism can fail, and this
proves it does.

### `docs/encode-sprint-1-agent-loop` — the agent loop

| # | criterion | observed |
|---|---|---|
| 1 | report-as-you-go is stated to every agent, and the brief cites it | `documentation/80-agent-operating-procedure.md`, the `## Reporting` section carrying `<a id="report-as-you-go"></a>`, placed outside the orchestrator-only step 0, with an *Implementing* and an *Orchestrating* bullet; brief item 5 under *What a brief must contain*, `**When to report** — as each finding lands, never only at the end (\`#report-as-you-go\`)`, which states no rule of its own |
| 2 | the name list is required before every push, with the shapes that make it necessary | `documentation/80-agent-operating-procedure.md`, step 5's rule 5.9, naming `git diff --name-only origin/main`, `git reset --soft origin/main` and the force-push, and "a path you did not intend to touch is a stop" |
| 3 | a closure needing evidence is a work item; criteria are walked against this run | `documentation/80-agent-operating-procedure.md`, two rules in step 6's `### Rules` list, "Closing a bean records observations that have already been made. If one has not, the closing change is the work of making it" and "An earlier run measured a different tree, whatever it printed" |
| 4 | the three orchestration anti-patterns, each with why it is a failure of the role | `documentation/80-agent-operating-procedure.md`, `### Anti-patterns, each reported from one sprint` in step 0, three rows: merge order chosen against a gate, a queued action stated as fact, an id allocated without a work item |
| 11 | budgets held | `doc:80` at 491 of 500; `doc:00-constitution` untouched by this branch |
| 12 | the gate | the run below, on this branch |

```
cmd:      wc -l documentation/80-agent-operating-procedure.md
observed: 491 documentation/80-agent-operating-procedure.md
exit:     0

cmd:      git diff --name-only origin/main
observed: .beans/modus-0068--encode-sprint-1-findings.md
          documentation/80-agent-operating-procedure.md
exit:     0

cmd:      ./gradlew qualityCheck
observed: docs-lint: OK — 19 documents, 108 anchors, 1114 references, 77 beans, 37 graph
          edges, 28 selectable, 77 bean ids, 0 introduced, 77 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 17s
tree:     this branch rebased onto origin/main at bd9da18
exit:     0
```

Re-derived after `bean:0068`'s base branch merged as `97f13b0`. The name list was four paths
and is now two: `doc:00-constitution` and `doc:20-ddd-practices` are on `main` and are no
longer this branch's diff.

`68 on origin/main` and `0 introduced` are the **externally dependent** fields of that line —
they count a ref this branch does not contain, and both moved when the base merged. They are
quoted against the named sha above and are not reproducible for a later reader; the rest of
the line is, and was re-run to prove it.

### `docs/encode-sprint-1-claims` — claims

| # | criterion | observed |
|---|---|---|
| 5 | §2.2 rejects the four unsourced shapes | `documentation/50-memory-and-evidence.md` — four rows added to *What is explicitly not evidence*: a figure with no command, a count with no command and no tree, arithmetic over a table of figures, and a citation that resolves without carrying its claim |
| 6 | the shapes are named in one place and uncounted, the fabricated decline among them | `documentation/50-memory-and-evidence.md` §2.5, `The shapes in which a claim reads as verified` — a marked derived table of shape/tell/rule which states that the set "is deliberately not counted, here or in the heading", closing on why the invented reason for a declined fix is the worst of them; `documentation/80-agent-operating-procedure.md` step 9 resolution 3 cites that anchor rather than restating it |
| 7 | §3 states both authoring rules | `documentation/05-authoring-for-agents.md` — "Removing a copy and restating it in the same change is how the duplication comes back", and "A pointer that carries content its target does not is an unowned rule", worked against `bean:0058` |
| 11 | budgets held | the `wc -l` below, each of 500 |
| 12 | the gate | the run below, on this branch |

Line numbers are deliberately absent from criteria 5, 6 and 7. They were `:111`, `:150` and
`:128` when first written, and the rebase onto `97f13b0` moved every one of them. A line
number is a **rebase-dependent** figure — a fourth class beside the three below, and the
cheapest fix is not to cite one where the anchor and the quoted sentence already locate the
claim.

```
cmd:      wc -l documentation/05-authoring-for-agents.md documentation/50-memory-and-evidence.md documentation/80-agent-operating-procedure.md
observed: 308 documentation/05-authoring-for-agents.md
          439 documentation/50-memory-and-evidence.md
          493 documentation/80-agent-operating-procedure.md
exit:     0

cmd:      git diff --name-only origin/main
observed: .beans/modus-0068--encode-sprint-1-findings.md
          documentation/05-authoring-for-agents.md
          documentation/50-memory-and-evidence.md
          documentation/80-agent-operating-procedure.md
exit:     0

cmd:      ./gradlew qualityCheck
observed: docs-lint: OK — 19 documents, 109 anchors, 1212 references, 83 beans, 37 graph
          edges, 33 selectable, 83 bean ids, 0 introduced, 83 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 19s
tree:     this branch rebased onto origin/main at 161a7c3
exit:     0
```

### `docs/encode-sprint-1-gate-design` — gates

| # | criterion | observed |
|---|---|---|
| 8 | `observed-failing` binds a fix | `documentation/00-constitution.md` §9.1 — "The rule binds a **fix** as it binds a gate: a fix nothing can be observed to protect is not yet enforced", a fourth bullet in §9.1's list |
| 9 | the allowlist rule and its admissible case | `documentation/00-constitution.md` §9.1, the paragraph after that list — "Enumerating the shapes a gate accepts fails open; requiring the token that settles the question fails closed", closing on the one set an allowlist may bind over |
| 10 | the input surface is separated from the decision, and the plant covers the enabling condition | `documentation/35-testing.md` §6, a bullet: "What a mechanism perceives is a separate subject from what it decides, and takes its own tests… Plant the claim's *enabling condition*, not only the claim" |
| 11 | `doc:00-constitution` did not grow | 500 of 500, unchanged from `main`, which already carries the §1.3 rewrite this stack's base landed. `doc:35-testing` likewise |
| 17 | the negative half is written down | `documentation/50-memory-and-evidence.md` §2.2 gains the row "A mechanism observed firing, never observed silent" — three observations, not one — and the paragraph on a retained "best" value corrupting the state it compares against, with the count assertion that is the only thing that catches it. It belongs at `doc:00-constitution#observed-failing` beside the positive half and could not go there; `bean:0089` records that as the second casualty of the ceiling |
| 12 | the gate | the run below, on this branch |

The retention instance was **reported, not reproduced** — it is a live detector on PR #45,
`fix/per-request-usage-vocabulary`, which this agent does not own and did not run
(`doc:80-agent-operating-procedure#reports-are-evidence`). The rule it produced is general
and stands on its own; the instance is attributed rather than claimed. Verified only that the
pull request exists and is open: `GITHUB_TOKEN= gh pr view 45` → `state=OPEN`,
`headRefName=fix/per-request-usage-vocabulary`.

`doc:00-constitution` §9.1's superseded "and in the pull-request body" clause is corrected
here too, at **zero line cost** — the phrase sat inside one line and striking it left one
line. `bean:0089` had claimed that correction was impossible at 500 of 500 and has withdrawn
the claim: the wall binds what must be **added**, not what can be **struck**, and pricing a
correction before declaring it unaffordable is one subtraction.

The wall this branch hit while making that room is raised as `bean:0089`, with the pinned-anchor
count measured rather than estimated, per `doc:80-agent-operating-procedure#encode-the-learnings`:
something noticed and not fixed is a work item. Its measurement was taken before `bean:0091`,
`bean:0095` and `bean:0098` reached `main`, so its pinned-set figure is a snapshot against the
sha it names, and criterion 2 of that bean already requires re-measurement at the time of the
fix rather than reuse of this one.

```
cmd:      wc -l documentation/00-constitution.md documentation/35-testing.md documentation/50-memory-and-evidence.md
observed: 500 documentation/00-constitution.md
          500 documentation/35-testing.md
          450 documentation/50-memory-and-evidence.md
         1450 total
exit:     0

cmd:      ./gradlew qualityCheck
observed: docs-lint: OK — 19 documents, 109 anchors, 1300 references, 89 beans, 37 graph
          edges, 39 selectable, 89 bean ids, 5 introduced, 84 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
tree:     this branch rebased onto origin/main at 9c9940d
exit:     0
```

This block and the criterion-12 block of the section below quote **one run**, because they
stamp one tree. They had come to carry two figure sets — `1263 references, 84 beans` here
against `1290, 88` there — under the same `tree:` line, and one command on one commit cannot
print both: the lower set was left over from a tree that had stopped existing. The run above
is on the final tree of this branch, transcribed into both places from the one terminal.

Two things moved these figures while the branch was open, and neither is `main` growing
underneath it: `#53` merged the bean its own branch introduced, moving one id out of this
branch's introduced set and onto `origin/main`; and the review round that produced this
version raised `bean:0111`, which adds a bean to the tree and to that set. Both are legible
in the line above, so neither is restated here.

The `BUILD SUCCESSFUL in Ns` line is dropped from this block. A duration does not reproduce —
this branch has printed 17s, 19s, 20s and 22s for the same command on the same tree — so
pasting one makes the block unverifiable for a reason that has nothing to do with the claim.
**Quote what reproduces, not what merely got printed.**

Room was made rather than taken. §1.1's dependency table was a self-declared prose
rendering of `doc:10-architecture#module-dependencies` §4.1; it is deleted, and the two rows
§4.1 did not carry — `backoffice/` and `e2e/` — moved into §4.1 rather than being dropped.
In `doc:35-testing`, two closing recaps and one clause duplicating
`doc:00-constitution#observed-failing` were removed (`doc:05-authoring-for-agents#prose-ban`).

#### Two figures promoted into normative documents without checking the beans beside them

Row 1 of the provenance table above was relayed as "seven review rounds" and "every escape
entered through the parser's input surface". Both went into `doc:35-testing` §6. A third,
"held under direct attack", went into `doc:00-constitution` §9.1. All three were wrong, and
the artefacts that refute them are the two beans cited in the same row.

| claim as written | what `bean:0036` and `bean:0064` say |
|---|---|
| seven review rounds | six. `bean:0036:690` — "the defect **these six rounds** exist to close"; `bean:0064:12` — "after **six** review rounds"; the last round heading is *Review round six* |
| **every** escape entered through the input surface | round six had four escapes and **one** was a parse bug — `bean:0036:598` says so in terms, and X3, `get() = held.register().toList()`, is a rule-logic gap that passed the round-four rule. The bean's claim is per **round**: "the fourth consecutive round in which the defect was in what the parser could see rather than in what the rules decided" |
| the requirement held under **direct attack** | it was *introduced* as X4's fix in the final round, so nothing attacked it afterwards. `bean:0036:628` is prospective — "cannot be walked around by writing an expression the list does not name" — and `bean:0064` §2 records it **misfiring** on `internal fun isFrozen() = frozen` |

**The rules survive; only the evidence sentences were wrong.** X4 alone establishes the
input-surface rule, and the requirement genuinely replaced three allowlists that each failed
open and genuinely fails closed. So the cost was not a false rule — it was three unearned
strengtheners, each making a true claim sound better than its evidence.

**This is the sharpest instance in the bean, because of where it happened.** Every other
figure on this branch was checked against source — the ArchUnit predicate, the fourteen-row
count, the package roots. These three came from a relay, went into the provenance table, and
were promoted from there into two normative documents without anyone re-opening the beans
sitting in the citation column beside them. A provenance table is not a source; it is a
record of what was said, and the source is the artefact it names. **Two paragraphs away, this
same branch corrects an over-claim in `doc:15` §4.2 for exactly this reason.**

#### A relocation is a deletion plus an addition, and only the deletion is guaranteed

That deletion was safe **only if** §4.1 already carried every rule §1.1 stated. Two did not
survive, and review found both. `core:core-application` lost its `Kotlin stdlib` ALLOW, so
the surviving table denied the standard library to a core module. `modules:*` lost its
`app:*` DENY, so §4.1 — which calls itself the source table an ArchUnit test is derived from
— understated `rule:archunit/modulesDoNotDependOnAdaptersOrApp`, whose predicate is
`resideInAnyPackage(ADAPTERS, APP)`, the predicate at `ArchitectureRulesTest.kt:132` under the
val at `:126`. The only document
stating the modules-to-app prohibition was the one being deleted. Both rows are restored
above.

**The failure is in what was checked, not in the care taken.** The two rows §4.1 visibly
lacked — `backoffice/` and `e2e/` — were transferred deliberately and verified. The rows
assumed already present were never enumerated, so the check ran over exactly the subset that
could not fail it. That is this branch's own input-surface rule
(`doc:35-testing#load-bearing-evidence`) arriving in prose rather than in code: the
verification's *input* was selected by the belief it was meant to test.

**The check is mechanical and takes a minute.** Enumerate the deleted rows, enumerate the
destination's rows, diff the sets; do not re-read the destination for the ones you remember
moving. Run here over all nineteen MAY/MUST-NOT pairs the deleted table stated:

```
cmd:      python3 - <<'EOF'
            t = open('documentation/10-architecture.md').read()
            i = t.index('### 4.1'); sec = t[i:t.index('\n## ', i)]
            checks = [                       # every MAY/MUST-NOT pair the deleted table stated
              ("core-domain MAY Kotlin stdlib",      "| `core:core-domain` | Kotlin stdlib | ALLOW |"),
              ("core-domain MAY java.time types",    "| `core:core-domain` | `java.time.*` (types only, no `now()`) | ALLOW |"),
              ("core-domain MUST NOT (catch-all)",   "| `core:core-domain` | anything else | DENY |"),
              ("core-application MAY core-domain",   "| `core:core-application` | `core:core-domain` | ALLOW |"),
              ("core-application MAY Kotlin stdlib", "| `core:core-application` | Kotlin stdlib | ALLOW |"),
              ("core-application MAY coroutines",    "| `core:core-application` | `kotlinx.coroutines` | ALLOW |"),
              ("core-application MUST NOT (catch-all)", "| `core:core-application` | anything else | DENY |"),
              ("adapters MAY core",                  "| `adapters:*` | `core:core-domain`, `core:core-application` | ALLOW |"),
              ("adapters MAY third-party",           "| `adapters:*` | `org.springframework.*`, its own third-party libs | ALLOW |"),
              ("adapters MUST NOT other adapters",   "| `adapters:*` | another `adapters:*` | DENY |"),
              ("adapters MUST NOT modules/app",      "| `adapters:*` | `modules:*`, `app:*` | DENY |"),
              ("modules MAY core",                   "| `modules:*` | `core:core-domain`, `core:core-application` | ALLOW |"),
              ("modules MAY spring/third-party",     "| `modules:*` | `org.springframework.*`, its own third-party libs | ALLOW |"),
              ("modules MUST NOT another module",    "| `modules:*` | another `modules:*` | DENY |"),
              ("modules MUST NOT adapters+app",      "| `modules:*` | `adapters:*`, `app:*` | DENY |"),
              ("app MAY everything",                 "| `app:modus-server` | any Gradle module | ALLOW |"),
              ("backoffice MAY contract",            "| `backoffice/` | the REST API contract, over HTTP | ALLOW |"),
              ("e2e MAY running system",             "| `e2e/` | the running system, over HTTP | ALLOW |"),
              ("backoffice/e2e MUST NOT Kotlin",     "| `backoffice/`, `e2e/` | any Kotlin source | DENY |"),
            ]
            lost = [n for n, r in checks if r not in sec]
            print("LOST:", len(lost), lost)
          EOF
observed: LOST: 2 ['core-application MAY Kotlin stdlib', 'modules MUST NOT adapters+app']
          re-run after restoring both rows:
          LOST: 0 []
exit:     0
```

The script is written out rather than elided. A fence reading `python3 - <<'EOF'` with the body
removed is a figure whose command cannot be re-run — `doc:50-memory-and-evidence` §2.2's first
row, in the bean that adds it. The enumeration is also the part worth checking: the conclusion
depends entirely on the nineteen pairs being the right nineteen, and only the list shows that.

Its first run reported all nineteen lost, because the section slice terminated on the table's
own `|---|` separator. A check reporting total failure is as untrustworthy as one reporting
total success; this one was wrong in the safe direction by luck, not by design.

**A relocation MUST be verified as a set difference, never by re-reading the destination.**
`doc:05-authoring-for-agents#one-fact-one-place`'s fix pattern — "replace every other copy
with a reference" — is a relocation every time it is applied, so this binds it.

Two corrections were made in passing, both in the family this bean is about:

- `doc:35-testing` §6 required the evidence "in the pull-request body's `verify` block",
  which `adr:0005-evidence-lives-in-the-work-item#evidence-home` moved to the work item. The
  document had been left behind by the ADR that governs it.
- `doc:00-constitution` §1.3 and `doc:15-repository-layout` §4.2 both over-claimed the time
  ban. Verified against source rather than relayed:
  `architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt:310`
  — `timeIsInjectedNeverReadFromAStaticClock` is `noClasses().should().callMethod(Instant,
  "now").orShould().callMethod(LocalDate, "now").orShould().callMethod(LocalDateTime,
  "now")`, and `callMethod` with no parameter types matches the zero-argument overload only.
  Three of the six methods the `NoAmbientTime` row names are reached at all, and those three
  only in that one overload; the rule binds the whole repository rather than `core-domain`,
  because it carries no `.that()`.

  **That citation was `:258` until review caught it, and the rebase is what moved it.**
  `bean:0065` added **52** lines above the rule — the val moved `258` → `310`, and the change
  deleted nothing — so `:258` now lands in KDoc prose. It added 167 lines to the whole file;
  that figure was relayed to me, I wrote it into this paragraph as "above the rule", and 167
  is true of the file while only 52 explains the move. **It landed in the paragraph whose own
  thesis is that a count beside a fact being corrected is invisible**, three lines from the
  sentence stating it, in the round that added that sentence.

  The `:258` citation itself was checked against source when written and was true then. It is
  the rebase-dependent class this bean already records for its own line numbers — and it
  survived a pass in which I corrected the `tree:` sha in the same fence, because I
  re-derived the figure the rebase was visibly about and not the citations the same rebase
  had moved. **A rebase invalidates every line number in the change, not the ones you are
  thinking about.**

  **I did it twice more while fixing it.** The first rebuild of §4.2's gap paragraph opened
  "Three further rows are reached in part" and closed "Five of these rows" — two counts, both
  wrong, inside the paragraph whose thesis is that the section should give no count. The rows
  are named now. A second draft then recorded that fact *in the document*, which review
  rejected: an uncommitted draft cannot be checked against any ref, so a normative document
  asserting one is unverifiable by construction. The rule stays in `doc:15`; the instance is
  here, which is where process that never reached a commit can legitimately live.

  `doc:15`'s gap paragraph
  also said "five of the thirteen rules in §4.2" over a table that has fourteen rows
  (`awk '/^### 4.2/,/^### 4.3/' documentation/15-repository-layout.md | grep -c '^| \`'` →
  `14`); the count is deleted rather than corrected, per
  `doc:05-authoring-for-agents#one-fact-one-place`.

  **And it came back, which is a sharper fact than it first looked.** While this branch
  waited, `bean:0065` landed and its author rewrote that sentence as "four of the thirteen
  rules". The numerator changed and was right both times; **`thirteen` is byte-identical
  across the two versions.** The table has had fourteen rows at every commit that has ever
  touched the file, so `thirteen` never went stale — it was wrong on arrival, and then
  survived a deliberate rewrite of its own sentence by an author who was actively editing the
  numbers standing next to it.

  That is a stronger argument than the one this paragraph first made. A stale count at least
  announces itself by drifting; this one could not drift, because it was never right, and the
  one act most likely to catch it — rewriting the sentence it sits in — did not. **A count
  beside a fact being corrected is invisible precisely because the corrector's attention is
  on the fact.** Deleting it is not tidiness, it is the only intervention the evidence
  supports.

### `docs/spend-record-behind-its-recorder` — the spend record was behind its own recorder

Routed in by the orchestrator mid-task and **verified here rather than relayed**, because a
relayed figure is how a false one becomes load-bearing (§2.2 of `doc:50-memory-and-evidence`,
this bean's own criterion 5).

| # | criterion | observed |
|---|---|---|
| 14 | the record names what is written, and the price book can price it | `documentation/60-cost-model.md` §3.2 now names `cacheRead`, `cacheWrite5m` and `cacheWrite1h` and says why the split exists; §3.2.1's "all four token kinds" is gone; §2.1 gains the rule that an entry prices every kind, not input and output alone; §3.2's `Enforcement gap:` now names `bean:0111`, having named two beans that cannot close it |
| 11 | budget held | `doc:60-cost-model` 473 of 500 |
| 12 | the gate | the run below, on this branch |

```
cmd:      ./gradlew qualityCheck
observed: docs-lint: OK — 19 documents, 109 anchors, 1300 references, 89 beans, 37 graph
          edges, 39 selectable, 89 bean ids, 5 introduced, 84 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
tree:     this branch rebased onto origin/main at 9c9940d
exit:     0
```

The duration is omitted for the reason the gates section gives: it does not reproduce, and a
line that cannot be re-derived makes the block around it look unverifiable. The counts are the
gates section's counts, from the same run on the same tree, and the two figures that moved
since the previous run moved for the reason given there.

What was checked, and what was found to differ from the relay:

```
cmd:      sed -n '116,122p' tools/cost_lib.py
observed: USAGE_KINDS = (
              "inputTokens",
              "outputTokens",
              "cacheReadTokens",
              "cacheWrite5mTokens",
              "cacheWrite1hTokens",
          )
exit:     0

cmd:      grep -n "cacheWriteTokens" tools/cost-record.py
observed: 255:        "cacheWriteTokens": usage["cacheWrite5mTokens"] + usage["cacheWrite1hTokens"],
          344:                           "outputTokens", "cacheReadTokens", "cacheWriteTokens",
exit:     0

cmd:      grep -n "MULT" tools/cost_lib.py
observed: 34:CACHE_READ_MULT = (1, 10)  # 0.1x
          35:CACHE_WRITE_5M_MULT = (5, 4)  # 1.25x
          36:CACHE_WRITE_1H_MULT = (2, 1)  # 2x
          79:        "cacheRead": inp * CACHE_READ_MULT[0] // CACHE_READ_MULT[1],
          80:        "cacheWrite5m": inp * CACHE_WRITE_5M_MULT[0] // CACHE_WRITE_5M_MULT[1],
          81:        "cacheWrite1h": inp * CACHE_WRITE_1H_MULT[0] // CACHE_WRITE_1H_MULT[1],
exit:     0
```

The multipliers are sourced in `tools/cost_lib.py`'s own comment to the `claude-api` skill,
`shared/prompt-caching.md:141`, read at CLI 2.1.236 on 2026-08-29 — not to this document,
which deliberately refuses to carry them (§2).

**Corrected against the relay.** `cacheWriteTokens` is still written, as the sum of the two
halves, so the record carries the four kinds §3.2 named *plus* the split — not five instead
of four; the second hit, `:344`, is the self-test's required-field list, so the folded field
is checked for as well as written. And the relayed corpus figures (861,927,115 tokens, cache reads 97.51%) are not
what this tree holds. Measured here instead, and the command is given so the difference is
attributable rather than mysterious:

```
cmd:      python3 - <<'EOF'
            import json
            ks = ("inputTokens", "outputTokens", "cacheReadTokens",
                  "cacheWrite5mTokens", "cacheWrite1hTokens")
            t = {k: 0 for k in ks}; n = 0
            for line in open('domains/modus/cost/0001.ndjson'):
                line = line.strip()
                if not line: continue
                d = json.loads(line); n += 1
                for k in ks: t[k] += d.get(k, 0) or 0
            tot = sum(t.values())
            print(f"records {n}  total {tot:,}")
            for k in ks: print(f"{k:18}{t[k]:>12,} {100*t[k]/tot:6.2f}%")
          EOF
tree:     origin/main at 8181726, domains/modus/cost/0001.ndjson
observed: records 2  total 249,780,821
          inputTokens              1,226   0.00%
          outputTokens           627,369   0.25%
          cacheReadTokens    248,029,474  99.30%
          cacheWrite5mTokens     116,944   0.05%
          cacheWrite1hTokens   1,005,808   0.40%
exit:     0
```

That script was elided to `for line in open(...): ...` until review caught it — **in the same
commit that added the rule against eliding a script**, about forty lines apart. The rule was
written for the §4.1 set-difference fence and applied there, not to the fence already sitting
further down the same file. A rule added to a document does not retroactively audit the
document it is added to, and nothing prompts a sweep, because the new text feels like the
whole of the change.

**Then writing it out exposed something the rule as first stated does not cover.** The
de-elided body did not produce the `observed:` block above it: `print("records", n, "total",
tot)` emits one space and an unformatted integer, and `f"{k:22} …"` pads two columns wider
than the output shows. Six lines of six differed. The measurement was never in doubt — the
numbers reproduce exactly from the cited tree — and the script was not wrong either; the
**transcription** of it into the fence was, and had been for as long as the fence was elided.
For that whole period **nobody could tell, because there was nothing anyone could run.**
De-eliding ended the period and produced the failure in the same act.

So elision does not merely make a fence unrunnable. **It conceals that the fence is already
broken**, and de-eliding is the act that surfaces it. An elided command cannot be falsified,
which makes it the one kind of evidence that never decays visibly: it reads the same on the
day it is right and every day after it stops being. The corrected body now reproduces the
block byte-for-byte.

This bean's criteria table is audited end to end for the first time on this branch, by
flipping it to `completed` and running check 14. The same flip on every earlier head in the
stack does **not** produce a per-criterion audit: on three of them a table header outside
check 14's vocabulary trips `NOEVCOL`, which suppresses the whole cascade, and on the fourth
the check names one criterion whose evidence is written and sitting on the next branch.
`bean:0101` carries all five runs and the correction of an earlier, invented one.

The relay is itself finding 6, arriving while finding 6 was being written down: a figure
travelled one hop from the tree that produced it, arrived with no command and no tree, and
would have been copied into a document had it not been checked. It is recorded here at the
orchestrator's own suggestion, because the finding is worth more with a live instance than
without one, and the instance cost nothing but the checking.

Both measurements support the same conclusion — the cache-read term dominates and fresh
input rounds to zero — and they disagree on every digit, because they were taken on
different trees. That is why neither number is written into `doc:60-cost-model`: a figure
that moves with every recorded run is a drift generator wherever it is copied
(`doc:05-authoring-for-agents#one-fact-one-place`). The rule went in; the number stayed here.

One half of the relay did **not** hold on checking. `doc:60-cost-model` §2 does not omit
cache pricing by oversight: it states that the multipliers "must not be written from memory"
and sends the reader to the `claude-api` skill, which is exactly what `tools/cost_lib.py`
did. The real gap was narrower and structural — the price-book entry shape had two rates
where the record has five kinds — and that is what §2.1 now states.

