---
# modus-0033
title: coverageBaselineWrite erases the recorded reason for an earlier regression
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# coverageBaselineWrite erases the recorded reason for an earlier regression

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

Success criteria:

- A recorded regression reason survives every later baseline write that does not itself
  regress that module. Observed failing first, per `doc:00-constitution#observed-failing`:
  write a baseline with a regression, write another without one, assert the reason is still
  there.
- Decide and record whether a reason should expire — a module whose figures have moved
  twice since may no longer be explained by it. Preserving forever and preserving until the
  module's row next changes are both defensible; silently dropping on any write is not.
- The `// The reason lives in the file…` comment at `modus.coverage.gradle.kts:244` is
  corrected or deleted. It is the claim this bean disproves.
