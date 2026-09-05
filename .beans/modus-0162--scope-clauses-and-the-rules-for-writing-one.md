---
# modus-0162
title: §4.1.8's unenforceable clause, and promoting two rule-authoring findings
status: in-progress
type: fix
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# §4.1.8's unenforceable clause, and promoting two rule-authoring findings

Three follow-ups from PR #83's approval. They are one change because they are one subject:
a rule that says more than anything can check, and the two authoring findings that produced
it.

## 1. §4.1.8's second clause has no enforcement and no gap note

`doc:20-ddd-practices#domain-events` §4.1.8 requires every implementation of the dispatch
port to **state which delivery mode it is**. Nothing parses KDoc, so nothing verifies it.

This is the milder of the two failures the rule has had — behaviour an implementation *can*
exhibit but nothing checks, rather than behaviour an asynchronous one *cannot* exhibit, which
was the over-scoping PR #83 corrected. The remedy the repository already uses is one
sentence, and the same PR applied it correctly to §4.1.7 twenty lines above and omitted it
here.

`doc:README#conventions`: *"If a MUST has no `Enforced by:` line, it carries a
`Enforcement gap:` line naming the work item that will close it."*

## 2. `bean:0161` criterion 4 describes where it must decide

As written, criterion 4 requires the module-enumeration check to **report** how many modules
it parsed. A reported number nobody asserts on is a diagnostic, not a gate: a run that parses
zero prints its zero and exits 0, which is `bean:0051`'s inert `docs-lint` check 11 exactly —
it printed `- introduced` rather than `0 introduced` and nobody was reading either.

The criterion must require the check to **fail** on a zero parse. This is the same
describe-versus-decide confusion as item 1, from the same author in the same pull request,
which is why it is worth fixing in the bean's text rather than leaving to whoever implements
it.

## 3. Two authoring findings are recorded where no rule-author will read them

`bean:0066` recorded both under "what this bean did not do". A bean is read by its reviewer
and then by nobody; `doc:05-authoring-for-agents` is read by everyone who writes a rule.

| finding | promoted to |
|---|---|
| Flagging a doubt about a rule's scope is not narrowing it | `doc:05-authoring-for-agents#bean-split` §5 |
| One belief restated at three sites had to be corrected at three, and the three agreed, so nothing could see it was wrong | `doc:05-authoring-for-agents#one-fact-one-place` §3 |

The second is the more generalisable. Where restated copies **drift apart**, the disagreement
is itself the signal; three consistent copies of one wrong belief drift not at all, so that
signal is absent. §3's existing evidence contains both shapes — row 4 is three documents
asserting a `@Disabled` check that cannot exist, agreeing perfectly — so `bean:0066` is the
pattern's second instance and not its first. An earlier draft of this bean said §3's six rows
were all facts that drifted apart; they were not, and criterion 3's evidence cell records the
correction.

`doc:20-ddd-practices` and `doc:00-constitution` are both at the 500-line ceiling.
`doc:80-agent-operating-procedure` is at 498, which is not room for either passage.
`doc:05-authoring-for-agents` is at 417 and owns both subjects, so both land there and
nothing is compressed to fit.

`bean:0066` is left untouched, **on the lifecycle ground alone**. It is `in-progress`, its
closure is the orchestrator's next act (`doc:00-constitution#bean-lifecycle`), and a
documentation branch editing an in-flight bean it does not own courts exactly the `.beans`
conflict this repository has already been bitten by.

An earlier draft of this note also argued the split was already the one
`doc:05-authoring-for-agents#bean-split` §5 prescribes — bean records the observation,
document states the rule. That is overstated: `bean:0066` does not confine itself to what was
observed, it generalises, so a sentence of the rule now lives in two places. The duplication
is real but inert, and its disposal is a one-line amendment at closure converting that
generalisation into a reference to `doc:05-authoring-for-agents#bean-split` — the
orchestrator's act, not this branch's.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | §4.1.8 carries an `Enforcement gap:` line naming a bean, and `doc:20-ddd-practices` is still within its 500-line budget | **MET.** The line names `bean:0163` and states what an unchecked declaration lets through: *"an implementation that declares nothing, or declares the mode it is not, fails no build"*. `wc -l documentation/20-ddd-practices.md` → **500**, unchanged, because §4.1.8 is a single-line table row and adding to its text costs no line. `docs-lint` check 8 green, and check 6 resolves `bean:0163` against the tree |
| 2 | `bean:0161` criterion 4 requires a failure on a zero parse, not a report | **MET.** It now reads *"The check **fails** when it parses zero modules, observed by planting a `settings.gradle.kts` whose `module(...)` calls the parser cannot see"*, with the count kept as a diagnostic beside the gate. The reason is recorded in the bean's prose beside the rule it protects, citing `bean:0051`'s inert check 11 |
| 3 | Both findings from `bean:0066` are stated in `doc:05-authoring-for-agents`, in the sections that own their subjects, and are not compressed to fit a budget | **MET.** §3 gains restatement's second cost — copies that agree produce no drift signal — citing row 4 of its own table as the prior instance and `bean:0066` as the second, plus the ranking rule that the costliest site is the type other authors implement. An earlier draft claimed **every** instance in that table was found by two copies disagreeing; three of the six were not, and the paragraph now records that the table's third column states what was wrong rather than how it was found, so no row of it can evidence a discovery mechanism. §5 gains "flagging a doubt is not narrowing it" and the rule it rests on: the author of a rule is the worst-placed reader of its scope, with the obligation to name the unbuilt member **in the rule's text or the PR body**, so a reviewer can tell a discharged obligation from a skipped one. `wc -l` → **464** of 500, so nothing was shortened to fit |
| 4 | `./gradlew qualityCheck` green | **MET.** Transcript below |

```
docs-lint: OK — 19 documents, 111 anchors, 1873 references, 128 beans, 47 graph edges, 62 selectable, 128 bean ids, 2 introduced, 135 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
docs-lint-gate-test: 168 passed, 0 failed, over 2 bash major version(s).

> Task :qualityCheck

BUILD SUCCESSFUL in 1m 32s
182 actionable tasks: 7 executed, 175 up-to-date
```

Re-taken from the shipped tree. An earlier revision recorded `1869 references`, measured
before the last content edit — the same family of defect as a filtered run recorded as a full
one, and smaller only because the figure was near-right. **The reference count moves with the
edit that records it**, so the transcript above was substituted and `docs-lint` re-run to
confirm it is a fixed point. It has taken three rounds so far — `1871`, then `1872` when this
very paragraph first cited a bean, then `1873` when the review round below added two more
citations. A transcript that counts the corpus it lives in is true only once nothing further
is written into that corpus, so it is the last thing to record and it is re-measured after
every subsequent edit, this sentence included.

Two of its figures are **not** properties of this branch and will read as stale without being
wrong: `2 introduced` and `135 on origin/main` are both relative to `origin/main`, which moves
whenever a sibling merges. `135` was `126` when this branch was cut. The `BUILD SUCCESSFUL`
timing and task counts likewise depend on configuration-cache state (`bean:0065`).

**Most of this is already a rule, and finding that out is the useful part.**
`doc:50-memory-and-evidence#corpus-figures` §2.7 third bullet owns the self-measurement half
outright — *"A record that measures a corpus it belongs to changes that corpus"* — and
prescribes the sentinel-and-diff procedure whose "re-run and diff" step is exactly what caught
the non-convergence above. Nothing here needs writing for that; it needed reading.

The residue is one bullet's worth and is **not** covered: a sentinel makes a figure neutral
against the author's own edit, and no sentinel stabilises a figure measured against a moving
external ref, because a sibling's merge changes it after the author has left. §2.7's second
bullet says that of a corpus *sweep* and not of a figure inside a pasted transcript, where
§2.6 forbids silent trimming. **If this recurs, it lands as a bullet on
`doc:50-memory-and-evidence#corpus-figures` §2.7 permitting a figure relative to a moving ref
to be elided as `[...]` under §2.6's marking rule** — the elision form already exists, and
only the permission is missing. One instance, so under
`doc:05-authoring-for-agents#bean-split` §5 it stays an observation here until there is a
second.

No plant is recorded against criteria 1 to 3: this change adds no mechanism. Criterion 1
**records** the absence of one, and `bean:0163` is where a mechanism is built and observed
failing. Saying so is the point rather than an omission — `doc:00-constitution#observed-failing`
asks that an unfalsifiable gate be demoted to an admitted gap, and that is the whole content
of this change.
