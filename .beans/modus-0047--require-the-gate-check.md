---
# modus-0047
title: Make a green CI run actually required to merge
status: todo
type: fix
priority: high
order: AK
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0045]
---

# Make a green CI run actually required to merge

`doc:00-constitution` §7.1 presents the `main-protected` ruleset as what enforces the
workflow. It does not enforce the build. Verified:

```
cmd:      gh api repos/m4xy/Modus/rulesets/21765196 -q '.rules[].type'
observed: deletion non_fast_forward pull_request

cmd:      gh api repos/m4xy/Modus/rulesets/21765196 \
            -q '.rules[] | select(.type=="required_status_checks")'
observed: (nothing — the rule does not exist)
```

So `rule:ci/build` has never been a merge gate. Every pull request in this repository could
have been merged red, and one was red for two pushes before it was noticed by a human
reading the checks tab rather than by anything stopping it. `doc:00-constitution#observed-failing`
is explicit that a mechanism nobody has watched reject something is a claim; this one cannot
reject anything at all.

## Why it is a separate bean from `bean:0045`

Because enabling it can lock the repository. A required check that never reports — a renamed
job, a workflow that fails to parse, a filter that skips every half — blocks every merge
with no way through except an admin edit. The safe order is: land the `gate` job, watch it
report green on a real pull request, **then** require it. Doing both in one change means
turning on a gate whose name has never been observed in a check run.

## Success criteria

- `main-protected` gains a `required_status_checks` rule naming the `gate` job, and only
  that job. Naming `build` or `frontend` would block every change that legitimately skips
  one, which is the whole reason `gate` exists.
- Observed failing before it is claimed: open a pull request whose CI is red, confirm the
  merge is refused, then fix it and confirm the merge is allowed
  (`doc:00-constitution#observed-failing`).
- `doc:00-constitution` §7.1's description of the ruleset is corrected to list what it
  actually carries, and its `Enforcement gap:` in §7.2.4 is removed.
- Decide whether `required_approving_review_count` stays at 0. It is 0 today, so
  `doc:80-agent-operating-procedure` step 8's "do not merge your own PR" is a convention
  with nothing behind it. That is a governance question rather than a build one — record the
  answer either way rather than leaving the rule to look enforced.
