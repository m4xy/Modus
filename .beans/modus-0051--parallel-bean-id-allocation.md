---
# modus-0051
title: Two parallel agents allocated the same bean id
status: in-progress
type: fix
priority: high
order: AL
created_at: 2026-08-29T00:00:00Z
---

# Two parallel agents allocated the same bean id

Three agents worked three beans concurrently in isolated worktrees, each branched from the
same `main`. Two of them needed to raise a follow-up, both took the next free id by reading
`.beans/`, and both got **0048**:

| bean:0048 | raised by | subject |
|---|---|---|
| `extract-the-first-skills` | the orchestrator | the first three skills |
| `adr-immutability-versus-deferred-detail` | `bean:0041`'s implementer | `adr:0001` §3 versus a deferred ADR row |

Neither was wrong. `.beans/` is the allocator, it is read at branch time, and nothing
serialises two readers. The collision surfaced only when the second pull request failed to
merge — `docs-lint` was green on both branches, because on each branch the id *is* unique.
Check 12 validates the graph within one tree and cannot see a sibling branch.

This is the first defect that is a property of **parallel execution** rather than of any
change, and it will recur every time two agents raise a bean at once — which is the normal
case once Modus schedules its own work (`doc:00-constitution` §12).

## Success criteria

- A collision is refused rather than discovered at merge. The check runs where both sides
  are visible — a `main`-side hook, a CI job comparing against the base, or an allocation
  that cannot collide by construction.
- Decide between **detect** and **prevent**, and record why:
  - *detect*: `docs-lint` compares ids against the merge base and fails when a bean id it
    introduces already exists on `origin/main`. Cheap, catches it at push, still lets two
    open branches collide until one merges.
  - *prevent*: ids stop being sequential. `hmans/beans` upstream generates a short nanoid
    (`pkg/bean/id.go`, `NewID`) using `beans.id_length` for exactly this reason, and
    `bean:0008` recorded that Modus chose numeric ids deliberately — this is the cost of
    that choice arriving. Revisit it with the evidence now available rather than re-deciding
    from taste.
- Whichever is chosen, observed rejecting a planted collision before it is claimed
  (`doc:00-constitution#observed-failing`).
- The orchestration guidance says how an agent should allocate an id when it cannot see its
  siblings — currently nothing does, and "read `.beans/` and add one" is what failed.
