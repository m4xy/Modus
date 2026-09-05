---
# modus-0161
title: The layout tree and the placement table drift from settings.gradle.kts
status: todo
type: fix
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# The layout tree and the placement table drift from settings.gradle.kts

`settings.gradle.kts` says it plainly:

> This list is the one home for the module set: no document restates it
> (`doc:05-authoring-for-agents#one-fact-one-place`).

Two documents restate it anyway, twenty lines apart in the same file:

| site | shape |
|---|---|
| `doc:15-repository-layout#repository-layout` §2 | a directory tree, one line per module, with a description |
| `doc:15-repository-layout#placement-table` §2.1 | "you are writing X, it goes in Y", one row per destination |

Neither is derived. Adding a Gradle module means editing `settings.gradle.kts` and then
remembering two more places, and `bean:0066` demonstrated the failure mode by adding
`adapter-events-inprocess` to §2.1's table — the one the section's own instruction names —
and leaving §2's tree listing four adapters where there are five. It was found in review, by
a human reading the diff, which `doc:00-constitution#mechanical-enforcement` says is a bug.

This is the **seventh** instance of a hand-maintained enumeration in `documentation/` going
behind the set it enumerates. The recurrence is the argument: six previous fixes were each a
correction to one enumeration, and the seventh arrived anyway.

## What would close it

A `docs-lint` check that parses `module("<name>", "<path>")` out of `settings.gradle.kts` and
requires every name to appear in §2's tree and in §2.1's table, and every module-shaped entry
in either to be a real module. The parse is a regex over one file with a fixed call shape, and
`docs-lint` already reads both documents.

Note what the check must **not** do: assert the two documents agree with each other. They
would agree perfectly while both being stale, which is the state §2 was in for the whole of
`bean:0066`'s review. `settings.gradle.kts` is the referent.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | A module in `settings.gradle.kts` and absent from §2's tree fails `docs-lint`, observed failing on a planted removal and reverted (`doc:00-constitution#observed-failing`) | |
| 2 | The same for §2.1's table, planted separately — one plant passing both arms proves only that one arm works | |
| 3 | A module-shaped entry in either document naming no real module also fails, so the check is not satisfiable by adding text | |
| 4 | The check reports what it examined — how many modules it parsed — because a run that parses zero and a run that passes both print `OK` (`bean:0051`) | |
| 5 | §2's `Enforcement gap:` line naming this bean is replaced by an `Enforced by:` | |
| 6 | `./gradlew qualityCheck` green | |
