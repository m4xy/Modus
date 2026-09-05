---
# modus-0132
title: The Markdown document codec and the two identity repositories
status: todo
type: feature
priority: high
order: CG
created_at: 2026-09-05T00:00:00Z
parent: modus-0017
blocked_by: [modus-0130]
---

# The Markdown document codec and the two identity repositories

The third child of `bean:0017`: bullet 3, plus the codec it needs. `bean:0130` writes bytes
atomically; this bean decides what bytes a document is, and makes `ActorRepository` and
`PermissionGrantRepository` real. It is the first implementation of a declared port in this
repository, which is what `doc:15-repository-layout` §4.3 says makes `AdaptersImplementPorts`
and `NoFieldInjection` — two of the five rules its enforcement gap names, and neither
implemented — able to fire for the first time. Written in plain backticks and not as a
`rule:archunit/…` reference, because a typed reference to a rule that does not exist is a
`docs-lint` check 6 failure (`doc:05-authoring-for-agents#reference-syntax`).

## The design question this bean must answer before it starts

`Actor` and `PermissionGrant` have a private constructor and a named factory that **raises a
domain event** (`bean:0009` §"Decisions the documents did not already settle"). So a
repository reading one back off disk has no reconstitution path that does not also fabricate
an `ActorRegistered` or a `GrantIssued` that never happened — and `bean:0066` is adding the
drain that would then publish it. Reading a revoked grant is worse: `revoke` opens with
`check(!revoked)`, so replaying it to restore the flag raises `GrantRevoked` a second time.

This is a `core-domain` change and `core-domain` is outside this bean's scope, so it is a
decision for the orchestrator rather than a unilateral edit. The three shapes, none chosen
here:

| option | cost |
|---|---|
| a `reconstitute` factory on each aggregate, raising nothing | widens the aggregate's API with a constructor only persistence may call; `doc:20-ddd-practices` has no rule about one either way |
| the repository drains and discards `pendingEvents` after constructing through the public factory | the discard is invisible at the call site and silently depends on the factory raising exactly one event |
| the repository stores and replays the event stream | event sourcing, which `adr:0002` did not choose and `doc:40-durability` §2.1 contradicts — the document *is* the record |

## Success criteria

| # | criterion | evidence |
|---|---|---|
| 1 | §2.1: a document is Markdown with YAML frontmatter, the file **is** the record, read whole and written whole, at the §3 layout paths — `identity/actors/<actorId>.md`, `identity/grants/<grantId>.md` | |
| 2 | §8: serialisation is canonical and deterministic — stable key order, stable list order, LF, no trailing whitespace, one trailing newline. The round-trip property the enforcement gap names: parse → serialise → parse yields an identical record **and identical bytes** | |
| 3 | §8: a one-field change produces a one-line diff. Prose is never reflowed and the body's whitespace is never rewritten | |
| 4 | Frontmatter is validated against an explicit schema on every read and every write; a document that fails is quarantined to `.modus/quarantine/`, never auto-repaired (§2.1.5, §7) | |
| 5 | `ActorRepository`: `findById` returns null for absent and **throws** for unreadable, as its KDoc requires — the two are not the same answer | |
| 6 | `PermissionGrantRepository`: all four methods, returning `List` and not `Set`, honouring the duplicate-`GrantId` rule of `bean:0009` review thread 2 — at most one instance per `GrantId`, and that instance the current one. A store that can return a live alias beside a revoked one is the privilege escalation that thread records | |
| 7 | An unreadable grant store throws rather than reading as an empty set; the KDoc says why, and the test asserts the throw and not the denial | |
| 8 | Round-tripping a `PermissionGrant` preserves revocation and preserves **two or more** capabilities — `doc:35-testing#fixture-variation`, and the exact uniformity that hid `bean:0009` thread 1 | |
| 9 | The reconstitution question above is answered by a recorded decision, not by whichever shape compiled first | |
| 10 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite` | |
