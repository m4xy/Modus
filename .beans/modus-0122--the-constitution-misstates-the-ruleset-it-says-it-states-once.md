---
# modus-0122
title: §7.1 states the main-protected ruleset "once, here" and states it wrong in three places
status: todo
type: fix
priority: high
order: AM
created_at: 2026-09-04T00:00:00Z
---

# §7.1 states the `main-protected` ruleset "once, here" and states it wrong in three places

`doc:00-constitution#workflow` §7.1 is the single owner of what the `main-protected` ruleset
carries — "What it carries is stated once, here" — and §7.2.4 and §7.4 both cite it rather than
re-reading the ruleset. That is the right shape (`doc:05-authoring-for-agents#one-fact-one-place`)
and it makes the accuracy of those two sentences load-bearing for three sections at once.

Read against the live ruleset, they are wrong in three ways.

1. **"and nothing else" is false.** §7.1 lists `pull_request`, `non_fast_forward`, `deletion`
   and `required_review_thread_resolution`. The `pull_request` rule also carries
   `require_extra_approval_for_unattributed_changes: true` and
   `allowed_merge_methods: ["squash","merge"]`, neither of which appears anywhere in
   `documentation/`.
2. **`allowed_merge_methods` permits a merge method §7.2.7 does not.** §7.2 step 7 is "Squash
   merge"; the ruleset accepts `merge` as well. Either the rule is stricter than its enforcement
   and says so, or the ruleset is narrowed to `["squash"]` and §7.2.7 becomes enforced rather
   than conventional — which is the choice `doc:00-constitution#mechanical-enforcement` §9 asks
   for, and it is a decision with a reason, not a typo fix.
3. **`required_approving_review_count: 0` is recorded as "asserted there, never observed", and
   it has now been observed.** §7.4's `Enforcement gap:` inherits that hedge. The figure is
   right; the epistemic label on it is stale, and `doc:50-memory-and-evidence#primary-sources`
   is the reason that matters — a hedge nobody removes is indistinguishable from one nobody
   checked.

There is a fourth thing §7.1 does not say, and it is the one another bean needs.
`bypass_actors: []` and `current_user_can_bypass: "never"` are what turn `pull_request` from a
default into a guarantee: with no bypass actor there is no direct push and no admin override, so
every tree `origin/main` has ever held arrived through a merged pull request. `bean:0120` rests
on exactly that fact and had to observe it for itself, having first argued the same conclusion
from §7.2.1's lifecycle rule — which does not support it, because nothing constrains the status
a bean is created with (`bean:0120` E4). §7.1 is where that fact belongs.

## Why this is not a one-line fix

`documentation/00-constitution.md` is at 500 lines, exactly the `max_lines: 500` ceiling
`documentation/README.md` states, so every line added costs a line evicted. `bean:0120`'s
criterion 4 needs an eviction in the same file for a different reason. Both are decisions for
review about what leaves the constitution, and taking them independently risks two changes each
evicting the other's replacement. Whoever takes this should look at both.

The two beans are not `blocked_by` one another. Either can be done first, and coupling them
would make each unselectable until the other lands.

## Success criteria

1. §7.1 states what the ruleset carries accurately, including `bypass_actors` and
   `current_user_can_bypass`, or states plainly that it lists only the rules bearing on §7 and
   names where the full read lives. The version that ships is checked against a live
   `gh api repos/m4xy/Modus/rulesets/21765196` in the same change, and the read is recorded.
2. The disagreement between `allowed_merge_methods` and §7.2.7 is resolved in one direction, the
   direction is recorded with its reason, and if the ruleset is not narrowed then §7.2.7 carries
   an `Enforcement gap:` naming this bean (`doc:00-constitution#observed-failing`).
3. `required_approving_review_count: 0` is no longer described as unobserved anywhere, §7.4
   included.
4. The eviction §7.1's new lines cost is stated, and it is checked against `bean:0120`'s
   criterion 4 so the two changes do not evict each other's additions.
5. `./gradlew qualityCheck` green.

## Not in scope

- Changing the ruleset itself. Modifying branch protection is refused to the agent by the
  harness (`bean:0047` records the same refusal), so a criterion 2 that narrows
  `allowed_merge_methods` is a request to a human and must be written as one.
- `bean:0047`'s required-status-checks rule, and `bean:0053`'s independent-review enforcement.
  Both are about rules the ruleset does **not** carry; this bean is about the ones it does.

## Evidence

### E1 — the live ruleset, read as §7.1 says to read it

```
cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 --jq \
            '{enforcement, rules: [.rules[].type], bypass_actors, current_user_can_bypass,
              pull_request: (.rules[] | select(.type=="pull_request") | .parameters)}'
expect:   the four items §7.1 names, and nothing else
observed: {"bypass_actors":[],"current_user_can_bypass":"never","enforcement":"active","pull_request":{"allowed_merge_methods":["squash","merge"],"dismiss_stale_reviews_on_push":false,"require_code_owner_review":false,"require_extra_approval_for_unattributed_changes":true,"require_last_push_approval":false,"required_approving_review_count":0,"required_review_thread_resolution":true,"required_reviewers":[]},"rules":["deletion","non_fast_forward","pull_request"]}
exit:     0
tree:     a live fetch, 2026-09-04, from the branch of PR #74. A ruleset is not in the
          repository and no commit stamps it; this record is falsified by anyone editing it,
          which is itself an argument for criterion 1 naming the command rather than the answer.
```

The `expect:` line is the one that failed. `required_review_thread_resolution` is not a rule at
all — it is a parameter of `pull_request`, beside six others — and two of those six are absent
from `documentation/` entirely.

### E2 — the three sentences this contradicts, with their locators

```
cmd:      awk 'NR >= 229 && NR <= 234 { printf "%d:%s\n", NR, $0 }' documentation/00-constitution.md
          grep -n 'Squash merge' documentation/00-constitution.md
          awk 'END { print NR }' documentation/00-constitution.md
          grep -n 'max_lines' documentation/README.md
observed: 229:**Enforced by:** repository ruleset `main-protected` (id `21765196`, `enforcement: active`),
          230:read with `gh api repos/m4xy/Modus/rulesets/21765196`. What it carries is stated once, here:
          231:`pull_request`, `non_fast_forward`, `deletion` and `required_review_thread_resolution`, so an
          232:unresolved review thread blocks merge — and nothing else. It has no `required_status_checks`
          233:rule (observed, `bean:0047`) and `required_approving_review_count: 0` (asserted there, never
          234:observed); §7.2.4 and §7.4 cite those absences rather than restating the list. Note the classic
          308:7. **Merge.** Squash merge; the squashed message is the PR title plus body.
          500
          112:- Line budget for `documentation/*.md`: `max_lines: 500`, `min_lines: none` (`adr:0003`).
exit:     0
tree:     `5ce4b80`, the head of PR #74, which does not touch `documentation/`. The `awk` numbers
          the lines it prints, so the locator and the text come out of one command
          (`doc:50-memory-and-evidence#capturing`).
```

The `grep` here is `ugrep 7.8.4`, the harness shim on the interactive `PATH`, not the
BSD `grep 2.6.0-FreeBSD` at `/usr/bin/grep` that CI has; both are plain fixed-string searches
and agree, and the awk lines are what the claim rests on.
