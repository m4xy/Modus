---
# modus-0105
title: The negative half of observed-failing is normative nowhere
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# The negative half of `observed-failing` is normative nowhere

`doc:00-constitution#observed-failing` requires that a mechanism be watched rejecting a planted
violation. It does not require that the same mechanism be watched **not** firing on input that
does not violate anything, and it does not say why that second observation matters.

So a mechanism that fires on **every** input satisfies §9.1 exactly as written. It has been
observed rejecting a planted violation — it rejects everything — and the `Enforced by:` line it
earns is true in the letter and worthless in the fact. The rule as written cannot tell that
mechanism apart from a working one.

## What §9.1 actually requires

Quoted rather than paraphrased, because the whole finding is about the difference:

```
cmd:      sed -n '/^### 9.1 /,/^\*\*A gate can be real/p' documentation/00-constitution.md
observed: ### 9.1 A gate is unverified until it has been observed failing <a id="observed-failing"></a>

          > **A mechanism nobody has watched reject a real violation is not enforcement. It is a
          > claim.**

          - Every `Enforced by:` line MUST name a mechanism that has been observed rejecting a
            planted violation of the rule it claims to enforce. The observation is recorded
            verbatim (§3), in the work item and in the pull-request body.
          - The procedure is `35-testing.md` §6, applied to gates rather than to tests: plant,
            observe the named mechanism fail, revert.
          - A mechanism that cannot be made to fail MUST be demoted to an `Enforcement gap:` naming
            the work item that closes it. An unfalsifiable gate is worse than an admitted gap,
            because it also stops anyone looking.

          **A gate can be real, correct, observed failing — and still not run.** `docs-lint` check 11
```

The range is anchored on the section heading and on the first line after the normative bullets,
rather than on line numbers, so it does not rot when the document moves; the last line is the
terminator and is the start of a separate finding, not part of the rule.

Three bullets, one direction each, and all three are about the mechanism saying *no*. Nothing
about the clean case, and nothing about the count.

`doc:35-testing#load-bearing-evidence` §6 is the procedure §9.1 delegates to, and for *tests*
it does carry a weak form of the other direction — step 1 records that the test passes on
unmodified source and step 4 records that it passes again after the revert. Two things follow,
and they cut in the same direction:

- §9.1's restatement for **gates** compresses that procedure to "plant, observe the named
  mechanism fail, revert" and the `Pre:`/`Post:` observations do not survive the compression.
  A gate is the case where firing-on-everything is most plausible and least visible.
- Neither document states the **reason**. `Pre:`/`Post:` read as bookkeeping around the real
  step. What makes the clean observation load-bearing is not symmetry, it is that *"observed
  failing" is satisfied by a mechanism that always fails*, and nothing in either document says
  so. A reader who follows the letter has done what is asked.

## The adjacent requirement that does exist, and why it makes this one look covered

§9.1's closing prose asks for something in the neighbourhood: *"make the run say what it
examined, because a check that examines nothing and a check that passes both print `OK`."* That
is a **vacuity** assertion, and `bean:0086` reads it as exactly that — its evidence records the
reference count on the `docs-lint` `OK` line as "the vacuity assertion
`doc:00-constitution#observed-failing` asks for", and records that the count did not distinguish
its plant from its control.

Vacuity and the negative half are different questions and neither implies the other:

| question | asks | failure it catches |
|---|---|---|
| vacuity | did the mechanism look at anything? | a check that skipped its input and printed `OK` |
| the negative half | does the mechanism accept input that does not violate the rule? | a check that rejects everything, including what is correct |

A mechanism can examine plenty and still reject all of it. `bean:0069`'s detector examined every
frame of every message and fired on most of them, for one fault. So the vacuity requirement is
satisfied by exactly the mechanism the negative half would reject, and its presence in §9.1 is
part of why the absence reads as coverage: a reader who checks that the run says what it
examined has done something real, in the right area, that does not answer this.

## Why the count is part of the same gap

The clean-input observation is the obvious missing half. There is a second one, and `bean:0069`
found it the expensive way: a mechanism can fire **the right number of times for the wrong
reason**, or the wrong number of times for the right one.

Its frame-disagreement detector reported a single planted fault N−1 times, because the value it
compared against was its own retained "best" frame rather than the source — so after the first
disagreement every later frame of that message disagreed with the retention. It fired on a
planted violation. §9.1 was satisfied. The count was an artefact of the mechanism and not a
property of the stream, and the only thing that caught it was an assertion that pinned the
count to exactly one.

That is the same gap seen from the other end. "Observed failing" asks whether the mechanism can
say *no*. It does not ask whether the mechanism says *no* only when it should, or once per
thing that is wrong. All three questions are cheap to answer at the moment the plant is being
made, and only the first is required.

## Why this is raised rather than fixed in the pull request that found it

The rule currently exists in two places, both of them unmerged work items, and both of them
this sprint's:

| where | form |
|---|---|
| `bean:0069`, in its detector evidence | three observations required of the detector: fires on a plant, fires exactly once, does not fire on a clean run |
| `bean:0090`, in its success criteria | "A comparator that fires on every run is not coverage, and one that never fires has not been shown to work" |

Those two are the whole of it. The sweep behind that, so the next reader does not have to
re-derive it:

```
cmd:      grep -rl 'not firing\|does not fire\|fires on every\|never fires' .beans documentation | sort
observed: .beans/modus-0068--encode-sprint-1-findings.md
          .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          .beans/modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
          .beans/modus-0090--constants-that-must-match-an-authority.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          documentation/80-agent-operating-procedure.md
```

Files, not lines, because what is being established is *where the rule is stated*, and reading
decides that; a count of matches would answer a different question. The `| sort` is part of the
command rather than something done to its output — `grep -rl` emits in traversal order, which is
filesystem-dependent, so without it this block reproduces as a set and not as a byte stream.

Every hit outside the table above was read, and none of them states the rule. `doc:80`'s line is
about choosing a merge order so that a gate does not fire; `bean:0068` and `bean:0086` narrate
specific mechanisms that did not fire, which is a report and not a requirement. `modus-0105` is
this file, in the corpus by construction — the assertion sits inside what it searches, so the
list grows whenever a bean discusses the subject, and the reading rather than the list is what
carries the claim.

**Two unmerged beans agreeing is a convention between them, not a rule.** Neither is normative;
neither is discoverable from `documentation/`; a bean written next week reads §9.1, does what it
says, and is correct to. The two authors of the convention are the two who already know it,
which is the population it does not need to reach.

`documentation/` is owned by another agent this sprint and §9.1 is byte-identical on `main` and
on the branch that found this, so no edit was made from either. Raising the bean is the whole of
what the finding supports: the job is to get the negative half into §9.1, and the job needs an
owner rather than a third bean stating it in prose.

## What the amendment has to settle, stated as the open questions

Not drafted here, because the wording is the work and two of these are genuine decisions:

- **Is the clean-input observation MUST or SHOULD?** Some gates have no meaningful clean input
  to observe — a check that examines a diff has nothing to run against when there is no diff,
  which is `doc:00-constitution#observed-failing`'s own `docs-lint` check 11 story.
- **Where does the count belong?** "Fires exactly once per fault" is not always well defined,
  and a rule that demands it universally will be ignored where it is not.
- **Does it change `Enforced by:` or the `verify` block?** §9.1 puts the planted observation in
  both the work item and the pull-request body. The negative observation is cheaper to record
  and easier to omit, so where it is written determines whether anyone notices its absence.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and
a cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | `doc:00-constitution` §9.1 states the negative half — a mechanism must be observed not firing on input that does not violate the rule — with the reason, not only the requirement | |
| 2 | The three open questions above are each answered in the amendment or explicitly deferred in it, so a reader knows which of them the rule covers | |
| 3 | `bean:0069` and `bean:0090` cite §9.1 for the negative half instead of stating it, so the rule has one home (`doc:05-authoring-for-agents#one-fact-one-place`) | |

## Scope, stated as a limit

This is a gap in a rule, not an instance of a rule being broken. No mechanism in the tree is
known to fire on every input; the two that were checked were checked because the authors
happened to hold the convention, which is exactly what makes the gap invisible from inside.
Nothing here estimates how many `Enforced by:` lines would survive the stronger rule, and
finding out is part of the work rather than a claim this bean makes.
