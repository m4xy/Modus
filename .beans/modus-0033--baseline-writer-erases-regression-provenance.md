---
# modus-0033
title: coverageBaselineWrite destroys recorded provenance on every write, and a regression destroys the ones before it
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# coverageBaselineWrite destroys recorded provenance on every write, and a regression destroys the ones before it

**The title changed in `bean:0147`.** It named the no-op write, which the seventh and
eighth observations show is the *milder* half: a write that legitimately records a new
regression destroys every regression recorded before it, and the original title did not
cover that. The two observations are at the end of this bean; nothing above them is
edited.

`config/coverage/baseline.tsv`'s own header states:

> The writer REFUSES to raise a missed count; a deliberate regression needs
> `-Pcoverage.regress=<reason>`, and **the reason is recorded below**.

It is recorded below only until the next baseline write, whatever that write is about.
`build-logic/src/main/kotlin/modus.coverage.gradle.kts` rebuilds the file from a
constant header, a note derived solely from **this** run's regressions, and the rows:

```kotlin
val note =
    if (regressions.isEmpty() || regressReason == null) { "" } else { … }
target.writeText(header + note + rows.joinToString("\n", postfix = "\n"))
```

The comment two lines above it claims the opposite — *"the regression is self-documenting
in the same diff that records it and stays in the history afterwards"*. It stays in the
git history; it does not stay in the file, and the file is what the next agent reads.

Observed while doing `bean:0032`, a refactor with no coverage regression of its own:

```
cmd:      ./gradlew coverageBaselineWrite && git diff config/coverage/baseline.tsv
observed: -# REGRESSION accepted with -Pcoverage.regress: identity validators replaced two
          -#   three-branch isNotBlank() && none{} checks with single whole-string regex
          -#   matches; six branches disappear from the source, none become uncovered
          -#   :core-domain: covered branches 44 -> 38
          -:core-domain                   0	0	618	38
          +:core-domain                   0	0	619	38
```

`bean:0009`'s reason was deleted by a change that had nothing to do with it. It was
restored by hand in `bean:0032`'s pull request, which is not a mechanism.

## Sixth observation (`bean:0065`) — the erasure is not conditional on anything

The five earlier instances were all writes that moved a figure, which made this look like a
bug about *regressions*: "a later regression rewrites an earlier one's history". It is not.
The erasure is unconditional, and the sixth observation isolates it because **no figure
changed at all**.

`bean:0065` adds three interfaces to `core-domain`. Interfaces generate no instructions, so
every numeric row was byte-identical before and after. The writer still deleted six lines:

```
cmd:      ./gradlew coverageBaselineWrite
observed: diff baseline.before.tsv config/coverage/baseline.tsv
          8,13d7
          < # REGRESSION accepted with -Pcoverage.regress: identity validators replaced …
          < #   :core-domain: covered branches 44 -> 38
          < # REGRESSION accepted with -Pcoverage.regress: GrantIssued stores its capabilities …
          < #   :core-domain: covered instructions 1549 -> 1543
          < # Restored by hand in bean:0032, bean:0030 and twice in bean:0036 after …
          < # it now also erases a PREVIOUS regression block when recording a new one. …
```

Both regression blocks **and the note recording that this keeps happening** were destroyed by
a write whose every row was unchanged. Restored by hand again, in `bean:0065`.

The mechanism is plain once stated in this order (`modus.coverage.gradle.kts`, `:258`):
`header` is a constant, `note` is empty unless **this** run regressed, and the file is
rebuilt as `header + note + rows`. Nothing reads the existing file's comments. So the
condition for losing provenance is not "a regression happened" — it is "`coverageBaselineWrite`
ran".

**This changes what the bean is about, and the reframing matters more than the extra data
point.** The dangerous case is not the one that looks dangerous. It is the agent who changes
something they believe has no coverage effect, runs the writer **to confirm nothing moved**,
sees `(unchanged)` on every row, and commits a diff that is pure history loss. That is a
reasonable, careful thing to do, and it is the worst thing to do. Every restoration so far
was caught by a human or agent reading the diff by eye, and none by a mechanism.

*(This sentence said "the six restorations" over an enumeration that names five. The count
is corrected in the running tally at the end of this bean rather than here, because a bare
total beside the list it summarises is exactly what drifts — see the note under observation
eight.)*

## Success criteria

- A recorded regression reason survives every later baseline write that does not itself
  regress that module. Observed failing first, per `doc:00-constitution#observed-failing`:
  write a baseline with a regression, write another without one, assert the reason is still
  there.
- **The no-op write is covered explicitly**, because it is the case that looks safe: run
  `coverageBaselineWrite` when no figure changes, and assert the file is byte-identical
  afterwards. A writer that preserves provenance only when it also rewrites a row would pass
  the criterion above and fail this one — that is the `bean:0065` observation, and it is the
  regression test for it.
- Decide and record whether a reason should expire — a module whose figures have moved
  twice since may no longer be explained by it. Preserving forever and preserving until the
  module's row next changes are both defensible; silently dropping on any write is not.
- The `// The reason lives in the file…` comment at `modus.coverage.gradle.kts` is
  corrected or deleted. It is the claim this bean disproves.

## Seventh and eighth observations (`bean:0147`) — the erasure is worse when the write is legitimate

Both on one branch, in one afternoon, which is what makes them a pair rather than two more
tally marks. The first is the known shape. The second is not, and it is what retitles this
bean.

### Seventh — the known shape, confirmed a third time

`bean:0147` adds the flat-file store's write path. Its first baseline write raised
`:adapter-persistence-flatfile` from `33 0 0 0` to `33 0 780 18` — a pure improvement, no
`-Pcoverage.regress` needed, no regression of its own.

```
cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 0 0 -> 33 0 780 18
cmd:      git diff config/coverage/baseline.tsv
observed: -# REGRESSION accepted with -Pcoverage.regress: identity validators replaced two …
          -#   :core-domain: covered branches 44 -> 38
          -# REGRESSION accepted with -Pcoverage.regress: GrantIssued stores its capabilities …
          -#   :core-domain: covered instructions 1549 -> 1543
          -# Restored by hand in bean:0032, bean:0030, twice in bean:0036 and again in bean:0065 …
          (11 header lines deleted; one row changed)
```

Identical to the `bean:0065` observation above, and restored by hand the same way.

### Eighth — a legitimate regression erases the two regressions before it

Later on the same branch, review found a real defect: `PathLocks` held its lock stripes per
instance, so two `DocumentStore`s over one root were not serialised. The fix moves the map to
the companion object, which removes the per-instance field initialiser — **two covered
instructions leave the source and none becomes uncovered**. Missed instructions and missed
branches both stay at `0`.

That is exactly the case `-Pcoverage.regress` exists for, and the writer's refusal worked:

```
cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 780 18 -> 33 0 778 18  <-- REGRESSION
          > coverageBaselineWrite refuses to record worse coverage:
            :adapter-persistence-flatfile (covered instructions 780 -> 778). Restore the
            coverage, or re-run with -Pcoverage.regress=<reason>; the reason is written into
            the baseline and belongs in the pull request body too.
          BUILD FAILED in 44s
```

Re-run with the reason, as instructed. It succeeded, recorded its own block — and deleted the
other two:

```
cmd:      ./gradlew coverageBaselineWrite -Pcoverage.regress="PathLocks moved its stripe map
            from an instance field to the companion object …"
observed: :adapter-persistence-flatfile  33 0 780 18 -> 33 0 778 18  <-- REGRESSION
          BUILD SUCCESSFUL in 17s
cmd:      git diff config/coverage/baseline.tsv
observed: -# REGRESSION accepted with -Pcoverage.regress: identity validators replaced two …
          -#   :core-domain: covered branches 44 -> 38
          -# REGRESSION accepted with -Pcoverage.regress: GrantIssued stores its capabilities …
          -#   :core-domain: covered instructions 1549 -> 1543
          -# Restored by hand in bean:0032, bean:0030, twice in bean:0036, again in bean:0065 …
          +# REGRESSION accepted with -Pcoverage.regress: PathLocks moved its stripe map from an
          +#   instance field to the companion object …
          +#   :adapter-persistence-flatfile: covered instructions 780 -> 778
```

Two `REGRESSION` blocks in, one out. `bean:0009`'s reason and `bean:0036`'s reason were both
destroyed by the act of recording `bean:0147`'s.

### What the eighth observation changes

The mechanism is the same one this bean already names — `header + note + rows`, where `note`
is derived from **this** run alone — so nothing about the diagnosis moves. What moves is the
severity and the framing:

- **The instruction the tool prints leads directly to the data loss.** The refusal message
  says "re-run with `-Pcoverage.regress=<reason>`; the reason is written into the baseline".
  Following that instruction, exactly as given, is what destroys the older reasons. An agent
  cannot avoid this by being careful, because being careful *is* the path.
- **The loss scales with how much history exists.** Every recorded regression makes the next
  recorded regression more destructive. The file is at its most informative immediately before
  the write that empties it.
- **The `bean:0065` framing said the dangerous case is the one that looks safe.** That is
  still true and is now the milder half. The eighth observation is the case that looks
  *correct* — a genuine regression, a written reason, a green build, a diff a reviewer will
  read as "one row and one comment moved" because the deletions sit above the addition in the
  same hunk.
- **Restorations by hand, none by a mechanism — one per bean, summed from the list rather
  than counted beside it:** `bean:0032` (1), `bean:0030` (1), `bean:0036` (2), `bean:0065`
  (1), `bean:0066` (2), `bean:0147` (3) = **10**.

  The per-bean figures are written out because the previous version of this line said
  "eight" over an enumeration of seven, and omitted `bean:0066` entirely — that bean merged
  after the line was written. `baseline.tsv`'s own note carried the same drift
  independently. A hand-written total placed beside the enumeration it summarises is made
  wrong by the next commit that extends the enumeration, which is the defect, not the
  arithmetic. `bean:0173` raises the mechanical check.

### One more success criterion, from the eighth observation

- **A write that records a new regression preserves every regression block already in the
  file.** Observed failing first: record a regression for module A, then record one for
  module B, and assert A's reason is still present.

  **The justification first written for this criterion was wrong, and the correction is the
  reason to keep it.** It claimed the criterion was independent of criterion 1 because
  "this branch passed criterion 1 while failing this one". That is false. Criterion 1 says a
  reason survives every later write *that does not itself regress that module*; observation
  eight regresses `:adapter-persistence-flatfile` and destroys `:core-domain`'s two reasons,
  which criterion 1 already forbids. So does observation seven. **The branch failed
  criterion 1 twice**, and this criterion was never the only thing it failed.

  The stated test was the wrong shape too: "a writer that appends satisfies both; one that
  rebuilds from a constant header satisfies neither" shows the two criteria *correlate* on
  two implementations. Independence needs a design where one holds and the other fails.

  Here is that design, and it is why the criterion stays. Criterion 1's qualifier is **"that
  module"**, and that qualifier is a real hole: a writer that, on regressing `:core-domain`
  a second time, **replaced** `:core-domain`'s existing reason with the new one satisfies
  criterion 1 — the write does regress that module, so criterion 1 says nothing — and still
  loses history. "Preserves every regression block already in the file" is what closes it.

  Recorded at this length because a future fixer reading the original text would have
  concluded criterion 1 needed no strengthening, and it does.

## Six further instances, from `bean:0152`

Numbered per bean rather than by position, as the baseline's own enumeration now is:
`bean:0152 (6)`, taking the running total to 16. A bare ordinal was wrong twice in this bean
already, because two branches in flight each thought they held the next number.

`bean:0152` is the largest single contribution so far, and that is a fact about the *cost*
rather than about the branch: it was six because the branch wrote the baseline six times, and
every write erases. Five were during development; the sixth was the rebase onto `36581a6`,
which is the shape worth noticing — **a restoration does not survive a rebase either**, so
the price is once per write *and* once per rebase, not once per regression.

| # | write | what the writer printed | what it erased |
|---|---|---|---|
| 1 | `:core-domain` gains the `work` context | `0 0 1543 130` -> `0 0 2505 216` | the whole block; upward-only, no regression in it |
| 2 | `:core-domain` gains `WorkItemSpecification` | `0 0 2505 216` -> `0 0 2654 238` | the whole block **including the note added by hand after erasure 1** |
| 3 | nothing changed | `:core-domain 0 0 2654 238 (unchanged)` | the whole block — the `bean:0065` no-op shape, reproduced deliberately |
| 4 | `successCriteria` deleted, with `-Pcoverage.regress` | the new `# REGRESSION accepted` pair | **both** regression blocks then on `main`, while recording the third |
| 5 | rebase onto `99212fc`, row re-derived | the new figures | the whole block, restored from the post-PR-83 `main` |
| 6 | rebase onto `36581a6`, row re-derived | the new figures | 33 comment lines and **all four** `REGRESSION` blocks, down to 7 lines and none |

Erasure 2 is the one that sets the mitigation's price: the note written by hand to record
erasure 1 was itself erased by the very next write. Erasure 4 is the one worth having — it is
the "erases a PREVIOUS regression block when recording a new one" clause, which `bean:0147`
found independently the same week, so it is now observed twice rather than inferred once.

Erasure 6 says something the other five do not. The branch was green, the row was correct,
and the only reason the baseline was touched at all was that `main` had moved. A contributor
who rebases and re-runs the writer — which `AGENTS.md` tells them to do, in preference to
hand-merging the numbers — destroys the file's history as a *direct consequence of following
the documented procedure*.

All six restored with `git show origin/main:config/coverage/baseline.tsv`, per `AGENTS.md`'s
rule against restoring from a `cp` taken earlier — which matters here, because a copy taken
before erasure 1 would have restored a block already missing what erasure 2 removed.
