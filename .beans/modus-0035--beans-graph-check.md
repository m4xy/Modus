---
# modus-0035
title: docs-lint must validate the bean graph, not just prose references
status: completed
type: fix
priority: high
order: AE
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
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

## Restated criteria

One check — `docs-lint` check 12 — with five decidable failure conditions, each observed
rejecting a planted violation before it is claimed (`doc:00-constitution#observed-failing`).

| # | criterion, restated | evidence kind |
|---|---|---|
| 1 | a `blocked_by` or `parent` id that matches other than exactly one file under `.beans/` fails the build | planted violation, reverted |
| 2 | a `blocked_by` edge whose target is `type: epic` fails the build | planted violation, reverted |
| 3 | a cycle anywhere in the `blocked_by` graph fails the build, at length two and at length three | planted violation, reverted |
| 4 | two beans that reach `AGENTS.md` step 1's tiebreak together and share an `order` value fail the build — **and two beans that share one without competing do not** | planted violation plus a negative control |
| 5 | a backlog in which step 1 would return nothing fails the build | planted violation, reverted |
| 6 | the check is not vacuous: its parse counts appear on the `OK` line | `bash tools/docs-lint.sh` output |
| 7 | the gate is green | `./gradlew qualityCheck` |

**Selectable** is defined as exactly what `AGENTS.md` step 1 returns, and nothing wider:
`status: todo`, not `type: epic`, every `blocked_by` id resolving to a `completed` bean.
Criterion 4 is scoped to that set grouped by `priority`, because only beans that reach the
tiebreak together can be tied by it. A bean carrying no `order` is not a collision — absence
is a defined position in the upstream sort (after every bean that has one), so a rule
demanding an `order` from every competing bean would be a new requirement, not this one.
Four `priority: normal` beans carry no `order` today; making that fatal is a separate call.

### Deliberately not in scope

`bean:0025` — `docs-lint` resolving `rule:archunit` references. Same tool, different gap.
Requiring an `order` on every competing bean, per the paragraph above.

## Evidence

```
cmd:      bash tools/docs-lint.sh   (during the plant sequence below)
observed: docs-lint: OK — 18 documents, 96 anchors, 686 references, 47 beans,
          25 graph edges, 14 selectable.
exit:     0

cmd:      bash tools/docs-lint.sh   (final, this bean now in-progress)
observed: docs-lint: OK — 18 documents, 96 anchors, 688 references, 47 beans,
          25 graph edges, 13 selectable.
exit:     0
```

The counts differ by design: this bean moving `todo` → `in-progress` removes it from the
selectable set, and its own evidence adds references. That the numbers move with the
corpus is itself the point of printing them.

The three trailing counts are criterion 6. They exist because `docs-lint` check 11 shipped
**inert** and passed four plants before anyone noticed (`bean:0038`). A check that parsed
nothing now reports `0 beans, 0 graph edges, 0 selectable` on a line that is read every
run, rather than reporting success.

Criteria 1–5, each planted into `.beans/` and reverted with `git checkout -- .beans`:

```
planted:  parent: modus-9999 on modus-0033
observed: FAIL check 12 .beans/modus-0033--baseline-writer-erases-regression-provenance.md:
          parent 'modus-9999' resolves to 0 bean files, expected exactly 1

planted:  blocked_by: [modus-9999] on modus-0033
observed: FAIL check 12 .beans/modus-0033--baseline-writer-erases-regression-provenance.md:
          blocked_by 'modus-9999' resolves to 0 bean files, expected exactly 1

planted:  blocked_by: [modus-0011] on modus-0033 — modus-0011 is type: epic
observed: FAIL check 12 .beans/modus-0033--baseline-writer-erases-regression-provenance.md:
          blocked_by 'modus-0011' is a 'type: epic' bean; step 1 never selects an epic,
          so the edge never clears

planted:  modus-0033 -> modus-0034 -> modus-0033
observed: FAIL check 12 blocked_by graph has a cycle: modus-0033 -> modus-0034,
          modus-0034 -> modus-0033

planted:  modus-0033 -> modus-0034 -> modus-0046 -> modus-0033
observed: FAIL check 12 blocked_by graph has a cycle: modus-0033 -> modus-0034,
          modus-0034 -> modus-0046, modus-0046 -> modus-0033

planted:  order: AE -> order: AD on modus-0035, colliding with modus-0034
observed: FAIL check 12 priority 'high' order 'AD' is shared by selectable beans:
          modus-0034 modus-0035

control:  order: AD added to modus-0033 (priority: normal) — the same value as
          modus-0034 (priority: high), but they never compete
observed: docs-lint: OK — 18 documents, 96 anchors, 686 references, 47 beans,
          25 graph edges, 14 selectable.
exit:     0

planted:  blocked_by: [modus-0047] on every non-epic todo bean; modus-0047 is itself
          todo and blocked behind in-progress modus-0045
observed: FAIL check 12 no bean is selectable: every non-epic 'status: todo' bean has
          an unsatisfied blocked_by edge, so AGENTS.md step 1 returns nothing
```

Every plant ran `bash tools/docs-lint.sh` and exited 1. Each was reverted and the clean
run above re-observed before the next.

The cycle at length three matters separately from the one at length two: the detector is
an edge-removal fixed point, not a pairwise scan, and a two-node cycle would be caught by
either. Length three is what distinguishes them.

### The corpus is acyclic and no epic is depended on today

Both were true before this change, which is why the bug in `bean:0012`'s split was latent
rather than live. The check is a guard against reintroducing it, and the negative control
above is what shows the guard is not simply always-on.

### Criterion 7 — the gate

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLint
          docs-lint: OK — 18 documents, 96 anchors, 688 references, 47 beans,
          25 graph edges, 13 selectable.
          > Task :qualityCheck
          BUILD SUCCESSFUL in 26s
          167 actionable tasks: 55 executed, 112 from cache
```

## Note on the parser

Absent front-matter scalars are emitted as `-`, never as an empty field. A tab is IFS
whitespace, so bash `read` collapses runs of tabs and discards leading and trailing ones:
one empty middle field would silently shift every field after it. This cost nothing here
only because it was anticipated; it is the kind of thing that makes a bash check quietly
wrong rather than loudly broken.

`docs-lint.sh` claims bash 3.2 compatibility, and nothing in the gate checks it: `bash` on
this machine resolves to Homebrew 5.3.9 and CI is Linux, so `bash tools/docs-lint.sh` never
exercises the version the claim is about. Verified explicitly instead:

```
cmd:      /bin/bash --version
observed: GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)

cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 18 documents, 96 anchors, 688 references, 47 beans,
          25 graph edges, 13 selectable.

cmd:      /bin/bash tools/docs-lint.sh, with the type: epic edge planted
observed: FAIL check 12 …: blocked_by 'modus-0011' is a 'type: epic' bean; step 1 never
          selects an epic, so the edge never clears
```
