---
# modus-0113
title: A close that rewrites its own criteria is indistinguishable from a close that met them
status: todo
type: fix
priority: normal
created_at: 2026-09-03T00:00:00Z
---

# A close that rewrites its own criteria is indistinguishable from a close that met them

`docs-lint` check 14 certifies that a closing bean numbers its criteria, that each numbered
criterion is answered, and that no evidence cell is empty or hollow. It has no view on **when
the criteria were written**. So a bean is closable by rewriting its criteria to describe
whatever happened, and the mechanism reports the same green it reports for a bean that met the
criteria it was written with.

The reader of a green check 14 believes closure means *the thing we said we would do is done*.
The mechanism certifies *some numbered claims carry evidence*. That is this sprint's recurring
shape rather than a new one: a check that is real, observed failing, and answering a different
question from the one its reader believes it answers (`doc:00-constitution#observed-failing`).

## The instance, which was authorised and is still the right decision

`bean:0105` was raised to ask for an amendment, was overtaken by `9c9940d` before it was
worked, and closed as **superseded**. Its criteria were rewritten to what the close actually
meets, and it says so in its own text:

```
cmd:      grep -n 'The criteria are those of the close' .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
observed: 249:**The criteria are those of the close, not of the amendment this bean was raised to ask for.**
```

This bean does not propose undoing that. Criteria written for *I will do this* cannot be met by
anyone once someone else's pull request has achieved the bean's purpose, so a superseded close
has no other honest shape. What is unsafe is the **precedent**, because nothing distinguishes
that rewrite from one made to reach green.

## What check 14 sees, and what it cannot

Check 14 examines the file as it stands in the working tree, and nothing else:

```
cmd:      grep -n 'awk -v KINDS' tools/docs-lint.sh
observed: 661:    awk -v KINDS="$KINDS" \
```

Its candidate set is a bean `completed` in the change and not `completed` on the merge base:

```
cmd:      grep -n '\[ "$was" = "completed" \] && continue' tools/docs-lint.sh
observed: 653:    [ "$was" = "completed" ] && continue
```

Check 11 — the immutability guard — begins exactly where check 14 stops, on beans the base
already closed:

```
cmd:      grep -n '\[ "$was" = "completed" \] || continue' tools/docs-lint.sh
observed: 333:    [ "$was" = "completed" ] || continue
```

So there is one commit in a bean's life in which its criteria may be rewritten with no
mechanism looking at the rewrite. Both checks read that commit. Neither reads what the criteria
said before it.

## Why the obvious rule does not hold on the instance that produced it

The candidate rule this bean is raised with is that a criteria rewrite must land in the same
commit that sets `status: completed` — mechanically detectable, where intent is not. It does
not survive contact with `bean:0105`, which arrived on `origin/main` already `completed`, in
one squash:

```
cmd:      git diff --name-status 6fbf0e0 05939b8 -- .beans | grep 0105
observed: A	.beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
```

```
cmd:      git show 05939b8:.beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md | sed -n 's/^status: *//p' | head -1
observed: completed
```

and did not exist on the parent commit at all:

```
cmd:      git cat-file -e 6fbf0e0:.beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md; echo "exit $?"
observed: fatal: path '.beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md' exists on disk, but not in '6fbf0e0'
          exit 128
```

There is therefore no earlier version of those criteria in any tree for a check to diff
against. The branch history that held the rewrite is not on `main` and never will be — this
repository merges by squash only:

```
cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus --jq '{merge: .allow_merge_commit, rebase: .allow_rebase_merge, squash: .allow_squash_merge}'
observed: {"merge":false,"rebase":false,"squash":true}
```

`bean:0103` states the general form: committed history cannot answer a question about a state
that existed only before the merge. Any mechanism here runs **on the branch, against the merge
base**, as checks 11, 13c and 14 already do; and it must decide what to do with a bean that is
born closed, where the rewrite is unobservable by construction rather than by oversight.

## The candidate rule, recorded as a candidate

Written down so it is attacked rather than adopted by default. It is the orchestrator's
formulation, not a specification:

1. Criteria MAY be rewritten only when a bean closes as `superseded`.
2. The rewrite MUST name the artefact that superseded it.
3. The criteria diff MUST land in the same commit that sets `status: completed`.

Rule 3 is the mechanical half, and it is the half the instance breaks. Rules 1 and 2 are
decidable from the closing file alone — a `superseded` close is a claim the bean makes about
itself, and the artefact it names resolves or does not — which makes them cheaper than rule 3
and independent of it. A design that adopts 1 and 2 and refuses 3 is a legitimate outcome of
this bean, provided the refusal says what it leaves uncaught.

## Scope, and what this bean does not claim

Owned: whatever mechanism or rule is adopted, and the document that states it. Not owned:
`doc:00-constitution#bean-lifecycle`, check 14's other conditions (`bean:0087`, `bean:0061`),
and `bean:0105`, which is `completed` and is neither to be repaired nor retitled
(`adr:0005-evidence-lives-in-the-work-item#finalisation`) — it is the worked example and is
more useful intact.

No count of closed beans whose criteria were rewritten. Establishing that is part of the work;
it is a claim about branches that no longer exist; and a figure asserted here would be the
shape `doc:50-memory-and-evidence` §2.2 rejects.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and
a cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | A closing bean whose criteria were rewritten in the closing change is distinguishable from one that met the criteria it carried — by a mechanism, or by a recorded refusal naming what makes it undecidable and what that leaves uncaught | |
| 2 | Whatever is adopted is observed failing against a planted rewrite **and** observed silent on a close that rewrote nothing, with the count of firings asserted (`doc:00-constitution#observed-failing`, `doc:50-memory-and-evidence` §2.2) | |
| 3 | The born-closed case — a bean added already `completed`, with no earlier criteria in any tree — is decided explicitly rather than left to pass by construction | |
| 4 | The resulting rule states which closes may rewrite criteria and what the rewrite must name, once, in a document chosen and justified here — `doc:00-constitution#bean-lifecycle` owns the lifecycle and `doc:05-authoring-for-agents#checks` owns what a check asserts, and they are not the same home | |
| 5 | `bean:0105` is neither edited nor retitled, and is cited as the instance rather than restated (`doc:05-authoring-for-agents#one-fact-one-place`) | |
| 6 | `./gradlew qualityCheck` green | |
