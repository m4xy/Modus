---
title: Agent-optimal authoring conventions
status: in-progress
type: task
priority: high
created_at: 2026-08-28T00:00:00Z
---

# Agent-optimal authoring conventions

Documents in this repository are consumed by agents. Define the authoring standard —
front-matter, references, one-fact-one-place, prose ban, mechanical checks — and ship the
router and the PR contract that use it.

## Scope

Owned: `AGENTS.md`, `documentation/05-authoring-for-agents.md`,
`.github/pull_request_template.md`, this bean.

Not owned, not touched: every other `documentation/*.md`, `documentation/README.md`,
`CLAUDE.md`, all build and source files. Concurrent work packages hold them.

## Problem, measured

| observation | figure | source |
|---|---|---|
| review threads on the documentation package | 8 | PR #1 |
| of those, caused by one fact restated in 2–4 documents and drifting | 6 | PR #1 review threads |
| package size at review | 13 files, +3458 lines | PR #1 |
| peak agent context per work package against a 300k ceiling | 268k | `doc:00-constitution` §6 |

Cause of the 268k: no document declared when it could be skipped, so all were read.

## Success criteria and evidence

| # | criterion | evidence | result |
|---|---|---|---|
| 1 | Front-matter schema defined field by field, with types and a validity rule per field | `doc:05-authoring-for-agents#front-matter` — 7 fields, worked example | met |
| 2 | `read_when` predicates matchable by a program against a changed-file path or a task description | `#read-when` — two predicate kinds (`path:` glob, `task:` regex), the `always` scalar, a 5-step selection algorithm | met |
| 3 | One reference scheme covering documents, anchors, beans, ADRs and rules, with resolution | `#reference-syntax` — 7-row resolution table; `rule:archunit/*` identifiers verified verbatim against `architecture-tests/` | met |
| 4 | One-fact-one-place stated as a prohibition, with the evidence | `#one-fact-one-place` — 6-row drift table naming every restated fact and its outcome | met |
| 5 | Prose ban enumerated, each banned form paired with its replacement | `#prose-ban` — 8 banned forms, 5 required forms | met |
| 6 | Documentation / bean / ADR split stated | `#bean-split` — 3-row ownership table plus 4 rules | met |
| 7 | Mechanical checks specified, each decidable from repository contents | `#checks` — 9 checks, all carrying `Enforcement gap:` | met |
| 8 | `AGENTS.md` routes and does not duplicate `documentation/` | 56 lines; a 2-row routing table for what `documentation/README.md` does not cover, no row restates a README row or a `read_when` predicate value | met |
| 9 | PR template is typed and bounds the reviewer's search | 55 lines; `out_of_scope` and `review_focus` present, purpose stated in an HTML comment; `out_of_scope` bounded against the diff itself (PR #4 thread 5) | met |
| 10 | Terse enough that the PR does not refute its own thesis | `wc -l`: 56 + 178 + 55 = 289 added lines | met |
| 11 | The standard is applied to itself | this bean and this PR's body are written to it; `AGENTS.md` is 56 lines against its own 120-line check | met |

Evidence for 10:

```
$ wc -l AGENTS.md documentation/05-authoring-for-agents.md .github/pull_request_template.md
      56 AGENTS.md
     178 documentation/05-authoring-for-agents.md
      55 .github/pull_request_template.md
     289 total
```

## Review cycle

PR #4, first review: verdict CHANGES REQUIRED, 5 threads. All 5 fixed, replied and resolved
before merge; `main` was merged in first (PR #1 had landed, so `documentation/README.md` and
`documentation/00`–`80` now exist and `doc:` references resolve).

| thread | file:line | root cause | fix |
|---|---|---|---|
| 1 | `AGENTS.md:26` | the routing table was a hand-written second copy of `documentation/README.md`'s reading-order table, so a row could narrow README's trigger and nothing would catch it | the table now points to README for every `doc:00`–`doc:80` row instead of restating any of them; the one row left states a fact README does not carry and cites its source instead of copying predicate values — there is no second copy left to drift |
| 2 | `AGENTS.md:57` | banned history prose, and a second copy of the 268k figure already owned here | replaced with the rule plus a `bean:0004` reference |
| 3 | `doc:05-authoring-for-agents#reference-syntax` | `name` had no minimum length (`doc:0` matched two files), and no required agent behaviour was stated for 0 or ≥2 matches | `name` now MUST carry the full fixed-width numeric id; an agent hitting zero or multiple matches MUST stop and report, never guess |
| 4 | `doc:05-authoring-for-agents#checks` check 9 | compared free prose to typed predicates with no equivalence algorithm, and had nothing to diff against on 8 of 9 documents | redefined as a textual check on `AGENTS.md` alone: a derived row MUST cite its `doc:` id and MUST NOT itself state a `path:`/`task:` value; decidable today, independent of front-matter landing elsewhere |
| 5 | `.github/pull_request_template.md:22` | `out_of_scope` had no limit, so it could suppress review of the diff's own changes | added: `out_of_scope` MUST NOT name anything the diff changes; a reviewer MUST still evaluate every changed line |

Also fixed: check 8's 400-line ceiling conflicted with `documentation/README.md`'s 250–500
range — `documentation/40-durability.md` is 419 lines and would have failed check 8 once
implemented. Check 8 now cites README's range instead of a second hard-coded number, so the
two cannot re-drift.

## Decisions

| decision | reason |
|---|---|
| `read_when` entries are typed predicates (`path:` / `task:`), not prose lines | a prose line cannot be matched against a changed-file set, so it cannot make skipping safe |
| `read_when` accepts the scalar `always` | `path: "**"` would make an unconditional document indistinguishable from a broad one |
| `provides` anchors are fully qualified (`doc:40-durability#atomic-write`), not bare (`durability#atomic-write`) | a bare namespace is a second name for a document that already has `id`; two names drift |
| document-level `enforced_by` dropped | enforcement is per rule, and `documentation/README.md` already puts `Enforced by:` on the rule line; a document-level copy is restatement, which is what this bean exists to stop |
| `depends_on` explicitly not transitive for reading | transitive expansion selects the whole package and reproduces the 268k overrun |
| the `AGENTS.md` routing table restates nothing `documentation/README.md` already states, and its one derived row cites `doc:05-authoring-for-agents` instead of copying its predicates | a restated predicate value is a second copy that can silently narrow the source (PR #4 thread 1); citing the id instead of the value removes the drift vector rather than merely labelling it |

## Follow-ups

- Add `doc:05-authoring-for-agents` to the reading-order table in `documentation/README.md`.
- Reduce `CLAUDE.md` to a pointer to `AGENTS.md`.
- Implement checks 1–9 as a `docs-lint` step in `rule:ci/build`.
- Back-fill front-matter into `documentation/00`–`80` and the ADRs — PR #1 is merged, `doc:` references resolve on `main`, but `read_when` is still absent on those nine files, so check 9 has only `doc:05-authoring-for-agents`'s row to apply to.
- Retro-fit the PR template to PRs #2 and #3 after their reviews close. PR #1 is retro-fitted in this work package.
