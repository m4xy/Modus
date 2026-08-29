---
# modus-0035
title: docs-lint must validate the bean graph, not just prose references
status: todo
type: fix
priority: high
order: AE
created_at: 2026-08-29T00:00:00Z
---

# `docs-lint` must validate the bean graph, not just prose references

`tools/docs-lint.sh` resolves typed `bean:NNNN` references **in prose**. It never reads
`blocked_by` or `parent` front-matter, so the dependency graph that `AGENTS.md` step 1
selects work from is unchecked by anything.

Review of `bean:0012`'s split found a live consequence: converting a bean to `type: epic`
while three others carried `blocked_by: [modus-0012]` would have made all three permanently
unselectable. Step 1 skips epics, so the epic is never worked, so it never becomes
`completed`, so the edges never clear. Every file was individually well-formed and
`docs-lint` was green.

```
cmd:      bash tools/docs-lint.sh
observed: docs-lint: OK — 16 documents, 87 anchors, 491 references.
          (with bean:0013, bean:0018 and bean:0023 all deadlocked behind an epic)
```

## Success criteria

Each check observed rejecting a planted violation before it is claimed
(`doc:00-constitution#observed-failing`).

1. Every `blocked_by` and `parent` id resolves to a bean file that exists.
2. **No `blocked_by` edge names a `type: epic` bean.** An epic may have children and edges
   of its own; it may never be the target of one, because nothing will ever complete it.
3. The `blocked_by` graph is acyclic. A cycle is the same deadlock reached a longer way
   round, and `bean:0011` already had to sequence `memory`/`execution` by hand to avoid one.
4. `order` values are distinct among beans that can compete — same `priority`, all
   `blocked_by` satisfied, not `type: epic`. A collision is not fatal, but it makes step 1's
   tiebreak arbitrary, which is exactly what `order` was added to `bean:0010` to prevent.
5. At least one `todo` bean is selectable. A backlog where step 1 returns nothing is the
   failure this bean exists to catch, and it is silent today.

## Note on scope

`bean:0025` covers `docs-lint` resolving `rule:archunit` references. This is the same tool
and a different gap; keep them separate — that bean is about a reference type, this one is
about front-matter the tool does not parse at all.
