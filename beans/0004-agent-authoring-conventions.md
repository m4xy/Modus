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
| 8 | `AGENTS.md` routes and does not duplicate `documentation/` | 64 lines; 11-row routing table; commands cite `doc:` and `rule:` references, no rule text | met |
| 9 | PR template is typed and bounds the reviewer's search | 51 lines; `out_of_scope` and `review_focus` present, purpose stated in an HTML comment | met |
| 10 | Terse enough that the PR does not refute its own thesis | `wc -l`: 64 + 180 + 51 = 295 added lines, against a 400 ceiling | met |
| 11 | The standard is applied to itself | this bean and this PR's body are written to it; `AGENTS.md` is 64 lines against its own 120-line check | met |

Evidence for 10:

```
$ wc -l AGENTS.md documentation/05-authoring-for-agents.md .github/pull_request_template.md
      64 AGENTS.md
     180 documentation/05-authoring-for-agents.md
      51 .github/pull_request_template.md
     295 total
```

## Decisions

| decision | reason |
|---|---|
| `read_when` entries are typed predicates (`path:` / `task:`), not prose lines | a prose line cannot be matched against a changed-file set, so it cannot make skipping safe |
| `read_when` accepts the scalar `always` | `path: "**"` would make an unconditional document indistinguishable from a broad one |
| `provides` anchors are fully qualified (`doc:40-durability#atomic-write`), not bare (`durability#atomic-write`) | a bare namespace is a second name for a document that already has `id`; two names drift |
| document-level `enforced_by` dropped | enforcement is per rule, and `documentation/README.md` already puts `Enforced by:` on the rule line; a document-level copy is restatement, which is what this bean exists to stop |
| `depends_on` explicitly not transitive for reading | transitive expansion selects the whole package and reproduces the 268k overrun |
| the `AGENTS.md` routing table is marked derived from `read_when` | the table is a second copy of the selection facts; marking it derived makes the front-matter normative and the table the bug on conflict |

## Follow-ups

- Add `doc:05-authoring-for-agents` to the reading-order table in `documentation/README.md`.
- Reduce `CLAUDE.md` to a pointer to `AGENTS.md`.
- Reconcile the 400-line budget (`#checks` check 8) with the 250–500 line range in `documentation/README.md`.
- Implement checks 1–9 as a `docs-lint` step in `rule:ci/build`.
- Back-fill front-matter into `documentation/00`–`80` and the ADRs once PR #1 merges; until then every `doc:` reference in this package is unresolvable on `main`.
- Retro-fit the PR template to PRs #2 and #3 after their reviews close. PR #1 is retro-fitted in this work package.
