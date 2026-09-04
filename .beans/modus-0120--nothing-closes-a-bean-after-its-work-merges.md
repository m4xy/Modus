---
# modus-0120
title: The rule that holds a bean open through review has no counterpart that closes it after the merge, and the backlog it leaves is invisible in every run
status: todo
type: fix
priority: high
order: AL
created_at: 2026-09-04T00:00:00Z
# order: `AGENTS.md` step 1 sorts a bean with no `order` after every bean that has one, and
# check 12 does not flag the absence, because absence is a defined position rather than a
# collision (`doc:05-authoring-for-agents#checks`). Without this line a high-priority fix
# raised about an invisible backlog would itself sort behind every ordered high-priority
# bean and tie untiebreakably with eight unordered ones. `AL` places it between `bean:0047`
# (`AK`) and `bean:0053` (`AN`), the other two beans about an enforcement gap in §7.
---

# The rule that holds a bean open through review has no counterpart that closes it after the merge, and the backlog it leaves is invisible in every run

`doc:00-constitution#bean-lifecycle` holds a bean `in-progress` for the whole life of its own
pull request, and it is right to: a bean cannot close itself, and `docs-lint` check 11 would
freeze it against its author's own review fixes. An agent refused an instruction to close a bean
on exactly that ground during `bean:0049`'s review and was right to refuse.

The rule that holds the bean open has **no counterpart that closes it after the merge**. §7.2.1
ends by saying closing "is the first act of the session after a merge", and nothing establishes
that the session happens. The close is a separate change nobody is assigned, and it is the one
change in this workflow whose omission leaves no trace: a bean left `in-progress` for ever
produces `0 closing transitions, 0 criteria checked` on the `docs-lint` line, which is exactly
what a week with no closes due produces (`bean:0096`). The signal and its absence print the same
characters.

## It has happened three times in one sprint, and the backlog is measurable (E1)

Walking `main` back thirty commits and counting `status: in-progress` beans in each tree:

- Sprint 3 opened by discovering five at once — `0065`, `0068`, `0069`, `0102` and `0115` —
  which `f55de2a` closed. The longest of the five is `bean:0068`: **twenty-three consecutive
  rows**, from `97f13b0`, the merge that put it on `main`, to `9adb8af`, the last tree before
  `f55de2a` closed it. `bean:0102` runs **eighteen**, `157a57a` to `9adb8af`. Both counts are
  taken off E1's own capture, in the second fence below; the prose here previously said
  fourteen for `bean:0102`, which the table it sits above already contradicted, and named the
  shorter backlog as the headline.
- `bean:0116` shipped in `2b67b23` and stayed `in-progress` until `8c3fd82` closed it.
- `bean:0049` and `bean:0096` shipped in `1c19cf0` and `0e4324d` and were closed by the change
  that raises this bean.

The count returns to zero at exactly three commits in that window — `08936ee`, `f55de2a` and
`8c3fd82` — and every one of them is a `chore(beans): close …` change. Between them it is never
zero. That is the backlog, and nothing in the repository reports it.

## The state is derivable, and more cleanly than by a proxy (E1, E2, E4)

**Not by construction of §7.2.1.** That was this bean's first argument and it does not survive
being checked. §7.2.1 puts the `todo` → `in-progress` flip on the branch, but **nothing
constrains the status a bean is created with**, and five of the nine beans in E1's window were
created directly at `status: in-progress` and never flipped: `0068` at `97f13b0`, `0069` at
`05939b8`, `0102` at `157a57a`, `0115` at `9adb8af`, `0116` at `2b67b23` (E4). No check reads a
new bean's status — check 11 classifies by the merge base and skips a bean absent from it,
check 12 reads `status` only to decide selectability, check 14 fires on `completed`. For five of
those nine beans §7.2.1's flip is not what put `in-progress` on `main`, because it never
happened, so a mechanism resting on it would rest on an act the record shows did not occur.

**The guarantee comes from the ruleset instead, and it is mechanical.** `main-protected`
(id `21765196`) is `enforcement: active`, with rules `deletion`, `non_fast_forward` and
`pull_request`, `bypass_actors: []` and `current_user_can_bypass: "never"` (E4). No direct push
and no admin bypass: every tree `origin/main` has ever held arrived through a merged pull
request, whatever status a bean was written with. `doc:00-constitution#workflow` §7.1 states the
ruleset; what §7.1 does not yet carry is `bypass_actors`, which is the half that makes it a
guarantee rather than a default, and correcting §7.1 is `bean:0122`.

**One step remains, and it is an assumption stated rather than absorbed into the word
"construction".** From "this bean is `in-progress` on `origin/main`" the mechanism needs "and the
pull request that put it there carried the bean's work". The ruleset does not give that. It holds
whenever a bean's branch is the branch that does its work — the ordinary case `doc:00-constitution#workflow`
§7.2 describes — and it is exactly what the stacked case below breaks. So the datum this bean
rests on is: **a bean `in-progress` on `origin/main` got there through a merged pull request,
and, on the stated assumption, that pull request was its own.** E2 is the empirical half and now
carries its own weight rather than being the weaker half of an argument that did not need it.

Either reading is stronger than the proxies the gap invites:

| proxy | why it is worse |
|---|---|
| the bean's branch no longer exists | requires the network. `doc:05-authoring-for-agents#checks` opens by requiring each check to be decidable from repository contents alone, and a deleted branch is also indistinguishable from an abandoned one |
| the `in-progress` state predates N merges | needs an arbitrary constant, and the constant is the whole of the rule |

`docs-lint` already resolves `origin/main` — checks 11, 13c and 14 all read it — so the datum
costs no new machinery. Check 13's inert-by-construction behaviour with no `origin/main`, which
reports `-` rather than `0`, is the precedent for what this must do in a shallow checkout
(`bean:0051`).

## What is not settled, and is this bean's work

**Whether the check blocks.** A blocking check fires on every pull request until the outstanding
bean is closed, including pull requests with no connection to it: after `1c19cf0` merged, every
subsequent change would have gone red for `bean:0049`, `f319f76` included. That is stop-the-line,
and whether this repository wants it is a decision with a reason, not a default.

**Whether a non-blocking counter is worth anything.** A field on the `OK` line — beans
`in-progress` on `origin/main` and not closed by this change — makes the backlog legible at the
point of use for the cost of one integer. `bean:0096` rejected an `OK`-line edit on the ground
that a report nobody must act on is a report nobody reads, and on the ground that the line's text
is quoted verbatim in check-11-frozen transcripts. Both objections apply here and neither is
obviously decisive: the line has been extended before, and the corpus already holds several
spellings of it.

**The false-positive class, re-weighted: it is the intended path, not an edge case.** A bean
whose work is split across stacked pull requests is `in-progress` on `main` after the first
merges, correctly, and a check cannot tell it from a pending close. This bean first weighed that
as "a residual for the design to name, not a refutation", resting partly on the observation that
no instance appears in the thirty commits measured. Both halves of that weighting were wrong.

The owner's stated preference is to **split a bean into stacked children rather than ship one
large pull request**, stated in review on this pull request; this paragraph is that comment's
resolution, which `doc:00-constitution#independent-review` §7.4 requires to be a change, a rule
or a stated refusal. So the shape is what this repository is trying to do, not an accident. It
is a preference and not a rule until `bean:0037` lands, which is the point: an undocumented
intended path is one no check can be designed against. And absence over thirty commits is not
evidence of rarity when
the practice is undocumented: `bean:0037`, the stacked-pull-request procedure, is `status: todo`,
so no convention exists for an agent to follow and nothing in E1's table would mark an instance
as one if it were there. `doc:50-memory-and-evidence#corpus-figures` is explicit that a claim of
absence needs a second search by different words and a control; neither was run, and this bean
does not now claim the absence.

It compounds with the beans E4 names as born `in-progress`: those are the same act — an agent
that knows the work is already under way writes the status the work is in — and both leave a
bean `in-progress` on `main` for a reason that is not a pending close. Criterion 3 is therefore
not satisfied by naming the residual in prose. The mechanism must exclude the case by
construction or name it in its own output, and which of the two is a decision with a reason.

## Success criteria

1. A mechanism exists that distinguishes "no close was due" from "a close is overdue", and it is
   observed making that distinction on the real corpus rather than only on a fixture
   (`doc:00-constitution#observed-failing`).
2. If it blocks, the stop-the-line consequence is stated with the pull request it would have
   turned red, and the decision to accept it is recorded. If it does not block, the reason a
   non-blocking report is worth its cost here is recorded against `bean:0096`'s rejection of the
   same shape.
3. The stacked-pull-request false positive is either excluded by construction or named in the
   mechanism's own output as an accepted residual.
4. `doc:00-constitution#bean-lifecycle` gains the line that names the mechanism, or an
   `Enforcement gap:` naming this bean. `documentation/00-constitution.md` is at 500 lines,
   exactly check 8's ceiling, so that line costs an eviction — which is a decision for review and
   is why it is not taken in the change that raises this bean (E3).
5. `./gradlew qualityCheck` green.

## Not in scope

- **Amending §7.2.1's hold.** The rule is correct and both its reasons hold. This bean adds the
  missing half, it does not weaken the existing one.
- Check 11's classification by merge-base status, which is what makes a close legal at all.
- What check 14 accepts inside an evidence cell once it does run — `bean:0087`, unmerged.

## Evidence

### E1 — the backlog, walked over thirty commits of `main`

Read-only: one `git grep -l` per commit against that commit's tree, no working-tree access.

```
cmd:      for c in $(git log --format=%H -30 1c19cf0); do
            subj=$(git log -1 --format=%s "$c")
            ids=$(git grep -l '^status: in-progress' "$c" -- '.beans/*.md' 2>/dev/null \
                  | sed 's|.*/modus-\([0-9]*\)--.*|\1|' | tr '\n' ' ')
            n=$(printf '%s' "$ids" | wc -w)
            printf '%s  %2d  [%s]  %s\n' "${c:0:7}" "$n" "$ids" "$subj"
          done
observed: 1c19cf0   2  [0049 0096 ]  feat(tools): enforce the bash 3.2 claim the gate only asserted
          f319f76   1  [0096 ]  chore(beans): raise 0119, spend records carry no seq, kind or crc (#73)
          0e4324d   1  [0096 ]  docs(05,beans): state what a green check 14 establishes, and what it cannot (#70)
          8c3fd82   0  []  chore(beans): close 0116, raise the spend-ledger finding, name 0091's fifth shape (#72)
          2b67b23   1  [0116 ]  fix(beans,agents,skills): the plant hazard recurs through the capture procedure (#71)
          f55de2a   0  []  chore(beans): close the five work items whose changes have merged (#68)
          9adb8af   5  [0065 0068 0069 0102 0115 ]  docs(50,80,beans): encode sprint 2's findings and hand off to sprint 3 (#67)
          05939b8   4  [0065 0068 0069 0102 ]  fix(agent): publish per-request, cache-aware usage instead of a cumulative total (#45)
          6fbf0e0   3  [0065 0068 0102 ]  docs(60): name the token kinds the recorder writes, and price them (#54)
          9c9940d   3  [0065 0068 0102 ]  docs(00,35): encode what makes a gate believable, and correct one that is not (#53)
          905a5f9   3  [0065 0068 0102 ]  docs(50,05): encode the four shapes a claim takes when it is not verified (#52)
          161a7c3   3  [0065 0068 0102 ]  docs(80): encode sprint 1's agent-loop findings (#48)
          4ee94a4   3  [0065 0068 0102 ]  docs(beans): raise bean:0100 — why the false precedence clause survived (#61)
          cdfba7c   3  [0065 0068 0102 ]  docs(beans): raise bean:0104 — a scripted edit produces no reading of its own result (#66)
          ecd2168   3  [0065 0068 0102 ]  docs(beans): raise bean:0103 — a null git log result that does not mean what it appears to (#64)
          cf6063b   3  [0065 0068 0102 ]  feat(core-domain): the ambient-capability ports and their leaf gate (#55)
          bd9da18   2  [0068 0102 ]  docs(agents): read what you changed with three dots, not two (#65)
          63f367e   2  [0068 0102 ]  docs(05): point the citation gap line at the bean that closes it (#62)
          a99a9f6   2  [0068 0102 ]  chore(beans): close bean:0063, and unblock the family behind it (#63)
          52e6f49   3  [0063 0068 0102 ]  docs(20): state the check in §5.1's note, not a snapshot of the answer (#58)
          2c958e4   3  [0063 0068 0102 ]  chore(beans): raise the three beans blocking the walking skeleton (#44)
          8116b28   3  [0063 0068 0102 ]  docs(beans): raise bean:0099 — the fence classifier and the citation matcher compose (#57)
          74cb201   3  [0063 0068 0102 ]  docs(beans): raise bean:0093 — pasted output in top-level prose answers its own criterion (#56)
          157a57a   3  [0063 0068 0102 ]  docs(agents): commit before planting, or the plant erases the change (#60)
          9c262e1   2  [0063 0068 ]  docs(beans): raise bean:0096 — check 14 contributes nothing to an implementation PR (#59)
          97b3dfe   2  [0063 0068 ]  docs(beans): raise bean:0087 — check 14 verifies evidence shape, not content (#50)
          cda5613   2  [0063 0068 ]  docs(beans): raise bean:0086 — check 6 resolves references through a naive fence toggle (#49)
          e756042   2  [0063 0068 ]  fix(docs-lint): read a quoted fence marker as content, not as a fence (#46)
          97f13b0   1  [0068 ]  docs(20,10): correct §5.1's packages and settle the ambient port names (#51)
          08936ee   0  []  chore(beans): close the four work items whose changes have merged (#47)
exit:     0
```

The two run lengths the prose above quotes, taken off that capture rather than counted by eye.
The rows are one per commit and in `git log` order, so a row count equals a run length exactly
when the rows are contiguous, which is what the last column checks.

```
cmd:      bash [...]/e1.sh > [...]/e1.out          # the capture above, regenerated
          bash [...]/runs.sh                        # /usr/bin/grep -c and -n over it
expect:   `bean:0068` runs longer than `bean:0102`, and both runs are unbroken
observed: 0068  rows=23  first=7(9adb8af)  last=29(97f13b0)  last-first+1=23
          0102  rows=18  first=7(9adb8af)  last=24(157a57a)  last-first+1=18
          total rows in E1: 30
exit:     0
tree:     the capture is regenerated from `1c19cf0`'s history by the same command as the fence
          above and is byte-identical to it. `/usr/bin/grep` is BSD grep 2.6.0-FreeBSD, named
          because the interactive shell's `grep` in this harness is `ugrep 7.8.4`; the two
          agree here, and the run that produced these figures is the BSD one, which is also
          what CI has.
```

`bean:0068` is the worse case and is now the named instance. Neither figure is a count of
"merges" in the abstract: each is a count of trees in this thirty-commit window on which that
bean was `in-progress`. Both runs are contained by the window rather than clipped by it —
row 30, `08936ee`, carries neither id, and row 29 and row 24 are the merges that created
`bean:0068` and `bean:0102` respectively (E4).

Two readings the numbers do not carry on their own. The window ends at a close, `08936ee`, so the
run of zeroes before it is outside the window and not absent from history. And the count rises
at ordinary changes — `97f13b0`, `e756042`, `cf6063b` — because each of those is the merge that
put some bean's `in-progress` on `main`, which is the mechanism this bean rests on, visible in
the same table.

### E2 — the same claim, from the other side: no bean is `in-progress` on `main` for any other reason

Every id in E1's rows was later closed by a `chore(beans): close …` change; none returned to
`todo`, none was abandoned, and none stayed open because its work was still in flight. The three
zero rows are the three closing changes and nothing else zeroes the count. This is an
exhaustive reading of the window in E1, not a separate run, and it is the weaker half of the
argument: thirty commits is the whole recorded history of this convention, so the absence of a
counterexample is an absence over a small corpus (`doc:50-memory-and-evidence#corpus-figures`).
The construction argument this sentence used to defer to has been withdrawn (E4), so this is no
longer the weaker half of something stronger. It stands beside the ruleset, which settles
mechanically that a tree on `origin/main` arrived through a merged pull request and settles
nothing about whose work that pull request carried.

### E3 — the constitution is at its ceiling, so criterion 4's line costs an eviction

Planted one blank line, ran the gate, restored from a copy taken before the plant. No `git`
operation was involved, so no uncommitted work in the tree could be discarded (`bean:0102`).

```
cmd:      /usr/bin/grep -n 'max_lines' documentation/README.md
          awk 'END { print NR }' documentation/00-constitution.md
observed: 112:- Line budget for `documentation/*.md`: `max_lines: 500`, `min_lines: none` (`adr:0003`).
          500

cmd:      printf '\n' >> documentation/00-constitution.md
          awk 'END { print NR }' documentation/00-constitution.md
          bash tools/docs-lint.sh
observed: 501
          FAIL check 8  documentation/00-constitution.md: 501 lines, over the 500 ceiling
          docs-lint: 1 failure(s).
exit:     1

cmd:      cp <copy> documentation/00-constitution.md
          diff <copy> documentation/00-constitution.md && echo identical
          awk 'END { print NR }' documentation/00-constitution.md
observed: identical
          500
exit:     0
```

`<copy>` is the scratchpad path the pre-plant copy was written to, elided as `[...]` is elsewhere.
`doc:80-agent-operating-procedure` is at 498 of the same ceiling, so it has two lines of room and
is not where this rule belongs anyway: §7.2.1 already states the rule, and restating it in
`doc:80` would be the duplication `doc:05-authoring-for-agents#one-fact-one-place` forbids. What
is missing is the gate, which is what `doc:00-constitution#mechanical-enforcement` §9.1 calls an
unenforced rule.

### E4 — what actually puts `in-progress` on `main`: not §7.2.1's flip, and not nothing

Two halves. The first refutes the construction argument this bean opened with; the second
supplies what replaces it. Read-only, no working-tree access in either.

```
cmd:      bash [...]/e4.sh
          # half one, per id: p=$(git ls-tree -r --name-only 1c19cf0 -- .beans | grep ...)
          #                   add=$(git log --diff-filter=A --format=%h 1c19cf0 -- "$p" | tail -1)
          #                   git show "$add:$p" | sed -n 's/^status:[[:space:]]*//p' | head -1
          # half two:         GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 --jq
          #                     '{enforcement, rules: [.rules[].type], bypass_actors,
          #                       current_user_can_bypass,
          #                       pull_request: (.rules[] | select(.type=="pull_request") | .parameters)}'
expect:   every bean in E1 was created `status: todo` and flipped on its branch, as §7.2.1
          describes; the ruleset carries a `pull_request` rule
observed: --- status a bean carried in the commit that ADDED it, for every id in E1
          0049  added b217c0e  status-at-add: todo
          0063  added fe44aa1  status-at-add: todo
          0065  added 2c958e4  status-at-add: todo
          0068  added 97f13b0  status-at-add: in-progress
          0069  added 05939b8  status-at-add: in-progress
          0096  added 9c262e1  status-at-add: todo
          0102  added 157a57a  status-at-add: in-progress
          0115  added 9adb8af  status-at-add: in-progress
          0116  added 2b67b23  status-at-add: in-progress
          --- the ruleset that makes a merged pull request the only way onto main
          {"bypass_actors":[],"current_user_can_bypass":"never","enforcement":"active","pull_request":{"allowed_merge_methods":["squash","merge"],"dismiss_stale_reviews_on_push":false,"require_code_owner_review":false,"require_extra_approval_for_unattributed_changes":true,"require_last_push_approval":false,"required_approving_review_count":0,"required_review_thread_resolution":true,"required_reviewers":[]},"rules":["deletion","non_fast_forward","pull_request"]}
exit:     0
tree:     `1c19cf0` for half one — the same window E1 walks — read through git objects, so the
          result does not depend on which checkout it is run from. Half two is a live fetch and
          is stamped by nothing but its date; a ruleset can be edited, which is why the check
          this bean asks for must read it or cite it, not assume it.
```

**The `expect:` line is what was refuted.** Four of the nine were created `todo` — `0049`,
`0063`, `0065`, `0096` — and five were created `in-progress` and never flipped. `git log
--diff-filter=A ... | tail -1` takes the *oldest* addition, so a bean deleted and re-added would
report its first creation; none in this set was.

`97f13b0` and `157a57a` are also the rows E1's run lengths start at for `bean:0068` and
`bean:0102`, which is why both runs are contained by the window rather than clipped by it.

Half two carries more than this bean needs, and two of the extras are wrong in
`doc:00-constitution#workflow` §7.1 rather than here: the ruleset also sets
`require_extra_approval_for_unattributed_changes: true` and
`allowed_merge_methods: ["squash","merge"]`, and §7.1 says the ruleset carries its four named
items "and nothing else". `allowed_merge_methods` permits a merge method §7.2.7 does not.
`required_approving_review_count: 0` is now observed, where §7.1 calls it asserted and never
observed. Correcting §7.1 is out of this pull request's scope and the file is at its 500-line
ceiling (E3), so it is `bean:0122`.
