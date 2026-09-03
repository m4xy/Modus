---
# modus-0107
title: bean:0103 states a count in the same paragraph that argues for a quantifier
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# `bean:0103` states a count in the same paragraph that argues for a quantifier

One sentence, in a merged bean, wrong in the way the sentence directly after it warns against —
and the two are one paragraph, not neighbouring ones. `bean:0103`'s *The instance, in full* reads:

> Exactly one commit has ever touched the line carrying the Opus 5 rate, and it introduced it at
> $5/$25 — so the rate has never been changed in committed history at all. That is phrased as a
> quantifier over the set rather than as a count of it, deliberately: **a claim quantified over a
> set that grows is stale on arrival, while a claim about the set itself is not.**

The count is now wrong. The conclusion is not.

**And the claim is asserted to satisfy the rule it violates, in the same breath.** The paragraph
does not merely state the quantifier rule near a count — it labels the count as compliant with
it: *"That is phrased as a quantifier over the set rather than as a count of it, deliberately."*
It is not, and the word doing the damage is `deliberately`, because it tells the next reader the
question has already been considered.

That is the failure `bean:0069` names as **a claimed-re-derived table and a re-derived one are
indistinguishable from outside**, occurring inside the document that warns about it, applied by
its author to their own sentence. A self-certification is worth nothing precisely where it is
most persuasive, and a reviewer who reads that clause has been given a reason not to check the
sentence it describes. It survived two rounds for that reason as much as for the `-S` result
below.

## Why the block's own command does not reveal it

The second reason it survived, and the mechanical one.

```
cmd:      git log --all -S "opus-5" --oneline -- backoffice/src/agent/transport.ts
observed: 10af4f7 feat(backoffice): scaffold the backoffice with a tokenised design system
```

One commit, exactly as the sentence says — and it will keep saying one. **`git log -S` searches
for a change in the *number of occurrences* of the string**, not for a change to the line. Every
later edit to that rate table rewrote the entry while leaving the number of occurrences of
`opus-5` identical on both sides, so `-S` cannot see any of them. The cited evidence agrees with
the sentence forever, and agrees for a reason that has nothing to do with whether the sentence is
true.

`-G` searches the diff text and does see them:

```
cmd:      git log --all -G "claude-opus-5" --format="%s" -- backoffice/src/agent/transport.ts | sort -u
observed: feat(backoffice): scaffold the backoffice with a tokenised design system
          fix(agent): publish per-request, cache-aware usage instead of a cumulative total
```

More than the one. **No sha and no count is recorded here on purpose**: `bean:0103` establishes
that a commit id is a figure over a moving set exactly as a count is, and under `--amend` it moves
faster — that bean lost a `d2d431a` about a minute after writing it. Subjects, deduplicated, carry
the claim the sentence needs and nothing that rots.

## The conclusion survives, and is stronger stated as a set

What the block is arguing is that the defect was never committed. That is still true, and the
honest form of it makes no reference to how many commits exist:

```
cmd:      git log --all -G "claude-opus-5" -p -- backoffice/src/agent/transport.ts | grep "^[-+].*'claude-opus-5'" | sort -u
observed: +  'claude-opus-5': { input: 5_000_000, output: 25_000_000 },
          +  'claude-opus-5': { inputPerMTok: 5, outputPerMTok: 25 },
          -  'claude-opus-5': { inputPerMTok: 5, outputPerMTok: 25 },
```

Every version of that line ever added or removed carries $5/$25. The unit changed — per-MTok to
integer micro-dollars — and the rate did not. So: **in committed history the Opus 5 rate has only
ever been $5/$25.** A claim about the set, which cannot be falsified by the set growing, and which
happens to be the claim `bean:0103` was making all along.

All three commands are pathspec-scoped to `backoffice/`, so recording them in `.beans/` does not
put their search strings into their own corpus — mitigation 1 from `bean:0103`'s own table.

## The fix, which is one sentence

Replace the quoted sentence with the set form above. If a command is cited beside it, cite `-G`
and not `-S`, and say which question each answers.

**Do not restate the finding.** `bean:0103` owns the pre-merge mechanism, the `-S` last-wins
mechanism and the recording hazard; this bean owns one sentence inside it and nothing else. The
`-S`-is-an-occurrence-count-search property recorded above is new — `bean:0103` records that `-S`
is *last-wins* across multiple arguments, which is a different property of the same flag — and it
belongs wherever that bean keeps the first.

## Why this needs an id rather than a line in someone's findings list

It was first written down as a bullet in `bean:0069`'s *Findings for others, not acted on here*,
which is the consistent place for a finding about a file the pull request does not own. That is
correct treatment and it is not sufficient: **a bullet in a merged bean has no owner and nothing
that will notice it.** `bean:0069` closes, the sprint ends, and the sentence stays.

The sharper reason is that `bean:0103` already contains the remedy. It tables three mitigations
for the recording hazard and separately records that *a mitigation written into the same document
as the defect it prevents is not thereby applied* — one technique, three chances, one hit. This
sentence is the fourth chance and the same miss: the quantifier rule is stated two lines below the
count it should have governed, by the author who wrote both, in the same edit. That is not an
erratum. It is the bean's own central finding happening to the bean, and it is worth an id so it
is fixed rather than noted.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and a
cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | `bean:0103`'s *The instance, in full* states the claim over the set — the Opus 5 rate has only ever been $5/$25 in committed history — with no count of commits, and the "phrased as a quantifier … deliberately" clause is either true of what follows it or gone | |
| 2 | Any command cited beside it is `-G`, and the block says what `-S` answers instead, so the next reader does not repeat the substitution | |
| 3 | Nothing else in `bean:0103` is restated or moved; the change is the one sentence and its evidence block (`doc:05-authoring-for-agents#one-fact-one-place`) | |
| 4 | The `-S` occurrence-count property is recorded wherever `bean:0103` keeps its `-S` last-wins finding, rather than in a second place | |

## Scope, stated as a limit

`bean:0103` is merged on `main`. Nothing here is a defect in its conclusions, and the finding is
not that the bean is unreliable — it is one of the more carefully evidenced documents in the tree,
which is the point. The claim it got wrong is the kind it exists to warn about, and it got it
wrong inside the warning.
