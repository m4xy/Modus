---
# modus-0111
title: The spend-record enforcement gap names no bean that closes it
status: todo
type: fix
priority: normal
created_at: 2026-09-03T00:00:00Z
---

# The spend-record enforcement gap names no bean that closes it

`doc:05-authoring-for-agents` §5 requires an `Enforcement gap:` line to name the bean that
closes it. `doc:60-cost-model` §3.2's gap — nothing compares the documented field list against
the records actually written — named `bean:0054` and `bean:0068`, and neither closes it.

```
cmd:      sed -n '187p' documentation/05-authoring-for-agents.md
observed: - An `Enforcement gap:` line MUST name the bean that closes it.
exit:     0

cmd:      grep -n '^status:' .beans/modus-0054--cost-baseline-and-run-recorder.md
observed: 4:status: completed
exit:     0

cmd:      sed -n '9,11p' tools/cost_lib.py
observed: Enforcement gap: nothing in `./gradlew qualityCheck` lints or tests this file. ktlint, Detekt
          and ESLint cover Kotlin and TypeScript; there is no Python gate and wiring one means editing
          build.gradle.kts, which bean:0054 does not own. Recorded, not glossed.
tree:     this branch, rebased onto origin/main at 9c9940d
exit:     0

cmd:      grep -c 'Enforcement' tools/docs-lint.sh tools/lib/docs-lint-c14.awk tools/lib/docs-lint-fence.awk
observed: tools/docs-lint.sh:0
          tools/lib/docs-lint-c14.awk:0
          tools/lib/docs-lint-fence.awk:0
exit:     1
```

`bean:0054` is `completed`, so it closes nothing further; `bean:0068` closes with the pull
request that records the drift, and recording a gap is not closing it. So the MUST was
satisfied in form and not in substance, and **nothing in `docs-lint` checks the rule** — it is
a MUST enforced by reading alone, which is the shape `doc:00-constitution#observed-failing`
treats as unenforced.

## Why the line is not simply deleted

An enforcement gap that stops being written down does not stop existing. The document would
read as though the field list were checked, which is the more expensive error: a reader who
knows a gap exists can price it, and a reader who is told nothing cannot.

## Two gaps, not one, and they nest

`tools/cost_lib.py:9-11` carries its own `Enforcement gap:` — no Python gate exists in
`./gradlew qualityCheck` at all, and wiring one means editing `build.gradle.kts`, which
`bean:0054` does not own. So the outer gap (nothing lints the file) is why the inner one
(nothing compares the field list to the records) cannot be closed by the file's own author.
Any fix that adds a comparison without a gate that runs it reproduces the defect one level up.

The recorder's own self-test shows what the missing comparison costs. It requires
`cacheWriteTokens` and never the two split fields — the fields `doc:60-cost-model` §3.2 was
corrected to name — so the halves are written and checked by nothing:

```
cmd:      grep -n 'cacheWrite5mTokens' tools/cost-record.py
observed: 255:        "cacheWriteTokens": usage["cacheWrite5mTokens"] + usage["cacheWrite1hTokens"],
          256:        "cacheWrite5mTokens": usage["cacheWrite5mTokens"],
exit:     0

cmd:      sed -n '343,345p' tools/cost-record.py
observed:     missing = [k for k in ("runId", "parentRunId", "role", "modelId", "effort", "inputTokens",
                                     "outputTokens", "cacheReadTokens", "cacheWriteTokens",
                                     "peakContextTokens", "outcome", "startedAt", "endedAt", "gitBranch")
exit:     0
```

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `doc:60-cost-model` §3.2.1's `Enforcement gap:` is gone because the drift is caught, not because the line was inconvenient — replaced by an `Enforced by:` naming what catches it | citation |
| 2 | A Python gate runs in `./gradlew qualityCheck`, or its absence is recorded as the blocker that it is with the bean that owns `build.gradle.kts` named | command |
| 3 | The comparison, once it exists, is observed failing on a planted mismatch between `USAGE_KINDS` and the §3.2 table — a gate never seen to fail is not yet a gate (`doc:00-constitution#observed-failing`) | test-run |
| 4 | Whether any other `Enforcement gap:` line in `documentation/` names a bean that cannot close it is answered rather than assumed; this one was found by reading, and reading does not scale | command |
| 5 | `docs-lint` gains the check, or it is recorded why the MUST stays unenforced — a rule whose only enforcement is review is what this bean is an instance of | test-run |
