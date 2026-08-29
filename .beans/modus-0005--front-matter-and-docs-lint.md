---
# modus-0005
title: Front-matter back-fill and docs-lint
status: completed
type: task
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0004]
---

# Front-matter back-fill and docs-lint

`doc:05-authoring-for-agents` defines front-matter, one reference scheme and nine checks.
Before this work package only `documentation/05-authoring-for-agents.md` carried
front-matter, so `read_when` selected nothing and all nine checks were an
`Enforcement gap:`. Closes the four back-fill and enforcement follow-ups of `bean:0004`.

## Success criteria and evidence

| # | criterion | evidence | result |
|---|---|---|---|
| 1 | Every file under `documentation/` carries schema-valid front-matter | 14 files: 11 `documentation/*.md`, 3 ADRs; `docs-lint` checks 1, 2, 3, 7 pass over all of them | met |
| 2 | `read_when` selects a strict subset per task, and the selection is reproducible | the six briefed scenarios select 5, 7, 6, 6, 5 and 4 of 14 documents; predicates and matches recorded in the PR body | met |
| 3 | `provides` anchors are unique repo-wide and declared where they are provided | 72 anchors, no bare-name collision; `docs-lint` checks 4 and 5 pass | met |
| 4 | Every `doc:`/`bean:`/`adr:`/`rule:` reference resolves to exactly one target | 189 distinct references resolved; one pre-existing break found and fixed (below) | met |
| 5 | Each check is proved to fire | 12 mutations, one per check plus 3 second cases; each observed failing and reverted | met |
| 6 | Local and CI run the same gate | `docsLint` is a dependency of `qualityCheck`, which `rule:ci/build` invokes verbatim; there is no separate CI step to drift | met |
| 7 | `CLAUDE.md` holds no duplicated content | 5 lines, no rule of its own; the module graph, dependency rules, style rules and workflow it copied now live only in `AGENTS.md`, `settings.gradle.kts` and `documentation/` | met |
| 8 | The module list has one home | `settings.gradle.kts` no longer instructs the reader to keep a second copy in sync, because the second copy is gone | met |

Evidence for 5 — one mutation per check, each reverted:

| check | mutation | observed |
|---|---|---|
| 1 | delete the opening `---` of `70-skills.md` | `FAIL check 1  documentation/70-skills.md: no front-matter block` |
| 2 | add unknown key `owner:` | `FAIL check 2  documentation/70-skills.md: unknown key 'owner'` |
| 2 | `id:` renamed to `71-skills` in `70-skills.md` | `FAIL check 2  … does not match the filename (expected 'doc:70-skills')` |
| 3 | `status: superseded`, `superseded_by: null` | `FAIL check 3  … status is superseded but superseded_by is null` |
| 4 | `70-skills` provides `#budgets`, which `60-cost-model` owns | `FAIL check 4  anchor '#budgets' is provided by more than one document: …` |
| 5 | remove `<a id="skill-lifecycle">` from its heading | `FAIL check 5  … provides '#skill-lifecycle' but no heading declares <a id="skill-lifecycle">` |
| 5 | add `<a id="undeclared">` to a heading | `FAIL check 5  … declares <a id="undeclared"> but does not provide it` |
| 6 | `depends_on` cites a `doc:` id that names no file | `FAIL check 6  … resolves to 0 files, expected exactly 1` |
| 6 | reference to an anchor `40-durability.md` does not provide | `FAIL check 6  … documentation/40-durability.md does not provide '#…'` |
| 7 | `- when you are being careful` in `read_when` | `FAIL check 7  … is neither a path: nor a task: predicate` |
| 7 | `- path: "**"` | `FAIL check 7  … read_when uses the banned predicate 'path: **'` |
| 8 | lower `max_lines` to 400 in `documentation/README.md` | `FAIL check 8  documentation/10-architecture.md: 441 lines, over the 400 ceiling` |
| 8 | append 40 lines to `AGENTS.md` | `FAIL check 8  AGENTS.md: 135 lines, over the 120 ceiling` |
| 9 | replace the derived row's citation with `path: documentation/**` | `FAIL check 9  AGENTS.md:22: derived row states a path:/task: value instead of citing its doc: id` |

## Found by the checks, fixed here

| finding | fix |
|---|---|
| `doc:05-authoring-for-agents#reference-syntax` cited the `DomainScopedRoute` rule under the `rule:detekt/` kind; `doc:30-code-style#custom-detekt-rules` specifies that rule but `config/detekt/detekt.yml` does not declare it, so the example resolved to 0 targets | the row cites `rule:detekt/CyclomaticComplexMethod`, which the config declares, and states that a specified-but-undeclared rule is not a target |
| `doc:05-authoring-for-agents`'s own `read_when` regex `write\|edit\|review .*(document\|…)` alternates at the top level, so the bare word "write" selected it for any writing task | parenthesised: `(write\|edit\|review).{0,30}(document\|…)` |
| `documentation/README.md`'s 250-line floor was violated by `README.md` (107) and `05-authoring-for-agents.md` (178) at the moment check 8 was written | `adr:0003-document-line-budget`: ceiling only, no floor; both bounds are typed fields read by check 8 |
| ADR bodies carried `**Status:**` and `**Superseded by:**` bullets that front-matter now owns | bullets removed; `adr:0001-record-architecture-decisions#adr-format` cites front-matter instead of describing a second header block |

## Decisions

| decision | reason |
|---|---|
| `docs-lint` is bash in `tools/`, not a Kotlin `JavaExec` | the checks match lines and globs; a source set, a toolchain and a test fixture would buy nothing, and bash already runs in `rule:ci/build` |
| the front-matter parser accepts exactly the serialisation `doc:05-authoring-for-agents#front-matter` shows | a parser that accepts every YAML spelling of one document lets two spellings drift |
| check 4 compares bare anchor names, not qualified ones | check 2 forces every `provides` entry to carry its own document's id, so a qualified duplicate cannot occur and a check on it could never fire |
| a reference is recognised only with its full fixed-width id | `bean:NNNN` in a template and `doc:0` quoted in `bean:0004` are not references; a short id is not silently resolved to a prefix match |
| `documentation/README.md` and `documentation/80-agent-operating-procedure.md` are `read_when: always` | README defines MUST/SHOULD/MAY and `Enforced by:`, without which no other document is readable as written; 80 is the agent loop |
| `doc:README` and `adr:NNNN-slug` ids are accepted despite the `doc:<NN>-<slug>` shape in the schema table | the field's stated rule is "equals the filename, prefixed by its kind"; the shape column was written for `doc:` only |

## Follow-ups

- Front-matter `status` has no value for an ADR that is `Deprecated` or `Rejected`
  (`adr:0001-record-architecture-decisions#adr-format`, statuses).
- Check 6 cannot see a bare file path used where a reference exists
  (`doc:05-authoring-for-agents#reference-syntax`); the documents still cite each other as
  `40-durability.md §3`.
- `doc:05-authoring-for-agents#reference-syntax` gives no way to quote a broken reference
  as evidence; the mutations above are recorded in prose because writing them verbatim
  fails check 6 in the file that reports them.
