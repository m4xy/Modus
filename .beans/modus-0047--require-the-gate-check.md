---
# modus-0047
title: Make a green CI run actually required to merge
status: todo
type: fix
priority: high
order: AK
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0045]
# Blocked on a human: modifying branch protection is refused to the agent by the harness.
---

# Make a green CI run actually required to merge

`doc:00-constitution` §7.1 presents the `main-protected` ruleset as what enforces the
workflow. It does not enforce the build. Verified:

```
cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 -q '.rules[].type'
observed: deletion
          non_fast_forward
          pull_request

cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 \
            -q '.rules[] | select(.type=="required_status_checks")'
observed: (no output, exit 0 — the rule does not exist)
```

Both commands read `.rules[].type` and the `required_status_checks` rule's absence. Neither
read the **parameters** of the `pull_request` rule, so the `required_approving_review_count`
figure in "Success criteria" below is prose, not an observation — nothing in this tree has
watched that number. `doc:00-constitution` §7.1 and `bean:0053` cite it and say so.

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

## The observation `bean:0045` was held back for

`gate` reported green on a real pull request (#22, run `33256522531`): `build` 50s,
`frontend` 1m32s, `gate` 4s. The check name exists, always runs, and passed — which was the
precondition for requiring it.

## The change, ready to apply

Modifying branch protection is refused to the agent by the harness, correctly: it is an
outward-facing governance change that can lock the repository. One command, run by a human:

```bash
GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 > /tmp/ruleset.json
python3 - <<'EOF'
import json
r = json.load(open('/tmp/ruleset.json'))
r['rules'].append({
    "type": "required_status_checks",
    "parameters": {
        "strict_required_status_checks_policy": False,
        "do_not_enforce_on_create": False,
        "required_status_checks": [{"context": "gate"}],
    },
})
json.dump({k: r[k] for k in ("name", "target", "enforcement", "conditions", "rules")},
          open('/tmp/ruleset-new.json', 'w'))
EOF
GITHUB_TOKEN= gh api -X PUT repos/m4xy/Modus/rulesets/21765196 --input /tmp/ruleset-new.json
```

`context: "gate"` and nothing else. Naming `build` or `frontend` would block every change
that legitimately skips one, which is the whole reason the `gate` job exists.

Then the observed-failing half, which is the criterion and not a formality: open a pull
request whose CI is red, confirm the merge is refused, fix it, confirm the merge is allowed.
Until that is recorded, `doc:00-constitution` §7.2.4's `Enforcement gap:` stays.

## The API that answers about itself

Rulesets and classic branch protection are two APIs over one question, and the classic one
answers about the classic resource. On this repository, whose `main-protected` ruleset is
active, it reports the branch as unprotected:

```
cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/branches/main/protection
observed: {"message":"Branch not protected","documentation_url":"https://docs.github.com/rest/branches/branch-protection#get-branch-protection","status":"404"}gh: Branch not protected (HTTP 404)
```

Every command in this bean is a rulesets command and none is affected. The trap is one call
away from anyone auditing the state, and an audit written against the classic endpoint would
report an unprotected `main` and find nothing to do (`bean:0115` re-verified the ruleset at
`05939b8`).
