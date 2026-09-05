---
# modus-0155
title: docsLintGateTest failed 8 of 168 once and has not reproduced
status: todo
type: fix
priority: medium
order: AQ
created_at: 2026-09-05T00:00:00Z
---

# docsLintGateTest failed 8 of 168 once and has not reproduced

**This is a could-not-reproduce report, and it is written down rather than dropped because a
gate suite that fails transiently is a gate nobody can trust a red from.** A red run that
might be noise gets re-run instead of read, which is how a real failure gets waved through.

## What was observed

One `./gradlew qualityCheck` run during `bean:0152`, on `feat/work-item-aggregate`:

```
docs-lint-gate-test: 160 passed, 8 failed, over 2 bash major version(s).
> Task :docsLintGateTest FAILED
```

The eight failures were the same assertion under both bash majors, and each carried a
stderr line the assertion did not expect:

```
     FAIL check -  line 787: a command exited 1 and nothing checked it: 'cd /no/such/dir/__probe_failed_cd__'
     (it wrote 1 line(s) to stderr; the first is: mktemp: mkdtemp failed on /no/such/dir/__probe_no_tmpdir__/rl6y8p: No such file or directory)
```

Note the mismatch inside that record: the command named is the `__probe_failed_cd__` plant,
and the stderr quoted is from the `__probe_no_tmpdir__` plant. Two different plants appear
in one record.

## What is known, and what is not

Known:

- The tree was byte-identical to `origin/main` for `tools/`, `build.gradle.kts` and
  `build-logic/` — `git diff --stat origin/main -- tools build.gradle.kts build-logic`
  was empty. Nothing in `bean:0152`'s diff touches the gate or its test.
- Every subsequent run in the same worktree passed `168 passed, 0 failed`, including four
  full `qualityCheck` runs.
- The run that failed was the same run in which `docsLint` itself failed check 12, and in
  which `:core-domain:detekt` and `:architecture-tests:test` also failed. Whether that is
  related is **not** known.
- Other agents were running concurrently in sibling worktrees, some of them on docs-lint
  itself (`bean:0123` through `bean:0127`).

Not known, and deliberately not asserted: the cause. The suite backgrounds mutated copies
of `tools/docs-lint.sh` and runs them against temporary directories, so contention between
concurrent runs is *a* mechanism that would fit — but nothing was observed that
distinguishes it from any other, and this bean does not claim it
(`doc:00-constitution#observed-failing` binds a diagnosis as it binds a gate).

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The two-plants-in-one-record shape above is either reproduced or shown to be impossible in a single-run execution | test-run |
| 2 | If the suite shares any mutable path between concurrent runs — a fixed temp directory, a fixed copy name, a fixed marker file — that sharing is named, and either removed or shown not to reach this assertion | citation |
| 3 | A run of the suite states enough to tell a real red from a transient one: which plant produced each record, and which copy the record came from | test-run |
| 4 | If the cause cannot be found, that is recorded here as a finding rather than the bean being closed as fixed | citation |

Criterion 4 is not a formality. `bean:0103` records that `git log` cannot answer whether a
defect existed; the same applies here, and closing this as "could not reproduce, assumed
fine" would be the failure this repository keeps naming.
