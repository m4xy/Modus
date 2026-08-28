---
id: adr:0003-document-line-budget
title: Document line budget has a ceiling and no floor
status: active
superseded_by: null
read_when:
  - path: documentation/**
  - task: line budget|split a document|document length|min_lines|max_lines
provides:
  - adr:0003-document-line-budget#line-budget-decision
depends_on: [doc:05-authoring-for-agents]
---

# ADR 0003 — Document line budget has a ceiling and no floor

- **Date:** 2026-08-29
- **Deciders:** Modus core
- **Supersedes:** —

## Context

`doc:05-authoring-for-agents#checks` check 8 fails a `documentation/*.md` outside the line
range `doc:README#changing-this-package` states. That range carried a lower bound of 250
lines. Two of the eleven documents in the package are under it:

| file | lines at the time check 8 was written |
|---|---|
| `documentation/README.md` | 107 |
| `documentation/05-authoring-for-agents.md` | 178 |

Both are complete. Neither has a missing rule. The only way to satisfy a floor is to add
lines that state nothing new, which is the set of forms `doc:05-authoring-for-agents#prose-ban`
bans: summaries, recaps, motivation, examples that re-encode a rule already stated.

Removing a rule requires an ADR (`doc:README#changing-this-package`). This is it.

## Decision <a id="line-budget-decision"></a>

**The line budget is a ceiling only. The floor is removed.**

1. The ceiling is unchanged and stays enforced: a document over it is two documents, or it
   holds material belonging in an ADR or a skill.
2. There is no minimum length. A document is as long as its rules require.
3. Both values live in one place, `doc:README#changing-this-package`, as typed
   `key: value` fields. `docs-lint` check 8 reads them from there; no second copy exists.

## Consequences

### Positive

- Check 8 is satisfiable by the package as it stands, so it can be turned on today.
- Splitting a document no longer risks pushing either half below a floor.

### Negative

- A stub document — front-matter, a heading, no rules — now passes check 8. Checks 1, 2, 5
  and 6 still apply to it, and `provides` may be empty only for `status: draft`.

### Neutral

- The ceiling remains the only length signal that a document is doing two jobs.

## Alternatives considered

| alternative | rejected because |
|---|---|
| Keep the floor; pad the two short documents | padding is the banned prose forms, and check 8 would then reward the thing the prose ban exists to stop |
| Keep the floor; exempt `README.md` and `05` by name | a name-keyed exemption list is a second copy of the rule that drifts as documents are added |
| Keep the floor; merge the two short documents into longer ones | `README.md` is the index and `05` is the authoring spec; merging either into a neighbour makes `read_when` unable to skip it |
| Drop check 8 entirely | the ceiling catches the real failure — one document doing two jobs — and it is decidable |
