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

The second is the more generalisable: §3's existing evidence is six facts that **drifted
apart**, and drift is what makes a duplicate visible. Three consistent copies of one wrong
belief drift not at all, so the section's own failure mode does not detect them.

`doc:20-ddd-practices` and `doc:00-constitution` are both at the 500-line ceiling.
`doc:80-agent-operating-procedure` is at 498, which is not room for either passage.
`doc:05-authoring-for-agents` is at 417 and owns both subjects, so both land there and
nothing is compressed to fit.

`bean:0066` is left untouched, deliberately, on two grounds. It is `in-progress` and its
closure is the orchestrator's next act (`doc:00-constitution#bean-lifecycle`), so editing it
here risks a conflict on a file this branch does not own. And the split is already the one
`doc:05-authoring-for-agents#bean-split` §5 prescribes: the bean records **what was observed**
— a doubt flagged and shipped, one belief corrected at three sites — and `doc:05` states the
rule derived from it. Promoting the rule does not make the observation a duplicate of it.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | §4.1.8 carries an `Enforcement gap:` line naming a bean, and `doc:20-ddd-practices` is still within its 500-line budget | **MET.** The line names `bean:0163` and states what an unchecked declaration lets through: *"an implementation that declares nothing, or declares the mode it is not, fails no build"*. `wc -l documentation/20-ddd-practices.md` → **500**, unchanged, because §4.1.8 is a single-line table row and adding to its text costs no line. `docs-lint` check 8 green, and check 6 resolves `bean:0163` against the tree |
| 2 | `bean:0161` criterion 4 requires a failure on a zero parse, not a report | **MET.** It now reads *"The check **fails** when it parses zero modules, observed by planting a `settings.gradle.kts` whose `module(...)` calls the parser cannot see"*, with the count kept as a diagnostic beside the gate. The reason is recorded in the bean's prose beside the rule it protects, citing `bean:0051`'s inert check 11 |
| 3 | Both findings from `bean:0066` are stated in `doc:05-authoring-for-agents`, in the sections that own their subjects, and are not compressed to fit a budget | **MET.** §3 gains restatement's second cost — consistent copies of a wrong belief produce no drift, so the signal every instance in §3's own evidence table was found by is absent — plus the ranking rule that the costliest site is the type other authors implement. §5 gains "flagging a doubt is not narrowing it" and the rule it rests on: the author of a rule is the worst-placed reader of its scope. `wc -l` → **450** of 500, so nothing was shortened to fit |
| 4 | `./gradlew qualityCheck` green | **MET.** Transcript below |

```
docs-lint: OK — 19 documents, 111 anchors, 1869 references, 128 beans, 47 graph edges, 62 selectable, 128 bean ids, 2 introduced, 126 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
docs-lint-gate-test: 168 passed, 0 failed, over 2 bash major version(s).

> Task :qualityCheck

BUILD SUCCESSFUL in 1m 32s
182 actionable tasks: 7 executed, 175 up-to-date
```

No plant is recorded against criteria 1 to 3: this change adds no mechanism. Criterion 1
**records** the absence of one, and `bean:0163` is where a mechanism is built and observed
failing. Saying so is the point rather than an omission — `doc:00-constitution#observed-failing`
asks that an unfalsifiable gate be demoted to an admitted gap, and that is the whole content
of this change.
