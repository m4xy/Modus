---
# modus-0103
title: A null result that does not mean what it appears to — git log, pre-merge state, and -S
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# A search whose store cannot hold the answer — `git log` and pre-merge state

Three agents independently concluded that a documented defect had never happened, because
`git log -S` did not find it. It had happened. It was caught in review and fixed before the
merge, so committed history never contained it — and the record was in `.beans/`, the store
this repository designates for exactly that.

**This is not a sourcing failure, and that is why it is raised separately.** Every party
re-derived the claim against evidence. Every party reasoned soundly from what came back. Better
habits about checking claims would not have caught it; the searches *were* the check. What was
wrong was the belief that the store being searched could hold the answer.

Three distinct ways a null `git log` result misleads are recorded below, found in this order
and each independent of the others:

1. **The store cannot hold the answer.** A pre-merge fix leaves no trace in committed history.
2. **The command searched less than was typed.** `git log -S` is last-wins; a second `-S`
   silently replaces the first.
3. **Recording the observation can invalidate it.** An `observed:` block asserting a string's
   absence puts that string into the searched corpus.

The first is the reason this is not a borrowed-authority problem. The second and third were
found while writing up the first, which is itself the argument for writing these up.

## Why this is not a fourth borrowed-authority mechanism

Those mechanisms are about a claim's **authority** — who vouched for it, and why the vouching
went unexamined. A claim inherits credibility from its author, its age, or its mere presence in
a file, and nobody re-derives it.

This is about a claim's **search space**. Authority is not involved: the claim *was*
re-derived, by three parties, against evidence. The defect is that the store searched cannot
represent the event being asked about, so a null result carries no information and was read as
though it carried a lot.

The two need different fixes. Borrowed authority is fixed by re-deriving rather than trusting.
That fix was applied here, three times, and produced the wrong answer three times — because
re-deriving against a store that cannot hold the answer is not re-deriving. **Folding this in
would blunt both**: it would imply the diligent path already covers this, when the diligent
path is precisely what failed.

## The mechanism, which is structural rather than incidental

A defect caught in review and fixed before the merge leaves **no trace in committed history, by
construction**. There is no commit in which the wrong value appears, because the branch was
corrected before it merged. Squash-merge makes this the **normal** case rather than an edge
one: intermediate states are collapsed and only the final tree survives.

So an empty `git log -S` result is consistent with two very different worlds:

| world | what `git log -S "X"` returns |
|---|---|
| `X` never existed | nothing |
| `X` existed, was caught in review, and was fixed properly | nothing |

**The command cannot distinguish them, and the second world is the one where the process
worked.** A well-run project produces more of the second, not fewer — so the better the review
culture, the more events are invisible to this search, and the more confidently wrong the
inference from its silence becomes. That is the finding.

## A second mechanism: the command silently searched less than was typed

Found after the above, and independent of it. The search that produced the null result was:

```
cmd:      git log --all -S "75_000_000" -S "outputPerMTok: 75" --oneline
```

Read as "find either string". It is neither. **`git log -S` is last-wins: a second `-S`
replaces the first rather than combining with it.** Verified with a control rather than from
the manual, because the whole subject of this bean is not trusting what a command appears to do:

```
cmd:      git log 10af4f7 -S "ZZZNOTHINGZZZ" -S "outputPerMTok" --oneline
observed: 10af4f7 feat(backoffice): scaffold the backoffice with a tokenised design system
cmd:      git log 10af4f7 -S "outputPerMTok" -S "ZZZNOTHINGZZZ" --oneline
observed: (no output)
```

Same two strings, order swapped, opposite answers.

**Pinned to `10af4f7` and counting nothing, and both choices are this bean's own mitigations
applied to itself.** An earlier revision ran this control with `--all | wc -l` and recorded
`observed: 4` and `observed: 0`. Both were wrong within a day — 8 and 1 when a reviewer ran
them, 9 and 2 when it was fixed — and the single commit the second form returned was **the one
that had recorded the observation**. So the control block reproduced, in miniature, both defects
this bean names: a count over a growing set, and an absence assertion falsified by writing it
down. It sat four sections below a table listing three mitigations and applied none of them.

Pinning to a commit is mitigation 2 from that table, and it is the right one here rather than
the pathspec: `10af4f7`'s ancestry is fixed forever, so this control cannot rot no matter what
lands later. Reporting a commit id rather than a count removes the quantity that could go stale
at all — the demonstration needs "one specific commit versus nothing", not "how many". Only the last `-S` is applied. So the
original command searched **only** `outputPerMTok: 75`; `75_000_000` was discarded silently —
no error, no warning, and **nothing in the output distinguishes "searched both, found nothing"
from "searched one"**.

This belongs in this bean because it is the same failure one level down. The first mechanism is
that the *store* cannot hold the answer. This one is that the *command* answered a narrower
question than the one asked. Both produce a confident null, and neither announces itself.

Either defect alone was sufficient for the wrong conclusion, which is worth noticing: the
conclusion was not marginal, it was **overdetermined**. A reviewer who caught the last-wins bug
and reran with a single `-S` would still have got nothing, and still been wrong — because the
pre-merge mechanism was the real one. Fixing the visible defect would have *increased*
confidence in the wrong answer.

**A flag that silently discards an argument is worse than one that errors**, because the failure
is indistinguishable from success. The general habit: when a null result is load-bearing,
falsify the *command* before trusting the *result* — run it against a string known to be
present, and confirm it comes back non-empty.

## The recording hazard: an observation invalidated by writing it down

A third property, and one with no obvious fix. The original evidence block asserted an absence:

```
observed: no output — the value appears in no commit on any ref
```

Re-run after that block was committed, the same command returns **the commit that recorded
it**. Writing the evidence down created the counter-example, because the searched string now
appears in the searched history — as prose, inside the bean documenting the search.

This is not a stale figure and not a moved target. It is **an observation invalidated by the
act of recording it**, and any `observed:` block asserting the absence of a string has the
property whenever the recording lands in the searched corpus.

Three mitigations, none free:

| mitigation | cost |
|---|---|
| scope the command away from the record — `-- backoffice/` | must be remembered at authoring time, and is wrong if the record ever moves into the scope |
| pin to a named commit rather than `--all` | accurate forever, but answers a narrower question than the reader usually wants |
| quote the string so the search cannot match it | fragile and obscure; the transcript stops being copy-pasteable, which `bean:0091` requires |

The first is used in *The instance, in full*, and the second in *A second mechanism*'s
control. Both are named rather than placed: an earlier revision said "the first is used
above" when it is used below, and "see the recording hazard below" when that section is
above — **both inverted by a scripted edit that inserted two sections and moved the ones
after them**, with the result unread. A cross-reference by direction is a claim about
layout; by name it survives the reordering. There is no general answer, and stating that
is better than pretending
otherwise.

## The set argues better than any of its members

Three beans were raised this sprint on adjacent failures of evidence. Each records at least one
figure that has since moved, and two of them return **their own commit** when their command is
re-run:

| bean | recorded | re-run | note |
|---|---|---|---|
| `modus-0100` | `returns 0` | **1** | returns `66fad7a`, the commit that recorded it |
| `modus-0100` | `return 4` | **8** | |
| this bean, before the fix above | `observed: 0` | **1** | returns whichever commit currently records this bean — see below |
| this bean, before the fix above | `observed: 4` | **8** | |
| `modus-0104` | script is `28 lines` | **32** | |
| `modus-0104` | `bean:0065` is `status: todo` | **in-progress** | conclusion unaffected; `in-progress` fails check 14's scope identically |

**No single bean makes this argument, and it is the strongest one available.** Each on its own
has an errant figure that reads as an ordinary slip — a number that drifted, a count taken on a
different day. Across the set the pattern is unmistakable and it is not carelessness: **six
figures, three authors' worth of care, one shared property — every one is a measurement of a
growing set, recorded as though it were a fact about the world.**

The two that return their own commit are the sharper half, because the failure is *caused* by
the recording. Writing `observed: 0` beside a command searching all history puts the searched
string into that history, so the observation is falsified by the act of publishing it. Both
beans state this hazard. Neither applied its own mitigation to its own control.

**That row names no commit id, and the reason is that an earlier draft of it did.** It read
"returns `d2d431a`, the commit that recorded the correction" — and `d2d431a` ceased to exist
about a minute later, when this bean was amended and force-pushed as a different sha. The
correction to the hazard was falsified by the hazard, inside the paragraph describing it, before
it was ever reviewed. **A commit id is a figure over a moving set exactly as a count is**, and
under `--amend` it moves faster than either. What is stable is the *property* — the command
returns whichever commit currently records this bean — and the property is what the finding
needed all along. The id added nothing but a hostage.

That is only visible from outside any one document, which is why it is recorded here rather
than in each: a per-bean note would read as an erratum, and the set reads as a mechanism.

## The mitigation was available and was reached for once in three chances

Worth more than any remedy the three beans propose, because it is not a proposal — it already
exists, it is cheap, and it was written down mid-sprint.

*The recording hazard* below tables three mitigations. Counting where each was applied:

| where | mitigation applied |
|---|---|
| this bean's **instance block** | **yes** — the `-- backoffice/` pathspec, and load-bearing: unscoped the same command returns four commits including this bean's own |
| this bean's **control block**, twenty lines earlier | **no** — until the fix above |
| `modus-0100`, throughout | **no** |

**One technique, three chances, one hit.** The technique was not missing, unclear or expensive:
it is a pathspec, it is written in the same document, and in the one place it was used it
demonstrably worked. It still was not reached for in the block twenty lines away.

So the useful finding is not that a mitigation is needed. It is that **a mitigation written into
the same document as the defect it prevents is not thereby applied** — knowing the technique and
reaching for it are separate acts, and only the first is what writing it down accomplishes. Any
remedy in this family that consists of documenting a practice should expect this hit rate.

## Why it is this repository's problem in particular

`adr:0005-evidence-lives-in-the-work-item#evidence-home` puts review findings in the work item.
So the class of defects invisible to committed history is **exactly the class this repository
documents most richly** — every review thread, with its before and after, in `.beans/`.

The store holding the answer was the one all three parties cite in every other paragraph. The
evidence was not merely elsewhere; **it was in the place the project designates for evidence**,
and the search went to the code store instead. This repository's strength and this blind spot
are the same fact seen from two sides.

## The instance, in full

`e2e/tests/agent-console.spec.ts` asserts that three models' session costs stand in the ratio
of their prices. Its docstring said the test had once caught Opus 5 priced at $15/$75.

Checked against committed history, that looks fabricated:

```
cmd:      git log --all -S "opus-5" --oneline -- backoffice/src/agent/transport.ts
observed: 10af4f7 feat(backoffice): scaffold the backoffice with a tokenised design system
cmd:      git show 10af4f7:backoffice/src/agent/transport.ts | grep -n "opus-5"
observed: 94:  'claude-opus-5': { inputPerMTok: 5, outputPerMTok: 25 },
cmd:      git log --all -S "outputPerMTok: 75" --oneline -- backoffice/
observed: no output
```

Exactly one commit has ever touched the line carrying the Opus 5 rate, and it introduced it at
$5/$25 — so the rate has never been changed in committed history at all. That is phrased as a
quantifier over the set rather than as a count of it, deliberately: **a claim quantified over a
set that grows is stale on arrival, while a claim about the set itself is not.** An earlier
revision of this block said "three commits"; it was six within a day, because this branch's own
commits touch that file.

The `-- backoffice/` pathspec is also deliberate — see *The recording hazard*.

It is not fabricated. `.beans/modus-0002--backoffice-foundation.md:130`, under
`## Review cycle 1` — "Six inline threads on PR #3, all six fixed":

```
cmd:      sed -n '130,144p;149,153p' .beans/modus-0002--backoffice-foundation.md
observed: ### 1. Pricing was 3x over on the console's default model

          `PRICING` claimed $15/$75 per MTok for `claude-opus-5`. The list price is
          $5/$25, so the headline cost figure on the cost-conscious screen read 3x high.
          [...]
          | Model | Was | Now | Note |
          | --- | --- | --- | --- |
          | `claude-opus-5` | 15 / 75 | **5 / 25** | wrong; the default model |
          | `claude-sonnet-4-5` | 3 / 15 | *removed* | two generations superseded |
          | `claude-sonnet-5` | — | **3 / 15** | introductory $2/$10 lapses 2026-08-31 |
          | `claude-haiku-4-5` | 1 / 5 | 1 / 5 | already correct |
          [...]
          *Load-bearing:* `agent-console.spec.ts` now runs the identical session on three
          models and asserts the cost ratios, which are exactly the price ratios — Opus 5
          is 5x Haiku 4.5, Sonnet 5 is 3x. Restoring 15/75 makes that ratio 14.95 and the
          test fails.
```

`bean:0002` is PR #3, whose commit is `10af4f7` — the same commit every party used as proof of
absence. Two details are worth keeping:

- **The test was written afterwards, as the guard.** A reviewer caught the error; the test is
  the regression barrier added in response. The original docstring's only real error was
  *mechanism* — "the test caught it" rather than "a reviewer caught it and this test exists so
  it cannot recur" — which is far smaller than fabrication, and makes the test's justification
  **stronger** than a self-referential assertion usually earns.
- **`bean:0002` pre-computes the failure**: "Restoring 15/75 makes that ratio 14.95 and the
  test fails." The bean states what the guard does if the defect returns. Anyone who read it
  had both the event and the guard's behaviour in one place.

### How close this came to being encoded backwards

The correction written in response to the null `git log` result said the incident **never
happened**. It reached two artefacts and was one edit from being committed — a false claim
replacing a true one, in a work item about unverified claims, carrying git output as proof. It
was stopped by a tree-wide `grep` that hit `bean:0002` incidentally.

That is the sharpest available statement of the risk: **this failure does not present as
uncertainty.** It presents as a well-evidenced correction, which is more durable than the error
it replaces, because the next reader sees a claim that has already been checked.

## What found it, which argues against triaging small work

A one-word tense fix. The date rolled over, making a KDoc's "goes stale in three days" wrong,
and re-reading that file to correct it surfaced the same claim in a **third** site after it had
been reported as living in two. The discrepancy triggered a tree-wide sweep instead of a
fix-list, and the sweep hit `bean:0002`.

Nothing about that task was important. It would not have been opened at all if the calendar had
not moved. **The sprint's best finding came out of its most trivial task**, which is an argument
against sorting work by apparent significance before doing it — and an argument for re-deriving
against the artefact rather than the list of places you remember touching, since such a list is
selected by the same belief it is meant to test (`bean:0091` reaches that rule from transcript
discipline).

## What to do about it — a habit, not a gate

**No mechanism is proposed, because none is proposed with confidence.** Saying that plainly is
better than inventing a check that would not have fired.

The honest form is a habit, and it is one sentence: **when asking whether an event occurred,
search `.beans/` before concluding anything from `git log`.** More generally — before treating
a null result as evidence of absence, establish that the store searched is capable of holding
the thing sought.

Why the obvious mechanisms are not proposed:

| candidate | why not |
|---|---|
| a lint flagging "never happened" claims | it would have to understand the claim. Nothing distinguishes a justified negative from this one except doing the search properly. |
| requiring a `.beans/` search alongside `git log` | unenforceable — nothing observes which searches an agent runs, and a rule nobody can check is `doc:00-constitution#mechanical-enforcement`'s definition of one that will keep being broken. |
| preserving pre-merge history | this is squash-merge working as intended; the fix would be worse than the defect. |

If a mechanism exists it is more likely a **habit written where agents read**, and that belongs
in `doc:80-agent-operating-procedure`, whose owner is not this bean.

## Success criteria and evidence

Numbered and tabled rather than bulleted, because `docs-lint` check 14b answers **per numbered
criterion**: against unnumbered bullets it collapses to a single aggregate judgement, so one
prose paragraph would satisfy this whole bean at closure. A family of beans exists to stop
exactly that, and this one shipped in the shape it warns about.

Evidence is empty by design while this is `todo` — the criteria describe follow-up work that
has not been done, and a cell filled now would be a plan, not an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | The distinction between a claim's **authority** and its **search space** is stated somewhere an agent reads, with this worked case, so the next null result is read as ambiguous rather than as proof | |
| 2 | The habit is recorded in whatever document owns agent search practice — one sentence, not a gate — or this bean records why no home was appropriate | |
| 3 | If a mechanism is proposed later, it is **observed failing** against a planted instance before it is claimed (`doc:00-constitution#observed-failing`), and observed **not** firing where the store searched can hold the answer. A check that would not have caught this one is not a fix for this | |

## On generality, stated as a limit

**This is one instance of the first mechanism.** It was found three times, by three parties,
which makes it a strong instance — the same wrong inference reached independently by everyone
who looked is evidence that the trap is in the situation rather than in one reader. It is still
one.

The second and third mechanisms are on firmer footing, because neither is an inference about a
past event: `-S` last-wins is demonstrated by a control that can be re-run in a second, and the
recording hazard is a property of the searched corpus rather than an observation about it. They
are stated as mechanisms, not as instances, and need no frequency behind them.

So this is a **named mechanism with one worked case**, not a rule with a frequency behind it.
What a second instance looks like is genuinely unknown: it may be another pre-merge fix, or
some other question asked of a store that cannot answer it — a `grep` of `main` for something
that only ever existed on a branch, or a search of merged documents for a decision taken in
review. The general form is *a null result from a store that structurally cannot hold the
answer*, and only the `git log`/pre-merge case has actually been seen.

Recorded now, at one instance, because the near-miss shows what it costs when it is not: the
wrong conclusion arrives dressed as the right one, with evidence attached.
