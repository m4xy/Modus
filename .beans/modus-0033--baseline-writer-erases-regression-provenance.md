---
# modus-0033
title: coverageBaselineWrite erases recorded provenance on any write, including one that changes nothing
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# coverageBaselineWrite erases recorded provenance on any write, including one that changes nothing

`config/coverage/baseline.tsv`'s own header states:

> The writer REFUSES to raise a missed count; a deliberate regression needs
> `-Pcoverage.regress=<reason>`, and **the reason is recorded below**.

It is recorded below only until the next baseline write, whatever that write is about.
`build-logic/src/main/kotlin/modus.coverage.gradle.kts:258` rebuilds the file from a
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

The mechanism is plain once stated in this order (`modus.coverage.gradle.kts:246`, `:258`):
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
- The `// The reason lives in the file…` comment at `modus.coverage.gradle.kts:244` is
  corrected or deleted. It is the claim this bean disproves.
