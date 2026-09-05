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
reasonable, careful thing to do, and it is the worst thing to do. Every one of the six
restorations so far was caught by a human or agent reading the diff by eye.

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
- **Eight restorations by hand, none by a mechanism.** `bean:0032`, `bean:0030`, twice in
  `bean:0036`, `bean:0065`, and twice in `bean:0147`.

### One more success criterion, from the eighth observation

- **A write that records a new regression preserves every regression block already in the
  file.** Observed failing first: record a regression for module A, then record one for
  module B, and assert A's reason is still present. This is not implied by the first
  criterion above — that one covers a later write which does *not* regress, and this branch
  passed it while failing this one. A writer that appends its note to the existing blocks
  satisfies both; one that rebuilds from a constant header satisfies neither.
