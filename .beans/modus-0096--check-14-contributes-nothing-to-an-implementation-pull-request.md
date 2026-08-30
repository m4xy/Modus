---
# modus-0096
title: Check 14 contributes nothing to any implementation pull request, by rule, and a green docs-lint line there says nothing about evidence
status: todo
type: task
priority: high
created_at: 2026-08-30T00:00:00Z
---

# Check 14 contributes nothing to any implementation pull request, by rule, and a green docs-lint line there says nothing about evidence

`docs-lint` check 14 is the mechanism named against
`adr:0005-evidence-lives-in-the-work-item#evidence-home`. On the pull request that does the
work it examines nothing at all, and it is right to: `doc:00-constitution#bean-lifecycle`
requires the bean to stay `in-progress` for the whole life of its own pull request, and check
14's candidate set is beans that become `completed` in the change.

**This is not a check 14 defect.** The check is behaving exactly as specified and reporting
its own vacuity truthfully. What is missing is anywhere saying what that means for a reader
of a green run.

## Observed

Measured on the pull request that implements `bean:0063` — an implementation PR whose bean
carries five criteria, each with a command, an expectation and verbatim output.

```
cmd:      bash tools/docs-lint.sh, on that branch, bean `in-progress` per doc:00 §7.2.1
observed: 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0

cmd:      the identical branch and the identical bean, flipped to `completed`
observed: 1 closing transitions, 5 criteria checked, 0 unnumbered.
          check 14 failures: 0
exit:     0
```

The mechanism works. Its five criteria pass inspection the moment it is asked to inspect
them. **On the pull request under review it is never asked**, and the difference between
"examined five criteria and found them sound" and "examined nothing" is one status field that
`doc:00-constitution#bean-lifecycle` forbids the author to change.

The counterpart is on the record and is not zero. The change that closed four beans at once
reported `4 closing transitions, 31 criteria checked, 0 unnumbered` — quoted in
`bean:0055`'s own evidence and in `bean:0054`'s. So check 14 does real work; it does it
exclusively on **bean-only closing pull requests**.

## The rule, verified rather than assumed

`doc:00-constitution#bean-lifecycle` reads, verbatim:

> `todo` → `in-progress` when the branch is cut → `completed` **after** the merge, in a
> separate change.
>
> The bean stays `in-progress` for the whole life of its own pull request, including through
> review. It is not set to `completed` in the change under review […]
>
> So closing a bean is always the *next* change, and is the first act of the session after a
> merge. This was convention rather than rule until `bean:0035` found it undocumented and
> load-bearing.

So the reading holds: it is a requirement, not a convention, and `bean:0035` is what made it
one. This bean does not propose amending it. The rule is right, and both its stated reasons
are right — a bean cannot close itself because its evidence includes the merge, and closing
it in its own branch would freeze it against the author's own review fixes under check 11.

Three properties of that passage are worth recording precisely, because they bound what any
fix can do:

| property | consequence |
|---|---|
| it uses no MUST, SHOULD or MAY | `documentation/README.md` defines those as the normative vocabulary and requires a MUST to carry an `Enforced by:` or `Enforcement gap:` line. This passage carries neither keyword nor line, so its force comes from `doc:00`'s precedence and its placement, not from the vocabulary the package defines |
| nothing enforces it | check 11 classifies by the status on the merge base. A bean set `completed` on its own branch moved from `in-progress`, which is a legal transition, so check 11 permits exactly the thing §7.2.1 forbids |
| therefore the silence is voluntary | an author who ignored §7.2.1 would get check 14 running on their implementation PR. Compliance with the rule is what produces the blind spot |

The third is the uncomfortable one and it is not an argument for non-compliance. It is the
reason this cannot be fixed by tightening §7.2.1: the rule is already doing its job, and the
gap is downstream of it.

## What it costs

- **The first mechanical inspection of a bean's evidence happens after the work has merged**,
  in a different pull request, reviewed by a different agent.
- **That closing pull request is typically bean-only.** Its reviewer is judging evidence cells
  that describe work they did not review, against a diff that contains none of it. The
  strongest form of the check runs in front of the reader least able to corroborate it.
- **It compounds with what the check accepts when it does run.** The bean on evidence-cell
  strength — `modus-0087`, unmerged, named by filename because a typed reference to an
  unmerged bean fails check 6 — records that check 14 decides only that a cell is non-empty
  and is not made entirely of evidence-kind names. Put the two together: **on an
  implementation pull request a green `docs-lint` line carries no information about evidence
  whatsoever, and on a closing one it carries very little.**

## Mechanism gap or documentation gap

**Documentation, with one caveat.** The counters do disclose it, and disclose it honestly:
`0 closing transitions, 0 criteria checked` is precisely what an honest vacuity assertion
should print, and `bean:0055` built those denominators for exactly this purpose — its own
text says the `OK`-line denominators are what make the check non-vacuous. A run that examined
nothing says so, in the line everybody quotes.

The caveat is that the disclosure is only legible to a reader who already knows what a
closing transition is. `0 closing transitions` and `31 criteria checked` are the same shape of
token in the same sentence, and nothing near them says that the first number being zero voids
the second. Nothing in `doc:05-authoring-for-agents#checks` states that a green check 14 on an
implementation PR asserts nothing about that bean's evidence.

There is a live instance of the invitation in this repository: `bean:0054`'s criterion 9 and
`bean:0055`'s criteria 6, 7 and 9 quote the counters verbatim inside their evidence cells.
Those uses are correct — they are closing PRs, and the numbers there are real. But it puts the
counters in evidence columns as a matter of house style, where the same line quoted from an
implementation PR would read identically and mean nothing.

## Options

| option | what it buys | cost |
|---|---|---|
| state it in `doc:05-authoring-for-agents#checks`: a green check 14 asserts nothing about a bean whose status did not change in the change | the reader of a green run stops over-reading it | one paragraph; does not change what any run reports |
| make the `OK` line say it in words when `closing transitions` is zero — for example `0 closing transitions (no bean evidence examined)` | the disclosure becomes legible at the point of use, not in a document | edits the line whose exact text is quoted verbatim in the evidence of completed, check-11-frozen beans; those transcripts record what was observed then, so nothing is invalidated, but the corpus will hold two spellings |
| run check 14 in a non-blocking, report-only mode over the branch's bean regardless of status | the author sees the verdict while the work is reviewable | a second execution path for the check, and a report nobody must act on is a report nobody reads |
| require the closing pull request to name the implementation PR it closes | the closing reviewer can reach the diff the cells describe | procedural, unenforced unless a check reads it |
| leave it | nothing | the misreading is available to every reviewer and is not currently warned against |

Deciding between them is this bean's work, not its premise.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The zero-contribution observation above is reproduced against the changed state, on a real implementation branch, with its non-zero counterfactual beside it | test-run |
| 2 | Whatever is adopted, a reader of a green `docs-lint` line on an implementation pull request can determine from the run or from one referenced anchor that no bean evidence was examined | citation |
| 3 | The chosen option is recorded with its reason, and the rejected ones with theirs | citation |
| 4 | If the `OK` line's text changes, every completed bean quoting it verbatim is identified, and the decision not to amend those frozen transcripts is stated | analyser run over the corpus |
| 5 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- **Amending `doc:00-constitution#bean-lifecycle`.** The rule is correct and both its reasons
  hold. This bean records a consequence of it, not an objection to it.
- What check 14 accepts in an evidence cell once it does run. That is `modus-0087`, unmerged.
- The fence classifier and the citation matcher (`bean:0061`, `bean:0063`).
- Check 11's classification by merge-base status, which is what makes §7.2.1 unenforced. That
  is a deliberate design recorded in `bean:0038` and changing it would block every closure.
