---
# modus-0109
title: bean:0051 needs an amendment recording the sibling-branch id collision
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# bean:0051 needs an amendment recording the sibling-branch id collision

`bean:0051` owns parallel bean-id allocation and prescribes allocating against `origin/main`.
It is `completed`, so the instance below cannot be added to it by editing. It needs an
`## Amendments` entry, and this bean is that work and nothing else.

## The instance

Two agents, two sibling branches, inside one hour, both allocated `0105`:
`.beans/modus-0105--a-kdoc-asserts-two-domain-rules-that-do-not-exist.md` on
`docs/spend-record-behind-its-recorder` and
`.beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md` on
`fix/per-request-usage-vocabulary`. **Both branches passed `docs-lint` the whole time.** The
collision was resolved by renumbering the first to `0108`, verified free across all refs and
all history rather than against `origin/main` alone.

## The mechanism

`bean:0068` states it and this bean does not restate it: check 6 resolves a bean against the
working tree and cannot see a sibling's; check 13 resolves id uniqueness against `origin/main`
and cannot see one either. Opposite blind spots, with a bean id passing through the middle.

## Why `bean:0051`'s rule is insufficient rather than wrong

Allocating against `origin/main` is the right rule. It detects rather than prevents — which
`bean:0051` states and accepts as a residual — and the detection is scheduled against
`origin/main`, **which is the one vantage point from which a concurrent sibling's allocation is
invisible.** So the residual is larger than "two open branches still collide until one merges":
the collision is not merely undetected until a merge, it is undetectable by the mechanism the
bean prescribes, because that mechanism looks exactly where the evidence is not.

## The residual, priced

Concurrent id allocation is safe today only while some party holds every branch in view at
once. That is a property of how the work is orchestrated, not of the repository, and it fails
silently the moment nobody is doing it. Both branches' reviewers verified their ids "across all
refs" and both were right when they looked; the id became non-unique when a sibling pushed.

## Appending to a completed bean is permitted, and the shape is enforced

Verified against `tools/docs-lint.sh` rather than inferred. Check 11's failure messages are
its whole contract: a completed bean may not be deleted; its base content must survive
verbatim as the head of the file; anything appended must open with `## Amendments`; and each
entry must carry the dated heading and one each of `**Claimed:**`, `**Found:**` and
`**Evidence:**`.

```
cmd:      grep -n 'fail 11' tools/docs-lint.sh
observed: 336:      fail 11 "$f: a completed bean was deleted; it is the durable evidence record (adr:0005#finalisation)"
          343:      fail 11 "$f: completed bean edited in place; it may only gain '## Amendments' entries (adr:0005#amendments)"
          351:        fail 11 "$f: appended '$first_new'; a completed bean may only gain a '## Amendments' section (adr:0005#amendments)"
          365:          *) fail 11 "$f: amendment heading '$h' is not '### YYYY-MM-DD · bean:NNNN'" ;;
          372:          fail 11 "$f: $n_amend amendment(s) but $n_k '**$k:**' line(s) (adr:0005#amendments)"
exit:     0
```

So the amendment this bean asks for is a sanctioned edit, not an exception, and its form is
decided by the check rather than by the author.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `bean:0051` carries an `## Amendments` entry recording the two-branch `0105` collision, dated, attributed to this bean, with `**Claimed:**`, `**Found:**` and `**Evidence:**` | citation |
| 2 | The entry cites `bean:0068` for the two-blind-spots mechanism rather than restating it (`doc:05-authoring-for-agents#one-fact-one-place`) | citation |
| 3 | `docs-lint` check 11 passes on the amended `bean:0051`, observed rather than assumed — the check is the arbiter of the entry's shape | test-run |
| 4 | Nothing in `bean:0051` above the `## Amendments` heading changes; check 11 rejects an in-place edit and that rejection is the guard being relied on | test-run |
