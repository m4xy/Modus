---
# modus-0096
title: Check 14 contributes nothing to any implementation pull request, by rule, and a green docs-lint line there says nothing about evidence
status: completed
type: task
priority: high
created_at: 2026-08-30T00:00:00Z
---

# Check 14 contributes nothing to any implementation pull request, by rule, and a green docs-lint line there says nothing about evidence

`docs-lint` check 14 is the mechanism named against
`adr:0005-evidence-lives-in-the-work-item#evidence-home`. The invariant, stated tightly:
**check 14 never examines the bean whose work the pull request contains.**

On an implementation pull request it examines nothing at all, and it is right to:
`doc:00-constitution#bean-lifecycle` requires the bean to stay `in-progress` for the whole
life of its own pull request, and check 14's candidate set is beans that become `completed`
in the change. The tighter form matters because a branch may close a previously merged bean
while implementing the next one — and then check 14 does run, on the bean being closed, never
on the bean being built.

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
| nothing enforces it | check 11 classifies by the status on the merge base. A bean set `completed` on its own branch moved from `in-progress`, which is a legal transition, so check 11 permits exactly the thing §7.2.1 forbids. **Observed in the second arm of the pair above**: flipping `bean:0063` to `completed` on its own branch produced `1 closing transitions, 5 criteria checked` and exit 0 — no check 11 failure, because check 11 never looked |
| the cause is check 14's scoping, not the rule | check 14 chose to scope by **closing transition**. It could have scoped by the bean the branch names. §7.2.1 does not cause the blind spot; it guarantees the chosen scope is empty on an implementation pull request |

The third is the one to be careful with. An earlier version of this bean said "the silence is
voluntary", on the reasoning that an author ignoring §7.2.1 would get check 14 running. That
sentence does rhetorical work its evidence does not carry and it is withdrawn: nothing here is
an author's choice, because complying with a rule of `doc:00-constitution` is not optional and
violating it is not an available option. The accurate statement is that **nothing forced check
14's scoping**, which is why the fix is downstream of §7.2.1 rather than in it — and why the
third option below, running over the branch's bean regardless of status, is available at all.

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

There are live instances of the invitation in this repository. Both beans' evidence tables
carry the header `| # | criterion | command | expectation | observed |`, so the evidence
column check 14 reads is `observed`, the fifth data column:

```
cmd:      over origin/main, for each numbered row, test whether the `observed` cell contains
          the counters line
observed: .beans/modus-0054 row 9                    observed cell quotes the counters
          .beans/modus-0055 rows 6, 7, 9 and 11      all four quote the counters
          grep -c "closing transitions" on main's modus-0054  ->  1
exit:     0
```

Five cells across two beans. Those uses are correct — they are closing pull requests and the
numbers there are real — but they put the counters into evidence columns as house style,
where the same line quoted from an implementation pull request would read identically and
mean nothing.

**This paragraph was reported in review as wrong on all four references, on a reading that
none of those cells quotes the counters. That reading does not reproduce.** It matches the
`expectation` column rather than the `observed` one — for `modus-0055` row 9, `expectation`
reads "green with `docsLint` inside it, on the tree that closes these four beans" while
`observed` reads "`BUILD SUCCESSFUL in 15s` … `docs-lint: OK — … 4 closing transitions, 31
criteria checked, 0 unnumbered.`" A five-column evidence table is off by one between the two,
and the check reads the last of them.

That is worth more than the correction it replaces. **A sub-reference into another bean —
"row 9 of that bean says X" — is invisible to `docs-lint` check 6 by construction.** Check 6
resolves `bean:0054` and `bean:0055` happily, because both exist; it has no way to test
whether the row named says what the citing sentence claims. Every other citing-while-restating
instance found this sprint was at least in principle mechanisable. This one is not, short of a
tool that reads the target's semantics — and it cuts both ways, since the review's own
sub-reference was the one that missed.

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

## Decision

**Adopted: option 1 — state it in `doc:05-authoring-for-agents#checks`.** The `OK` line already
carries the datum: a run on an implementation pull request prints `0 closing transitions, 0
criteria checked`, a true and complete report of what was examined. What no document owned was
what that zero means, so the gap closes where the check is specified rather than by changing
what any run reports. The paragraph sits directly after the one that already states check 14's
scope, so scope and consequence are read together, and it adds the half the scope paragraph did
not carry: the check's conditions are structural, so a green check 14 establishes the *shape* of
the evidence in the beans a change closes and nothing about the implementation under review.

| rejected | reason |
|---|---|
| reword the `OK` line when `closing transitions` is zero | It edits a string quoted verbatim in the evidence of `completed` beans that check 11 has frozen, and buys a reader who already has the number a second spelling of it. One anchor saying what the zero means costs no corpus divergence |
| run check 14 report-only over the branch's bean regardless of status | A second execution path, and a verdict nobody must act on. It also reports more than it can decide: a bean under implementation has criteria whose evidence is the merge, so at the moment such a run fires those criteria are *correctly* unanswered and every implementation branch draws the same routine report |
| require the closing pull request to name the implementation pull request it closes | Procedural and unenforced. It addresses the closing reviewer's corroboration problem, not the misreading of a green line on the implementation pull request, which is what this bean is about |
| leave it | The misreading stays available to every reviewer and nothing warns against it |

The `OK` line's text is unchanged by this bean, so no frozen transcript quoting it is affected
and none is amended.

## Evidence

Runs are against the working tree, which is what `tools/docs-lint.sh` reads by its own
construction (`BASE` against the working tree, not against `HEAD`). Every sha below was printed
by a `git rev-parse HEAD` in the same command as the run beside it, and names the commit that run
was made on. Three fences carry none: arm B, run on the same tree as arm A and named as such in
the sentence above it; criterion 4, whose command is itself written relative to `HEAD`; and the
first gate transcript under criterion 5, whose command prints no head and for which none is
recorded.

Later commits on this branch pasted these cells and rewrote the paragraph quoted under criterion
2. The re-run under criterion 1 was made after all of them, at a named head, and is what shows
the paste to be measurement-neutral rather than merely reproducible
(`doc:50-memory-and-evidence#corpus-figures`). It no longer reprints the record byte for byte,
and the difference is not the paste: `origin/main` has moved since arm A and arm B were taken, so
the `on origin/main` field differs and, as the diff shows, nothing else does. Those two arms are
left at the figures their stamped head printed, because a merge falsifies a corpus sweep and its
author cannot prevent it — re-running the sweep belongs to that merge, not to this pull request.
Nothing added to this bean since cites an anchor it did not already cite, for the same reason: a
new reference would move the `references` field the arms record, and the drift would stop being
attributable to one cause.

### Criterion 1 — the zero observation, reproduced on this branch, with its counterfactual

Arm A, the change as it is reviewed: `bean:0096` `in-progress`, as
`doc:00-constitution#bean-lifecycle` requires for the whole life of this pull request.

```
cmd:      git rev-parse HEAD && bash tools/docs-lint.sh
expect:   exit 0; the counters report no closing transition and no criterion examined
observed: c2b12c053210ddd2129f34dd25634788af7d8318
          docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

Arm B, the counterfactual: the identical tree with this bean's `status:` flipped to `completed`,
which `doc:00-constitution#bean-lifecycle` forbids in this pull request and which is therefore
the only way to observe the check running on the bean whose work this branch contains. Reverted
immediately after the run; the plant is uncommitted and the bean was committed first, so
`git checkout -- .beans` restores it (`AGENTS.md`).

```
cmd:      sed -i '' 's/^status: in-progress/status: completed/' .beans/modus-0096--*.md
          grep -n '^status:' .beans/modus-0096--*.md
          bash tools/docs-lint.sh; git checkout -- .beans
expect:   exit 0; the plant in place, one closing transition, and this bean's five criteria
          examined and sound
observed: 4:status: completed
          docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 1 closing transitions, 5 criteria checked, 0 unnumbered.
exit:     0
```

The pair is the bean's claim: one status field, forbidden to the author of this pull request,
separates "examined nothing" from "examined five criteria and found them sound".

Measurement-neutrality of the record itself (`doc:50-memory-and-evidence#corpus-figures`): both
arms were first run with the two cells above holding a sentinel marker, so no run could satisfy
itself from its own transcript; the outputs were then pasted and both arms re-run.

```
cmd:      git rev-parse HEAD && \
          B=.beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md
          bash tools/docs-lint.sh | tail -1 > /tmp/a2.txt
          sed -i '' 's/^status: in-progress/status: completed/' "$B"
          bash tools/docs-lint.sh | tail -1 > /tmp/b2.txt; git checkout -- .beans
          awk '/^ +docs-lint: OK — [0-9]+ documents/ { sub(/^ +/, ""); print }' "$B" \
            > /tmp/pasted.txt
          cat /tmp/a2.txt /tmp/b2.txt > /tmp/rerun.txt
          diff /tmp/pasted.txt /tmp/rerun.txt && echo "identical: what this bean records is what a re-run prints"
expect:   the head named, and the paste adding no document, anchor, reference or bean: any line
          the diff reports differs in the `on origin/main` field and in nothing else
observed: c9b0194c91ad7b12e918f7ac2ca019d758e51916
          1,2c1,2
          < docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          < docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 1 closing transitions, 5 criteria checked, 0 unnumbered.
          ---
          > docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          > docs-lint: OK — 19 documents, 111 anchors, 1473 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 1 closing transitions, 5 criteria checked, 0 unnumbered.
exit:     1
```

### Criterion 2 — a reader of a green line can determine that no evidence was examined

The run prints the discriminating number already; what was missing was an anchor saying what it
means. Both halves now exist, and the anchor is one hop from the check that printed the line.

```
cmd:      git rev-parse HEAD && awk '/^Every condition in check 14/,/^$/' \
            documentation/05-authoring-for-agents.md
expect:   the paragraph states that `0 closing transitions` means no bean's evidence was
          examined, and that an implementation pull request always reports it
observed: c937150e7538e60e8c3181eb1f68a7aa787b328a
          Every condition in check 14's row above is structural. Whether the output in an evidence cell
          was ever produced, whether the command beside it reproduces that output, and whether either
          bears on the criterion the cell is filed under are outside what the check can decide. Scope
          compounds it: `doc:00-constitution#bean-lifecycle` holds a bean `in-progress` for the whole
          life of its own pull request, so on the pull request that implements a bean, that bean is
          never a candidate. `0 closing transitions` on the `OK` line is that statement — no bean's
          evidence was examined, and the zero beside it under `criteria checked` follows from the empty
          candidate set, not from a bean inspected and found bare. A non-zero pair comes from a bean the
          change *closes*, whose implementation merged earlier and was reviewed elsewhere. A green check
          14 therefore establishes the shape of the evidence in the beans a change closes, and nothing
          at all about the implementation under review (`bean:0096`).
```

Residual, stated rather than claimed away: a reader who consults no anchor still sees only the
counters. Nothing in this change alters what a run prints, and the run does not cite the
paragraph — that is what the rejected `OK`-line option would have bought, at the cost recorded
under Decision.

### Criterion 3 — the chosen option and the rejected ones are recorded with reasons

The `## Decision` section above, in this bean: option 1 adopted with its reason, the four
rejected options each with theirs. `documentation/` is not the home for it —
`doc:05-authoring-for-agents#bean-split` puts what is being done and why in the bean, and
`doc:05-authoring-for-agents#prose-ban` keeps rationale out of the document.

### Criterion 4 — the `OK` line's text is unchanged, so no frozen transcript is touched

Conditional criterion, and its condition does not hold: this change edits no file under
`tools/`, so the `printf` that emits the `OK` line is byte-identical to `origin/main`'s.

```
cmd:      git diff --name-only origin/main...HEAD
expect:   documentation and this bean only; nothing under tools/
observed: .beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md
          documentation/05-authoring-for-agents.md
```

No completed bean quoting the `OK` line verbatim is amended, because none is invalidated.

### Criterion 5 — the gate is green

This first transcript is the one fence here whose command prints no head, and none is recorded
for it: `./gradlew qualityCheck` alone says nothing about the tree it ran on, and a sha written
beside it now would be a reconstruction rather than something the run printed. The stamped re-run
below is what ties the gate to a commit. `[same]` stands for the counters line quoted in full
under criterion 1 arm A, as its author recorded it; every other omission is marked `[...]`.

```
cmd:      ./gradlew qualityCheck
expect:   BUILD SUCCESSFUL, docsLint inside it
observed: [...]
          > Task :docsLintTest
          [...]
          docs-lint-test: 37 passed, 0 failed.
          [...]
          > Task :docsLint
          docs-lint: OK — [same]
          [...]
          > Task :qualityCheck
          [...]
          BUILD SUCCESSFUL in 5m 11s
          168 actionable tasks: 55 executed, 113 from cache
exit:     0
```

Re-run on the committed head, so the gate is observed green on a tree that differs from the one
this pull request presents only by the paste of this cell and of the criterion 1 re-run above it.
Its counters line is elided rather than marked `[same]`: it is not the same as arm A, because
`origin/main` moved after arm A was taken. It is byte-identical to the first `>` line of the
criterion 1 diff above, which is where the counters a run on this tree prints are quoted in full.

```
cmd:      git rev-parse HEAD && ./gradlew qualityCheck
expect:   BUILD SUCCESSFUL, with :docsLintTest and :docsLint inside it
observed: c9b0194c91ad7b12e918f7ac2ca019d758e51916
          [...]
          > Task :docsLintTest
          [...]
          docs-lint-test: 37 passed, 0 failed.
          [...]
          > Task :docsLint
          docs-lint: OK — [...]
          [...]
          > Task :qualityCheck
          [...]
          BUILD SUCCESSFUL in 26s
          159 actionable tasks: 5 executed, 154 up-to-date
          Configuration cache entry reused.
exit:     0
```

## Closing evidence — merged as PR #70, squashed onto `main` as `0e4324d`

A bean cannot close itself, so this is the next change (`doc:00-constitution#bean-lifecycle`).
Every command below names `0e4324d` explicitly, except the paired run under criterion 1, which is
about this tree and says which one it is.

**All five criteria are met.** Criterion 4 is conditional and its condition does not hold, which
is recorded as such rather than as a pass. Criterion 2 carries the residual its own section
already states.

| # | criterion | observed |
|---|---|---|
| 1 | the zero-contribution observation reproduced on a real implementation branch, with its non-zero counterfactual beside it | Met on the branch, in the two arms above. The counterfactual is no longer a plant: this change closes two beans, so `docs-lint` on this tree prints `2 closing transitions, 8 criteria checked` where the same command on the unmodified tree at the same head prints `0 closing transitions, 0 criteria checked` and differs in no other field. Block B |
| 2 | a reader of a green line can determine, from the run or from one referenced anchor, that no bean evidence was examined | Met, on `main`. `documentation/05-authoring-for-agents.md:245-255` at `0e4324d` carries the paragraph, one hop from the check that prints the line. The residual its own section states is unchanged and is not claimed away: a reader who consults no anchor still sees only the counters. Block A |
| 3 | the chosen option is recorded with its reason, and the rejected ones with theirs | Met, on `main`. `## Decision` at line 182 of this bean at `0e4324d`, opening `Adopted: option 1`, with a four-row table of rejected options each carrying its reason. Block A |
| 4 | if the `OK` line's text changes, every completed bean quoting it verbatim is identified and the decision not to amend them is stated | Condition does not hold, which is the answer rather than a pass. `0e4324d` changed two files and neither is under `tools/`, so the `printf` that emits the `OK` line is byte-identical to the one before it and no frozen transcript is invalidated. Block A |
| 5 | `./gradlew qualityCheck` green | Met. PR #70's checks are green on the pull request whose merge commit is `0e4324d`, and `qualityCheck` is green on this closing branch. Blocks C and D |

### Block A — criteria 2, 3 and 4, read off `0e4324d`

```
cmd:      git show 0e4324d:documentation/05-authoring-for-agents.md \
            | awk '/^Every condition in check 14/,/^$/ { printf "%d:%s\n", NR, $0 }'
observed: 245:Every condition in check 14's row above is structural. Whether the output in an evidence cell
          246:was ever produced, whether the command beside it reproduces that output, and whether either
          247:bears on the criterion the cell is filed under are outside what the check can decide. Scope
          248:compounds it: `doc:00-constitution#bean-lifecycle` holds a bean `in-progress` for the whole
          249:life of its own pull request, so on the pull request that implements a bean, that bean is
          250:never a candidate. `0 closing transitions` on the `OK` line is that statement — no bean's
          251:evidence was examined, and the zero beside it under `criteria checked` follows from the empty
          252:candidate set, not from a bean inspected and found bare. A non-zero pair comes from a bean the
          253:change *closes*, whose implementation merged earlier and was reviewed elsewhere. A green check
          254:14 therefore establishes the shape of the evidence in the beans a change closes, and nothing
          255:at all about the implementation under review (`bean:0096`).
          256:

cmd:      git grep -n '^## Decision$' 0e4324d -- '.beans/modus-0096--*.md'
          git grep -n '^\*\*Adopted: option 1' 0e4324d -- '.beans/modus-0096--*.md'
observed: 0e4324d:.beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md:182:## Decision
          0e4324d:.beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md:184:**Adopted: option 1 — state it in `doc:05-authoring-for-agents#checks`.** The `OK` line already

cmd:      git show 0e4324d --name-only --format=''
observed: .beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md
          documentation/05-authoring-for-agents.md
exit:     0
```

The `awk` range prints the paragraph's own line numbers, so the locator in criterion 2's row and
the text it names come out of one command rather than two. Its last line is the blank the range
terminated on, printed because the range is inclusive of its end.

### Block B — criterion 1's counterfactual, no longer planted

The two arms above were a plant and a revert, because
`doc:00-constitution#bean-lifecycle` forbade this bean's author from producing the non-zero arm
honestly. On this change it is produced honestly: the change closes two beans, so the check runs.
Both runs are at head `1c19cf0`, in the same worktree, and the only difference between the trees
is the `status:` line of two beans.

```
cmd:      git status --porcelain && git rev-parse HEAD && bash tools/docs-lint.sh
expect:   nothing modified; the counters at zero, as on `main`
observed: 1c19cf0fc911f10992181a494a4f74a5703644dc
          docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 46 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0

cmd:      git rev-parse HEAD && bash tools/docs-lint.sh
tree:     the same tree, with `status: in-progress` replaced by `status: completed` in
          `.beans/modus-0049--*.md` and `.beans/modus-0096--*.md` and nothing else changed —
          `git diff --stat` reports `2 files changed, 2 insertions(+), 2 deletions(-)`
observed: 1c19cf0fc911f10992181a494a4f74a5703644dc
          docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 46 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 2 closing transitions, 8 criteria checked, 0 unnumbered.
exit:     0
```

`git status --porcelain` printed nothing before the first run, which is what makes it a
measurement of `1c19cf0` rather than of a working tree that happens to sit on it. Every field but
the two counters is identical across the pair, `46 selectable` included — `in-progress` and
`completed` are both outside check 12's selectable set, so the status flip moves the closing
counters and nothing else. `8` is `bean:0049`'s three criteria plus this bean's five.

Both figures were captured before this section existed and the section was written from the
capture file, so neither run could read its own transcript
(`doc:50-memory-and-evidence#corpus-figures`). They are stamped at `1c19cf0`, which is
`origin/main`'s head at the time of the run; a merge on this sprint falsifies the corpus fields
and not the pair, which is what this block is about.

### Block C — criterion 5, PR #70's checks and the commit they belong to

```
cmd:      GITHUB_TOKEN= gh pr view 70 --json mergeCommit,mergedAt,title -q '.mergeCommit.oid, .mergedAt, .title'
observed: 0e4324d3be556136e16e4c05d779207cea09e697
          2026-09-04T08:27:49Z
          docs(05): state what a green check 14 establishes, and what it cannot
exit:     0

cmd:      GITHUB_TOKEN= gh pr view 70 --json statusCheckRollup \
            -q '.statusCheckRollup[] | "\(.name)\t\(.conclusion)\t\(.detailsUrl)"'
observed: which halves	SUCCESS	[...]/runs/33852845144/job/100959330898
          which halves	SUCCESS	[...]/runs/33852839248/job/100959313117
          build + mechanical gates	SUCCESS	[...]/runs/33852845144/job/100959368854
          build + mechanical gates	SUCCESS	[...]/runs/33852839248/job/100959350849
          backoffice + e2e	SKIPPED	[...]/runs/33852845144/job/100959370503
          backoffice + e2e	SUCCESS	[...]/runs/33852839248/job/100959350791
          gate	SUCCESS	[...]/runs/33852845144/job/100959615670
          gate	SUCCESS	[...]/runs/33852839248/job/100960998163
exit:     0
```

`[...]` elides the `https://github.com/m4xy/Modus/actions` prefix each URL carries, and nothing
else; the run and job ids are what the columns are here for. Every check appears twice because
two workflow runs answered for this pull request, `33852845144` and `33852839248`, and the pair
is printed rather than collapsed so that the one job whose conclusion differs between them is
visible: `backoffice + e2e` is `SKIPPED` in the first and `SUCCESS` in the second. Why the
per-path job resolved differently across two runs of the same head is not established here and
is not what this criterion rests on. Neither conclusion is a failure, and `gate` — the job
`bean:0047` is holding a required-status rule back for — is `SUCCESS` in both.


### Block D — criterion 5, the gate on the branch that carries this close

```
cmd:      git rev-parse HEAD && ./gradlew qualityCheck
observed: 1c19cf0fc911f10992181a494a4f74a5703644dc
          [...]
          docs-lint-test: 37 passed, 0 failed.
          [...]
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint: OK — 19 documents, 111 anchors, 1574 references, 103 beans, 37 graph edges, 47 selectable, 103 bean ids, 1 introduced, 102 on origin/main, 2 closing transitions, 8 criteria checked, 0 unnumbered.

          > Task :qualityCheck

          BUILD SUCCESSFUL in 21s
          160 actionable tasks: 6 executed, 154 up-to-date
exit:     0
tree:     `chore/close-0049-and-0096`, uncommitted, everything this pull request presents
          except this block. `git rev-parse HEAD` reports `1c19cf0` because the branch is cut
          from it and nothing is committed yet.
```

Each `[...]` marks omitted output: Gradle's task list and the 37 named lines of
`docsLintTest` before the first, the npm typecheck and lint output between the second and
third, and Gradle's incubating problems-report line and its Gradle 10 deprecation notice
before `BUILD SUCCESSFUL`. Nothing the criterion rests on was trimmed — `exit:` is stated
separately and `BUILD SUCCESSFUL` is present — and no line inside the capture was edited.

The `docs-lint` counters here read `1574 references` and `103 beans` where criterion 1's pair
reads `1552` and `102`: the pair was taken before `bean:0120` and these two closing sections
existed, and those are the only causes. `2 closing transitions, 8 criteria checked` is
identical across both, which is the field this bean is about.

Appending this block is the last edit to the tree the run measured, so the run does not cover
its own transcript. That is unavoidable for the final gate capture and is stated rather than
papered over; CI re-runs the same gate over the committed tree, and its result is on the pull
request.
### Block E — the run that proves check 14 examined this close rather than passed over it

`docs-lint: OK` on a tree with no closing bean and `docs-lint: OK` on this one differ only in
the counters, which is this bean's whole subject and is not by itself a demonstration that the
check would have rejected anything here. Two plants against the closing sections this change
adds, each restored from a copy taken before it, are. No `git` operation is involved in either,
so no uncommitted work in the tree could be discarded (`bean:0102`).

```
plant:    this bean's criterion 3 row, with its `observed` cell emptied
observed: 400:| 3 | the chosen option is recorded with its reason, and the rejected ones with theirs |  |
          FAIL check 14 .beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md: criterion 3 closes with an empty evidence cell (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

plant:    every `criterion 2` and `Criterion 2` in `.beans/modus-0049--*.md` reworded to
          `that criterion`, and its closing row renumbered from `| 2 |` to `| two |`, so
          nothing in that bean bears or cites the number any more
observed: FAIL check 14 .beans/modus-0049--bash-32-claim-is-unenforced.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

cmd:      diff <copy of 0049> .beans/modus-0049--*.md && diff <copy of 0096> .beans/modus-0096--*.md
          bash tools/docs-lint.sh
observed: identical
          docs-lint: OK — 19 documents, 111 anchors, 1572 references, 103 beans, 37 graph edges, 47 selectable, 103 bean ids, 1 introduced, 102 on origin/main, 2 closing transitions, 8 criteria checked, 0 unnumbered.
exit:     0
```

The second plant is the sharper of the two, because the criterion it unanswers is the one
`bean:0049` closes **unmet**. Check 14 accepts that bean with criterion 2 recorded NOT MET and
rejects it the moment nothing bears the number — which is the distinction between answered and
met, and the reason a green check 14 is a statement about shape (`doc:05-authoring-for-agents#checks`).
The restored tree's counters are `103 beans` and `1 introduced` rather than criterion 1's `102`
and `0`, because `bean:0120` is present by then; the two counters this section is about are
unchanged.
