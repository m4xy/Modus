---
# modus-0120
title: The rule that holds a bean open through review has no counterpart that closes it after the merge, and the backlog it leaves is invisible in every run
status: todo
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
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
  which `f55de2a` closed. `bean:0102` was in that set across **fourteen consecutive merges**.
- `bean:0116` shipped in `2b67b23` and stayed `in-progress` until `8c3fd82` closed it.
- `bean:0049` and `bean:0096` shipped in `1c19cf0` and `0e4324d` and were closed by the change
  that raises this bean.

The count returns to zero at exactly three commits in that window — `08936ee`, `f55de2a` and
`8c3fd82` — and every one of them is a `chore(beans): close …` change. Between them it is never
zero. That is the backlog, and nothing in the repository reports it.

## The state is derivable, and more cleanly than by a proxy (E1, E2)

§7.2.1 puts the `todo` → `in-progress` flip **on the branch**, at branch time. So the only way
`status: in-progress` reaches `origin/main` is through a merged pull request. A bean that is
`in-progress` on `origin/main` is therefore not *probably* one whose work has shipped; it is one
whose work has shipped, by construction of the lifecycle rule — and its close is outstanding.

That is stronger than either proxy the gap invites:

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

**The false-positive class.** A bean whose work is split across stacked pull requests is
`in-progress` on `main` after the first merges, correctly, and a check cannot tell it from a
pending close. `bean:0037` — the stacked-pull-request procedure — is still `todo`, so the
practice is undocumented, and no instance appears in the thirty commits measured. It is a
residual for the design to name, not a refutation.

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
The construction argument above does not depend on it.

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
