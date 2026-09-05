---
# modus-0151
title: Two test hazards this repository has now met and written down nowhere
status: todo
type: fix
priority: normal
order: CJ
created_at: 2026-09-05T00:00:00Z
---

# Two test hazards this repository has now met and written down nowhere

`bean:0147`'s evidence pass found two defects in **its own tests** before it found any in the
code it was testing. Both are general, both are cheap to state, and `doc:35-testing` says
nothing about either. They are recorded in `bean:0147` under
"What this evidence pass found in its own tests"; this bean moves them into the document,
which is where a rule lives (`doc:05-authoring-for-agents#bean-split`).

## The two hazards

**1. A test that asserts a file was NOT written writes that file whenever it fails.** So the
path it names must be one the harness deletes. `bean:0147`'s store-escape tests took their
outside path from `root.parent` — the shared system temp directory for a JUnit `@TempDir` —
so one planted mutation left a file there and the next **five** unrelated mutation runs all
reported those two tests red for a reason that had nothing to do with what they had broken.
Five plants agreeing on a failure none of them could have caused is what made it visible; a
single red run would have read as a real kill.

**2. When a concurrency test bounds two waits with one number, the verdict comes from
whichever expires first, and that is not the property under test.** `bean:0147`'s
ordered-acquisition test held one lock for ten seconds and probed another with a ten-second
timeout. With the ordering deleted from the code, the holder's own await expired first and
the probe acquired the lock 250 ms inside its window: **the mutation survived the test
written to kill it**. The second version shortened the probe and killed the mutation through
a *later* assertion, while the assertion its own comment pointed at still passed — green for
the wrong reason, with a comment describing behaviour it was not measuring. The fix is not a
better number: bound the observation with a latch and make the lock timeout a figure nothing
reaches.

## Why this is not a documentation edit in `bean:0147`'s own pull request

`documentation/README.md` sets `max_lines: 500` for `documentation/*.md` and
`doc:35-testing` is **exactly** 500 lines. Adding to it is impossible without splitting it,
and README says a file that outgrows the ceiling is two files — a split whose section numbers
may never be reallocated, because a `completed` bean cannot be corrected
(`adr:0005#finalisation`). That is its own change with its own review surface, so
`bean:0147` recorded the findings and raised this.

## Success criteria

| # | criterion |
|---|---|
| 1 | Both hazards are stated in `documentation/`, as rules with an observable trigger, not as anecdotes — `doc:35-testing#load-bearing-evidence` is the nearest owner and `#fixture-variation` is the nearest sibling |
| 2 | The line budget is respected. Either `doc:35-testing` is split — each half keeping the section numbers its sections already had — or the additions are offset by material that belongs in an ADR or a skill |
| 3 | Hazard 1 carries the mechanical question it raises: nothing stops a test writing outside its `@TempDir`, and whether that is checkable at all is answered rather than left open |
| 4 | Hazard 2 is stated so it discriminates. "Do not use sleeps" already exists as `rule:archunit/nothingSleepsTheThread` and would not have caught either version of the test that failed here — both used latches throughout |
| 5 | `./gradlew qualityCheck` green |
