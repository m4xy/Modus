---
# modus-0153
title: A test's unbounded readLine on a child process can hang the build
status: todo
type: fix
priority: normal
order: CL
created_at: 2026-09-05T00:00:00Z
---

# A test's unbounded `readLine` on a child process can hang the build

Raised by the review of `bean:0147`. `CrossProcessLockIntegrationTest.startLockHolder`
launches a second JVM and blocks on `process.inputStream.bufferedReader().readLine()` to
learn that the child has taken the cross-process lock. The handshake is deliberate and
correct — a pipe is a real signal, where polling for a marker file is a timer with a race
attached, and `rule:archunit/nothingSleepsTheThread` binds the whole repository.

What it lacks is a bound. `readLine()` returns when the child writes a line **or** closes its
stdout. A child that starts, does neither, and does not exit — the JVM fails to launch into a
state where it can exit, a lock acquisition blocks in the kernel, the machine is loaded past
the point where the child gets scheduled — leaves the test thread blocked with nothing to
time it out. JUnit has no per-test timeout configured in this repository, so the failure mode
is the whole build hanging until CI's job timeout kills it, with no output naming the cause.

Not observed happening; raised from reading. That is stated plainly rather than dressed up:
the failure has not occurred, and the argument for fixing it is that its symptom — a job
killed at the CI timeout with no failing test — is among the most expensive to diagnose.

## Success criteria

| # | criterion |
|---|---|
| 1 | The wait for the child's readiness line is bounded, and exceeding the bound fails the test with a message naming the child process and what it was waiting for — not a hang and not a bare `null` |
| 2 | The bound does not reintroduce polling or sleeping. A `Future` on a single-thread executor with `get(timeout)`, or `Process.waitFor(timeout)` composed with the read, both keep the pipe as the signal |
| 3 | The child is destroyed when the bound is exceeded, so a stuck JVM does not outlive the build that started it |
| 4 | Observed: a deliberately mute child — one that acquires nothing and writes nothing — makes the test fail inside the bound, with the message criterion 1 requires. This is the plant; without it the bound is a claim |
| 5 | Whether the same bound belongs on every test in the repository, as a JUnit global timeout rather than one test's local fix, is answered rather than left open |
| 6 | `./gradlew qualityCheck` green |
