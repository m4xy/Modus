---
# modus-0105
title: The negative half of observed-failing is normative nowhere
status: completed
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
updated_at: 2026-09-03T00:00:00Z
---

# The negative half of `observed-failing` is normative nowhere

> **Closed as superseded, on the day it was raised, by `9c9940d`.** The title is false and
> is retained deliberately: this file is the record of a claim that was believed, acted on,
> and falsified, and a closed bean is that record rather than a statement of current fact
> (`adr:0005-evidence-lives-in-the-work-item#finalisation`).
>
> The condition this bean names — that the negative half of `observed-failing` is stated
> nowhere normative — was fixed in `doc:50-memory-and-evidence` §2.2 before the bean was
> worked. The evidence is under "Success criteria and evidence" below. The sections between
> here and there are the finding as it was written; they are still true of §9.1 itself, and
> the two things this close does **not** settle are named under "What this close leaves
> open, and who has it".
>
> The file is **not renamed.** The slug is irrelevant to reference resolution — check 6
> globs on the id — but `modus-0109` and `modus-0068` are merged on `main` and cite both of
> this file's paths in prose, one of them to record the id collision that produced them.
> Renaming would falsify live text on `main` in order to correct a title.

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
            verbatim (§3), in the work item (`adr:0005-evidence-lives-in-the-work-item#evidence-home`).
          - The procedure is `35-testing.md` §6, applied to gates rather than to tests: plant,
            observe the named mechanism fail, revert.
          - A mechanism that cannot be made to fail MUST be demoted to an `Enforcement gap:` naming
            the work item that closes it. An unfalsifiable gate is worse than an admitted gap,
            because it also stops anyone looking.
          - The rule binds a **fix** as it binds a gate: a fix nothing can be observed to protect is
            not yet enforced (`bean:0064`, `doc:35-testing#load-bearing-evidence`).

          **Enumerating the shapes a gate accepts fails open; requiring the token that settles the
          question fails closed.** Three successive allowlists on the defensive-copy gate were each
          walked past by an expression nobody had named. The requirement that replaced them fails closed —
          *a non-private function mentioning a backing field MUST declare a return type* — and costs nothing
          in `core-domain` today, being broader than collections (`bean:0036`, `bean:0064`). An allowlist
          binds only over a set the tool enumerates exhaustively — a resolved classpath (`doc:35-testing#unit-classpath`).

          **A gate can be real, correct, observed failing — and still not run.** `docs-lint` check 11
```


The range is anchored on the section heading and on the first line after the normative bullets,
rather than on line numbers, so it does not rot when the document moves; the last line is the
terminator and is the start of a separate finding, not part of the rule.

**The anchoring worked and the transcript still had to be recaptured.** `9c9940d` added a
fourth bullet and a paragraph *inside* the range, so the block above is a re-run against
`6fbf0e0` and not the one this bean was raised with. An anchored range survives the document
**moving**, which is what it was chosen for, and not the document **changing**, which is a
different property and was never claimed. The earlier capture also quoted "in the work item
**and in the pull-request body**", a clause `9c9940d` struck; a reader checking §9.1 against
that block would have found the quotation wrong with nothing to say which half was stale.

Four bullets now, one direction each, and all four are about the mechanism saying *no* — the
one added binds a **fix** rather than a gate and does not change the direction. Nothing about
the clean case, and nothing about the count.

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

## Where the rule is stated, and what the sweep can and cannot tell you

When this bean was raised the rule existed in two places, both of them unmerged work items,
and both of them this sprint's:

| where | form |
|---|---|
| `bean:0069`, in its detector evidence | three observations required of the detector: fires on a plant, fires exactly once, does not fire on a clean run |
| `bean:0090`, in its success criteria | "A comparator that fires on every run is not coverage, and one that never fires has not been shown to work" |

**That is no longer the whole of it, and it stopped being so the same day.** `9c9940d`
merged on 2026-08-30 and put the rule in `doc:50-memory-and-evidence` §2.2, in the table of
what is not evidence, under the row *"A mechanism observed firing, never observed silent"*:

```
cmd:      grep -o 'the claim needs \*\*three\*\* observations rather than one[^.]*\.' documentation/50-memory-and-evidence.md
observed: the claim needs **three** observations rather than one: the mechanism fires on the planted fault, it fires the expected *number* of times, and it is silent on the unmodified source.
```

All three observations, the reason beside them — *"Enforcement is discrimination"* — and a
pointer to `doc:00-constitution#observed-failing` for the positive half. So the negative half
**is** normative and **is** discoverable from `documentation/`; `bean:0089` records why the
two halves could not be put in one document; and this bean's premise is spent.

### The sweep, re-run on the tree that closes this

Recorded because it is what a reader will re-run first, and because what it demonstrates is
not what it was written to demonstrate:

```
cmd:      grep -rl 'not firing\|does not fire\|fires on every\|never fires' .beans documentation | sort
observed: .beans/modus-0068--encode-sprint-1-findings.md
          .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          .beans/modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
          .beans/modus-0089--anchors-cited-by-completed-beans-pin-a-document.md
          .beans/modus-0090--constants-that-must-match-an-authority.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          .beans/modus-0110--dispatching-a-review-and-an-edit-against-one-head.md
          .beans/modus-0112--a-sweep-for-a-wording-read-as-a-sweep-for-a-rule.md
          documentation/80-agent-operating-procedure.md
```

Nine files where the original block had six. `bean:0089` and `bean:0110` arrived with the
two merges this branch was rebased over, and `bean:0112` is this close's own output; none of
the three states the rule as a requirement. `bean:0089` narrates this same gap and records
that its remedy went to `doc:50` rather than to §9.1; `bean:0110`'s line is about choosing a
merge order so that a gate does not fire, which is `doc:80`'s sense of the phrase and not
this one; `bean:0112` is about the sweep. `bean:0068` and `bean:0086` narrate specific
mechanisms that did not fire, which is a report and not a requirement. Read on its own, the
longer list says exactly what the shorter one said.

**`documentation/50-memory-and-evidence.md` is not in it, on any of the three trees this
sweep has been run against.** It is the one file that settles the question, and the sweep
cannot see it: §2.2 writes the rule as *"Firing on every input is also firing"* and *"never
observed silent"*, and the four patterns match neither.

```
cmd:      grep -c 'not firing\|does not fire\|fires on every\|never fires' documentation/50-memory-and-evidence.md
observed: 0
```

**That defect is `bean:0112`'s and is not restated here.** It generalises past this bean — a
match-based search offered as evidence about where a rule is stated fails silently and in
one direction — and it must not live in a record that may not be edited again.

Two properties of the block are worth keeping beside it, because both were right and both
are load-bearing on the close. The `| sort` is part of the command rather than something
done to its output: `grep -rl` emits in traversal order, which is filesystem-dependent, so
without it this block reproduces as a set and not as a byte stream. And `modus-0105` is this
file, in the corpus by construction — the list grew to nine because raising `bean:0112`
about the sweep put a ninth file into what the sweep searches. That was written as a caveat
when the number was six. It reads better as the first sign that the sweep was measuring its
own corpus rather than the rule.

## What this close leaves open, and who has it

Named rather than carried, because a closed bean cannot own work
(`adr:0005-evidence-lives-in-the-work-item#finalisation`):

- **No edge from the half a reader reaches first.** §9.1 is unchanged and still requires
  only the planted violation, and nothing inside its range points forward to
  `doc:50-memory-and-evidence` §2.2. `bean:0089` records why — the pointer costs a line the
  file does not have at 500 of 500 — and owns the `adr:0003`/`adr:0005` decision that
  unblocks it. One rule, two documents, no forward edge.
- **`bean:0069` and `bean:0090` state the rule rather than citing it.** That was a convention
  between two unmerged beans when this was written. It is now a
  `doc:05-authoring-for-agents#one-fact-one-place` duplication against a live document. Both
  beans now say so in place and cite §2.2; collapsing them to a bare citation is not done
  from here.
- **The instrument.** `bean:0112`.

## The three questions §2.2 does not bound

`doc:50-memory-and-evidence` §2.2 states the requirement and the reason. It does not bound
any of these, so they are still open — recorded here for whoever revisits the rule, and
owned by nobody:

- **Is the clean-input observation MUST or SHOULD?** Some gates have no meaningful clean input
  to observe — a check that examines a diff has nothing to run against when there is no diff,
  which is `doc:00-constitution#observed-failing`'s own `docs-lint` check 11 story.
- **Where does the count belong?** "Fires exactly once per fault" is not always well defined,
  and a rule that demands it universally will be ignored where it is not.
- **Does it change `Enforced by:` or the `verify` block?** §9.1 puts the planted observation in
  both the work item and the pull-request body. The negative observation is cheaper to record
  and easier to omit, so where it is written determines whether anyone notices its absence.

## Success criteria and evidence

**The criteria are those of the close, not of the amendment this bean was raised to ask for.**
That amendment was never written from here and must not be claimed: `9c9940d` satisfied the
condition first, in a different document, and the honest record of a bean overtaken before it
was worked is a close whose criteria are the ones the close actually meets.

| # | criterion | evidence |
|---|---|---|
| 1 | The requirement that a mechanism be observed **not** firing on input that does not violate the rule is stated normatively in `documentation/`, with the reason and not only the requirement | `cmd`: `grep -o 'the claim needs \*\*three\*\* observations rather than one[^.]*\.' documentation/50-memory-and-evidence.md` -> `the claim needs **three** observations rather than one: the mechanism fires on the planted fault, it fires the expected *number* of times, and it is silent on the unmodified source.` The reason sits beside it in the same §2.2 row — *"Firing on every input is also firing. Enforcement is discrimination"* — so the cell answers "with the reason" and not only "stated". `cmd`: `grep -o 'A mechanism observed firing, never observed silent' documentation/50-memory-and-evidence.md` -> `A mechanism observed firing, never observed silent`, the §2.2 row heading, which places the rule in the table of what is **not** evidence rather than in loose prose. Written without the surrounding table pipes on purpose: a `\|` inside a Markdown cell is alternation in a basic regular expression, so a command carrying one is corrupt the moment a reader pastes it (`bean:0069` found that the expensive way) |
| 2 | What this bean would still have wanted — an edge from the half a reader reaches first to where the rule now lives — is owned by an open work item rather than lost in this close | `cmd`: `grep -n '50-memory-and-evidence' documentation/00-constitution.md` -> exactly two hits, `19:` in the `depends_on` front matter — which `doc:05-authoring-for-agents#one-fact-one-place` says is not a reading list — and `129:`, inside §3. **Both section claims here carry the `awk` that fixes them**, because the two errors this cell has already shipped were both underived section numbers and nothing would have caught either. `cmd`: `awk 'NR>=100 && NR<=150 && /^#/{print NR": "$0}' documentation/00-constitution.md` -> `113: ## 3. The evidence rule [...]` and `139: ## 4. Investigate; do not ask`, so §3 spans 113 to 138 and 129 is inside it; an earlier revision of this cell said §4. `cmd`: `awk 'NR>=398 && NR<=463 && /^#/{print NR": "$0}' documentation/00-constitution.md` -> `400: ### 9.1 [...]` and `462: ## 10. The UI is a deliverable, not an afterthought`, with no heading between, so §9.1 spans **400 to 461**; an earlier revision said 400 to 423 — the terminator of the `sed` range quoted earlier in this file, which stops on a bolded paragraph *inside* §9.1. **A quoted extract's extent is not its section's**, and reading 423 as the end excludes the "a gate can be real, correct, observed failing — and still not run" material, which is the half of §9.1 most relevant to this bean's subject. Neither hit falls inside §9.1, so no forward pointer exists. Asserted positively rather than as `no output`, because an absence claim is falsified by its own record the moment the string appears (`bean:0069`). `cmd`: `grep -o '^status: .*' .beans/modus-0089--anchors-cited-by-completed-beans-pin-a-document.md` -> `status: todo`. `bean:0089`'s own body states the constraint and names the two ADRs a remedy must choose between, so the gap is owned and open, which is what this criterion asserts — **not** that it is fixed |
| 3 | The instrument this bean used to establish the gap is disposed of, not left in a record that may not be edited again | `cmd`: `grep -c 'fires on every' documentation/50-memory-and-evidence.md` -> `0`. `cmd`: `grep -c 'Firing on every' documentation/50-memory-and-evidence.md` -> `1`. One pattern of the sweep's four, and its twin differing only in case and inflection: that pair is the entire mechanism of the false negative, and it is citable in a cell where the four-way alternation is not. The full four-pattern run and the nine-file sweep are in the prose above. Raised as `bean:0112`, which carries the general shape, the two captures and the criteria; this bean states the instance and stops |

## Scope, stated as a limit

As raised, this was a gap in a rule and not an instance of a rule being broken. No mechanism in the tree is
known to fire on every input; the two that were checked were checked because the authors
happened to hold the convention, which is exactly what makes the gap invisible from inside.
Nothing here estimates how many `Enforced by:` lines would survive the stronger rule, and
finding out is part of the work rather than a claim this bean makes.
