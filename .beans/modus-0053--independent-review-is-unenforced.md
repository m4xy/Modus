---
# modus-0053
title: Nothing enforces that a reviewer is independent of the author
status: todo
type: fix
priority: high
order: AN
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0047]
# Blocked on modus-0047: it decides whether required_approving_review_count stays 0.
# Enabling any review requirement is the same governance change, refused to the agent.
---

# Nothing enforces that a reviewer is independent of the author

`doc:00-constitution#independent-review` now states the rule: an implementing agent MUST NOT
review its own change, the reviewer is a separately spawned agent briefed from the pull
request, and merge authority sits with the orchestrator once an independent review exists.
`doc:80-agent-operating-procedure#orchestrating` carries the mechanics.

Nothing mechanical enforces any of it. That is written into §7.4 as an `Enforcement gap:`
rather than dressed up as a gate, per `doc:00-constitution#observed-failing`. This bean is
what the gap names.

## What is actually true today

```
cmd:      gh api repos/m4xy/Modus/rulesets/21765196 -q '.rules[].type'
observed: deletion non_fast_forward pull_request
          (recorded by bean:0047; not re-run here — GITHUB_TOKEN in this environment is
           invalid and `gh api` returns 401 Bad credentials, so this is a citation, not an
           observation of mine)
```

Three consequences, none of which a check can currently see:

1. `required_approving_review_count` is 0 — so a pull request merges with **no** review, and
   the rule that a change is reviewed at all has nothing behind it. This one is *asserted*,
   not observed: `bean:0047` states it in prose, and the commands recorded there read
   `.rules[].type` only, never the `pull_request` rule's parameters. Observing it is the
   first thing this bean does, and it may find the number is not 0.
2. Even at 1, GitHub records an approval by a **GitHub account**. Every agent in this
   repository acts as the same account. An approval by the author's own session and an
   approval by an independently spawned reviewer are byte-identical to the API. The
   distinguishing fact — that two agents had separate contexts — exists only in a
   transcript, which is not in the tree and not in the GitHub event.
3. Nothing checks that a review cited anything. §7.4 says an approval citing nothing
   observed is not an approval; a `LGTM` review body satisfies GitHub identically.

## Why it cannot simply be turned on

`required_approving_review_count: 1` with a single-account setup blocks every pull request
permanently: GitHub refuses to count an approval from the pull request's own author, and
there is no second account. Turning it on is not a fix, it is a lock — the same failure
mode `bean:0047` is held back for, and the reason this is blocked on it rather than merged
into it. `bean:0047` decides the count; this bean is about independence, which the count
does not give even when it is greater than zero.

## Success criteria

- A decision is recorded, either way, on whether an independent reviewer gets an identity
  distinct from the implementer's — a second GitHub App or account, or an explicit
  statement that it will not, with what is enforced instead.
- If independence is to be mechanical: a check that a merged pull request carries a review
  event whose author is not the head commit's author, and whose body cites at least one
  `file:line`, command output, or rule. It runs in CI on the pull request, where the review
  exists; a check reading only the tree cannot see reviews at all.
- Observed failing before it is claimed (`doc:00-constitution#observed-failing`): open a
  pull request, self-approve it or approve it with an empty body, and watch the merge be
  refused. Both plants, not one — the two conditions fail independently.
- If it stays unenforced, §7.4's `Enforcement gap:` is rewritten to say so permanently
  rather than pointing at this bean, and this bean closes as `wontfix` with the reason.

## What is out of scope

The `required_status_checks` rule and the `gate` job (`bean:0047`). The orchestrator's own
delegation rules, which are equally unenforceable from repository contents and are recorded
as such in `bean:0052` — a transcript-shaped fact is not a tree-shaped one, and this bean
does not pretend otherwise for the half of the rule that is transcript-shaped either.
