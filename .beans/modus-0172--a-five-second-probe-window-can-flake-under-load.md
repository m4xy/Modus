---
# modus-0172
title: The ordering probe's five-second window can flake red under load
status: todo
type: fix
priority: normal
order: CM
created_at: 2026-09-05T00:00:00Z
---

# The ordering probe's five-second window can flake red under load

Raised by the review of `bean:0147`.
`PathLockingIntegrationTest.the multi-lock helper acquires in canonical order` asserts that a
third thread takes a free lock within `PROBE_WINDOW_SECONDS = 5`. With the ordering in place
that thread contends with nobody and should acquire in microseconds, so five seconds is four
or five orders of magnitude of headroom — and it is still a wall-clock assertion on a shared
CI machine, which is the shape that flakes.

**Read the history before changing the number.** That test was rewritten twice, and both
earlier versions were *timing-tuned* and both were wrong: the first let the mutation it was
written to kill pass outright, the second killed it through a later assertion while the
assertion its own comment named still passed. The third version's contribution was removing
the race, not picking a better constant — nothing in it times out, and the latch is the only
bound. Raising `PROBE_WINDOW_SECONDS` is therefore the *fourth* tuning of a test whose lesson
was that tuning was the wrong instrument, and this bean should say so if that is where it
lands.

The honest options, none chosen here:

| option | cost |
|---|---|
| raise the window | the cheapest, and it weakens nothing: under the mutation the lock is held for sixty seconds, so any window well under that still discriminates. It is still a wall-clock number |
| assert the *state* rather than the wait — the probing thread reaches its action while the helper has not | needs a way to observe "the helper is blocked on a specific lock", which `ReentrantReadWriteLock` exposes only through `hasQueuedThreads`, and building on that couples the test to an implementation detail |
| accept it and quarantine on flake | `doc:30-code-style` §5.1 requires a disabled test to name a work item; a flaky test that is never disabled names nothing |

## Success criteria

| # | criterion |
|---|---|
| 1 | The test either stops depending on a wall-clock bound, or the bound is justified against the sixty-second hold on the other side and the justification is in the test rather than in this bean |
| 2 | Whichever is chosen, `.sorted()` removed from `PathLocks.exclusiveAll` still fails **at the assertion whose comment describes the property** — the condition two earlier versions failed to meet. Verified by the line number in the JUnit XML, not by the console summary, because two assertions in that test emit the identical message |
| 3 | The unmutated test passes on a machine under deliberate load, not only on an idle one |
| 4 | `./gradlew qualityCheck` green |
