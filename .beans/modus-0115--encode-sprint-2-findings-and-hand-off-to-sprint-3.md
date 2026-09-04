---
# modus-0115
title: Encode sprint 2's findings, and hand off to sprint 3
status: completed
type: task
priority: high
created_at: 2026-09-03T00:00:00Z
updated_at: 2026-09-04T00:00:00Z
---

# Encode sprint 2's findings, and hand off to sprint 3

Sprint 2 ran from `8181726` to `05939b8` — twenty-three merges, that pair of shas being what
every range command in this bean is stamped against. Its findings
existed only in the orchestrator's session, and a session is discarded
(`doc:50-memory-and-evidence` §1), so every one of them was a thing the next agent would pay to
rediscover — which is what `doc:README#encoding-rule` exists to stop.

**Most of them are rules, and rules belong in documentation.** `bean:0068` is the precedent for
the shape of this record; the decision below is what differs from it.

## The decision: rules, not work items

The sprint's own diagnosis is that the machinery which verifies the work has outgrown the work,
and that **raising one bean per instance is what produced the backlog it diagnoses**. Raising a
bean per finding would have been the sprint diagnosing an error and then committing it. So two
beans were raised, both for mechanism nobody owns — `bean:0113` and `bean:0114`, ids allocated
centrally — and everything else was encoded as a rule, in the document that owns the subject,
by amending what was already there rather than appending a parallel section.

## The findings, and where each went

Reported by the orchestrator of sprint 2 from its own session and recorded as reported, in
`bean:0068`'s convention: where a finding has a durable artefact in this repository, the
artefact is cited and is the thing a reader should check; where it has none, it is attributed to
the sprint and to nothing else.

| # | finding | where it went |
|---|---|---|
| F1 | An orchestrator's own checkout is a vantage point that drifts silently, and no check covers it | `doc:80-agent-operating-procedure#orchestrating` rule 0.6 |
| F2 | Classic branch protection and rulesets are two APIs over one question, and the classic one 404s on a protected branch | `bean:0047`, beside the command it would mislead |
| F3 | The gate is watched and not required; every merge this sprint landed under it | the handoff below, and `bean:0047` |
| F4 | An enumeration is a count over a growing set | already normative — `doc:05-authoring-for-agents#one-fact-one-place`, *a set that can grow is named, never counted*. Not encoded twice |
| F5 | A dead agent's work survives in its worktree | folded into F8; the general rule is the one worth stating |
| F6 | The machinery that verifies the work has outgrown the work | the handoff below. It is a judgement about what to do next, not a rule |
| F7 | A `tree:` stamp does not only date a figure; it makes every figure sharing it falsifiable | `doc:50-memory-and-evidence` §2.2, in the row that already required the tree |
| F8 | An agent's last message is evidence of what it was about to do, never of whether it did it | `doc:80-agent-operating-procedure#orchestrating` rule 0.9 |
| F9 | A quantifier in a brief comes back as a count in the artefact | `doc:80-agent-operating-procedure#orchestrating` rule 0.7 |
| F10 | Two fix rounds in three introduced a fresh blocking defect; a fix round needs the review the change got | `doc:80-agent-operating-procedure#orchestrating`, reviewer rule R6 |
| F11 | Nearly every fence defect was transcription, not reasoning | `doc:50-memory-and-evidence#capturing`, with `bean:0104`'s inverse direction named as the same rule |
| F12 | A corpus sweep is falsifiable by a third party's merge, and re-running it belongs to that merge | `doc:50-memory-and-evidence#corpus-figures` |
| F13 | The figure that gets reasoned is the one whose measurement needs apparatus | `doc:50-memory-and-evidence` §2.2 and §2.5 — the shape, and the tell that "could not verify" is |
| F14 | Relaying a command is not verifying it | `doc:80-agent-operating-procedure#orchestrating` rule 0.8 |
| F15 | Reproducible is not correct: a sweep can be byte-perfect and wrong about its own subject | `doc:50-memory-and-evidence#corpus-figures`. `bean:0112` raised it and its criteria 1 and 2 are discharged by that text |
| F16 | An elision can conceal an output no run could produce | `doc:50-memory-and-evidence#capturing`. `bean:0091` criterion 2 is discharged by it |
| F17 | Mutable closure criteria make closure a record rather than a test | `bean:0113` |
| F18 | A record that measures a corpus it belongs to changes that corpus | `doc:50-memory-and-evidence#corpus-figures` |
| F19 | A range quoted to bound an excerpt is reread as the passage's extent | `doc:50-memory-and-evidence#capturing`, as the rule that a locator carries its derivation |
| F20 | A `refs:` omission makes a true claim uncheckable, and fails silently | `bean:0114` |
| F21 | The sentinel method for a figure that describes the tree it lives in | `doc:50-memory-and-evidence#corpus-figures`, and used to capture E6 below |
| F22 | When a reader cannot tell sound from unsound by looking, remove the ambiguity, not the instance | `doc:50-memory-and-evidence#capturing` |
| F23 | Writing a rule down does not install it | `doc:80-agent-operating-procedure#orchestrating` rule 0.5, extended |

Findings whose subject a bean already owns were not encoded a second time: `bean:0110` owns the
racing-dispatch row in the same table these rules were added to, and `bean:0100` owns the three
mechanisms by which a claim arrives carrying someone's confidence. Neither is restated here or
in the documents (`doc:05-authoring-for-agents#one-fact-one-place`).

## On this bean's own id, which was not allocated centrally

The brief for this change allocated `0113` and `0114`, said to take no others, and said to ask
before taking a third. `doc:00-constitution` §7.2's first step requires a work item, and
`doc:80-agent-operating-procedure#orchestrating` names *allocating an id centrally and then
accepting a branch that ships no work item* as an orchestration anti-pattern — so shipping this
change without a bean would have committed the failure the brief was written to avoid, and
asking would have meant stopping the change to do it. `0115` was taken as the next id free on
`origin/main` rather than the next free in this worktree (`bean:0051`), and it is flagged in the
pull-request body.

Renaming it costs one file rename and one line: `bean:0115` appears nowhere but the note
appended to `bean:0047`, and check 6 resolves a `bean:` reference by id-glob rather than by
slug. If the orchestrator had allocated `0115` elsewhere, check 13c would say so before the
merge, which is the residual `bean:0051` accepts.

## Scope

Owned: `documentation/50-memory-and-evidence.md`, `documentation/80-agent-operating-procedure.md`,
this bean, `bean:0113`, `bean:0114`, and two edits to `bean:0047`: the API note appended to it,
and its ruleset fence, whose two `gh api` commands carried no `GITHUB_TOKEN=` prefix and so
return `401` in the environment `AGENTS.md` documents. That fence was **re-run** with the
prefix and recaptured, not edited under an existing observation — and the recapture restored
three lines of output that had been recorded as one.

Not owned, and not touched: `tools/`, any build file, `AGENTS.md`, `doc:00-constitution` (full at
its ceiling — `bean:0089` carries why that matters), and every other bean. In particular
`bean:0091`, `bean:0100`, `bean:0104`, `bean:0110` and `bean:0112` keep their criteria; where a
rule written here discharges one, that is recorded above and closing them is sprint 3's
consolidation, not this change.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | The ten evidence findings are in `doc:50-memory-and-evidence`, amending its existing tables where one already covered the subject and adding two sections where none did | E1 — both anchors declared, and every row added to §2.2 and §2.5 printed by its opening cell |
| 2 | The six loop findings are in `doc:80-agent-operating-procedure`, in the table an orchestrator reads and the one a reviewer is briefed from | E2 — five rows numbered 0.5 to 0.9, and the R6 row |
| 3 | Both documents stay inside `docs-lint` check 8's ceiling, measured rather than assumed | E3 |
| 4 | Two beans are raised for the findings, on the ids allocated centrally, and neither restates a rule. The third file this branch adds is this work item, whose id was **not** allocated centrally — see below | E4 — the three files this branch adds, and the one it amends |
| 5 | The gate observation in the handoff is re-verified against the live ruleset rather than relayed (`doc:80-agent-operating-procedure#reports-are-evidence`) | E5 |
| 6 | The corpus sweep a `completed` bean carries is re-run on this tree, because this change edits the corpus it searches | E7 — eleven files where `bean:0105` recorded nine, both newcomers accounted for, and `doc:50` still absent |
| 7 | `./gradlew qualityCheck` green | E6 |

## Evidence

### E1 — `doc:50` carries both new anchors, and the rows amending what existed

```
cmd:      grep -n 'a id="capturing"\|a id="corpus-figures"' documentation/50-memory-and-evidence.md
observed: 189:### 2.6 Capturing a transcript <a id="capturing"></a>
          209:### 2.7 A figure whose subject is this repository <a id="corpus-figures"></a>
```

**This block shipped with `208:` where the anchor is at `209:`, and review caught it.** The `189:`
beside it was right, which is what hid it. E6 got the sentinel cycle because its figure was the
one I distrusted; E1 got a single capture, taken before a later edit to §2.6 pushed §2.7 down one
line. Again this sprint, a rule shipped beside a fresh violation of itself — and this rule is
forty lines away in the same diff (`doc:50-memory-and-evidence#capturing`): the locator did carry
its derivation, the derivation said `209`, and the prose said `208`. Writing the rule down did not
install it (`doc:80-agent-operating-procedure#orchestrating` rule 0.5); the block below is
re-captured, not hand-corrected.

**Rule 0.8 is written for what an orchestrator relays to an agent, and this change found the gap
from inside it.** The path here was reviewer to orchestrator to author: a reviewer's ratio for the
`grep` race in E3 became a brief and nearly became a claim in this bean, and was refused only
because a reviewer's figure is a report like any other
(`doc:80-agent-operating-procedure#reports-are-evidence`). Two relayed ratios exist, reported and
not measured here, and they disagree in direction on one machine — which is the answer: `ugrep`
parallelises across files, so the order is a race, and a race has no rate to quote. E3 keeps the
five runs it has and no ratio. Whether rule 0.8 should name the reviewer as well as the
orchestrator is sprint 3's to decide.

```
cmd:      grep -o '^| A fence composed from understanding rather than captured from a run\|^| A figure whose measurement needs apparatus nobody has built\|^| A search over chosen phrasings, offered as evidence about a rule\|^| a figure whose measurement needs apparatus\|^| a locator — a line number, a line range, a section extent — quoted with no command beside it' documentation/50-memory-and-evidence.md
observed: | A fence composed from understanding rather than captured from a run
          | A figure whose measurement needs apparatus nobody has built
          | A search over chosen phrasings, offered as evidence about a rule
          | a figure whose measurement needs apparatus
          | a locator — a line number, a line range, a section extent — quoted with no command beside it
```

### E2 — `doc:80` carries the four orchestrator rules and the reviewer rule

```
cmd:      grep -c '^| 0\.[5-9] |' documentation/80-agent-operating-procedure.md
observed: 5
```

```
cmd:      grep -o '^| R6 | \*\*A fix round is reviewed as the change was\.\*\*' documentation/80-agent-operating-procedure.md
observed: | R6 | **A fix round is reviewed as the change was.**
```

### E3 — both documents inside check 8's ceiling

`documentation/README.md` states `max_lines: 500`, which `docs-lint` check 8 reads there. The
`| sort` is part of the command and not something done to its output: `grep -c` over two files
emits them in either order on this system, observed five times, so without it the block
reproduces as a set rather than as a byte stream.

```
cmd:      grep -c '' documentation/50-memory-and-evidence.md documentation/80-agent-operating-procedure.md | sort
observed: documentation/50-memory-and-evidence.md:498
          documentation/80-agent-operating-procedure.md:498
```

### E4 — what this branch introduces

```
cmd:      git diff --name-status origin/main...HEAD -- .beans
observed: M	.beans/modus-0047--require-the-gate-check.md
          A	.beans/modus-0113--a-close-that-rewrites-its-criteria-is-indistinguishable.md
          A	.beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md
          A	.beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
```

### E5 — the ruleset, re-verified on the day this was written

```
cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/rulesets/21765196 --jq '.name, .enforcement, (.rules[].type)'
observed: main-protected
          active
          deletion
          non_fast_forward
          pull_request
```

Three rules, and no `required_status_checks`. `bean:0047` is the work item that closes it and
is blocked on a human.

### E6 — the gate, captured against a sentinel

The counts `docs-lint` prints describe the tree this bean is in, so the capture was taken with
the sentinel `@@docslint@@` standing in the fence below, making the asserted string absent from
the tree it measures; the result was inserted, and the command re-run and diffed against the
first capture, which is the step that would catch a figure agreeing with itself. The
substitution is measurement-neutral: it sits inside a fenced block, which check 6 does not read
for references, and it adds no document, anchor, bean, graph edge or bean id. The two runs are
byte-identical, so the line below describes the tree that carries it and not the one before it.
The gate itself — `./gradlew qualityCheck`, which runs `docsLint` — exited 0 on the same tree.

```
cmd:      bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1453 references, 98 beans, 37 graph edges, 45 selectable, 98 bean ids, 3 introduced, 95 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
```

### E7 — the sweep `bean:0105` records, re-run on this tree

`bean:0105` is `completed` and carries a sweep over `.beans` and `documentation`; this change
edits both. Re-running it is this change's job, not that bean's
(`doc:50-memory-and-evidence#corpus-figures`).

```
cmd:      grep -rl 'not firing\|does not fire\|fires on every\|never fires' .beans documentation | sort
observed: .beans/modus-0068--encode-sprint-1-findings.md
          .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          .beans/modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
          .beans/modus-0089--anchors-cited-by-completed-beans-pin-a-document.md
          .beans/modus-0090--constants-that-must-match-an-authority.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          .beans/modus-0110--dispatching-a-review-and-an-edit-against-one-head.md
          .beans/modus-0112--a-sweep-for-a-wording-read-as-a-sweep-for-a-rule.md
          .beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md
          .beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
          documentation/80-agent-operating-procedure.md
```

Eleven files where that bean recorded nine, and the two newcomers are this change's own: the
line of `bean:0105` quoted in `bean:0114` carries one of the four patterns, and the block above
carries all four, because a command that searches for a string puts that string into the corpus
it searches (`bean:0103`). Neither newcomer states the rule the sweep was written to find, so
the longer list says what the shorter one said — and the file that decides the question it was
asked is still not among them:

```
cmd:      grep -c 'not firing\|does not fire\|fires on every\|never fires' documentation/50-memory-and-evidence.md
observed: 0
```

---

## The handoff to sprint 3

### The evidence machinery is both the largest consumer of effort and the largest source of defects

Reported from the sprint, with no durable artefact beyond the pull requests themselves: every
defect found in the last five review rounds across two pull requests was in an evidence record.
The code was sound and untouched throughout. That is not an argument for abandoning the
machinery — the machinery is why the defects were found at all. It is an argument that raising
one bean per instance is the wrong response, because raising one bean per instance is what
produced the family below.

The family, by id, at `05939b8` — a reading of what these beans are about, not a measurement,
and the membership is the part to attack:

```
cmd:      grep -l '^status: todo' .beans/modus-{0049,0057,0059,0060,0061,0062,0063,0086,0087,0089,0091,0093,0094,0096,0098,0099,0100,0102,0103,0104,0105,0106,0107,0108,0109,0110,0112}--*.md | sed 's|.beans/modus-||;s|--.*||' | sort | tr '\n' ' '
observed: 0049 0057 0059 0060 0061 0062 0086 0087 0089 0091 0093 0094 0096 0098 0099 0100 0103 0104 0106 0107 0108 0109 0110 0112 
```

```
cmd:      grep -l '^status: todo' .beans/modus-{0049,0057,0059,0060,0061,0062,0063,0086,0087,0089,0091,0093,0094,0096,0098,0099,0100,0102,0103,0104,0105,0106,0107,0108,0109,0110,0112}--*.md | grep -c .
observed: 24
```

**Both fences above were re-run on the tree that closes this bean, and both are unchanged —
same twenty-four ids, same count of 24.** They are re-run rather than assumed because closing
`0102` moves it out of `in-progress`, and `0102` **is** one of the twenty-seven ids these two
commands enumerate. It does not move the count for a reason worth writing down rather than
being lucky about: the predicate is `^status: todo`, and `0102` had already left `todo` before
this change touched it, so it was absent from the twenty-four before and is absent after.

The complement is the check, because it is the half that moves. `grep -L` is `grep -l`'s
inverse over the same argument list, so the two partition it and neither can be read without
falsifying the other:

```
cmd:      ls .beans/modus-{0049,0057,0059,0060,0061,0062,0063,0086,0087,0089,0091,0093,0094,0096,0098,0099,0100,0102,0103,0104,0105,0106,0107,0108,0109,0110,0112}--*.md | grep -c .
observed: 27
exit:     0

cmd:      grep -L '^status: todo' .beans/modus-{0049,0057,0059,0060,0061,0062,0063,0086,0087,0089,0091,0093,0094,0096,0098,0099,0100,0102,0103,0104,0105,0106,0107,0108,0109,0110,0112}--*.md | sed 's|.beans/modus-||;s|--.*||' | sort | tr '\n' ' '
observed: 0063 0102 0105 
exit:     0
```

Twenty-seven in, twenty-four `todo` and three not: `0063` and `0105` were already `completed`
on `main`, and `0102` is the one this change moves. It moves from `in-progress`, which the
predicate never matched, which is why the twenty-four is the same twenty-four.

Against that, the product line has moved three items in two sprints:

```
cmd:      grep -H '^status:' .beans/modus-{0009,0011,0012,0013,0014,0015,0016,0017,0018,0019,0020,0021,0022,0030,0065}--*.md | sed -E 's|\.beans/modus-([0-9]{4})--[^:]*:status: |\1 |' | sort | tr '\n' ' '
observed: 0009 completed 0011 todo 0012 todo 0013 todo 0014 todo 0015 todo 0016 todo 0017 todo 0018 todo 0019 todo 0020 todo 0021 todo 0022 todo 0030 completed 0065 completed 
```

**Re-run, not re-typed, and two things about it changed.** `bean:0065` merged as PR #55 and
is `completed` as of the change that closes this bean, so the block above is no longer the one
this bean shipped with; re-running a sweep belongs to the change that moves its corpus
(`doc:50-memory-and-evidence#corpus-figures`), and that change is this close. The command also
gained a `| sort`, which is the repair E3 above already makes to a fence of the same shape.
Without it this one is a set rather than a byte stream: `grep` resolves either to
`/usr/bin/grep`, which reads the files in argument order, or to the `ugrep` shim an agent's
interactive shell installs, which parallelises across files and emits each as it finishes.
E3 has the `| sort` and this fence did not, because E3 is where the race was noticed — which
is the argument for that rule living in `doc:50` and not in one author's habit.

The race is measured rather than asserted. All three runs are on the tree this close produces,
and each collapses five runs to the set of distinct outputs, so the answer is a count of
orders and not an order. **Each fence carries the shell it depends on**, on a `shell:` line
beside its `cmd:`, and the lines are not decoration: every one was run under both an
interactive shell and `bash -c` before it was written. Only the first fence differs between
them, and it differs by the whole claim — 5 under the shim, 1 under `bash -c`. Stating that
on the fence is the point rather than a courtesy: the finding is that which `grep` you have
decides the answer and nothing on the page tells you, so a fence that left its own condition
to the prose two lines up would be reproducing the hazard in its own presentation.

```
cmd:      for i in 1 2 3 4 5; do grep -H '^status:' .beans/modus-{0009,0011,0012,0013,0014,0015,0016,0017,0018,0019,0020,0021,0022,0030,0065}--*.md | sed -E 's|\.beans/modus-([0-9]{4})--[^:]*:status: |\1 |' | tr '\n' ' '; echo; done | sort -u | grep -c .
shell:    an agent's interactive zsh, where `grep` is the shell function the harness
          installs; `type grep` names the snapshot file it comes from. **Under `bash -c`,
          `/bin/sh` or CI this returns 1**, because `grep` resolves to `/usr/bin/grep`
          there. That difference is the finding, not a defect in the capture, and it is
          stated on the fence because the finding is that nothing on the page states it
observed: 5
exit:     0

cmd:      for i in 1 2 3 4 5; do grep -H '^status:' .beans/modus-{0009,0011,0012,0013,0014,0015,0016,0017,0018,0019,0020,0021,0022,0030,0065}--*.md | sed -E 's|\.beans/modus-([0-9]{4})--[^:]*:status: |\1 |' | sort | tr '\n' ' '; echo; done | sort -u | grep -c .
shell:    any — `| sort` makes the answer independent of which `grep` is on `PATH`, which
          is the whole point of adding it; 1 under the shim and 1 under `/usr/bin/grep`
observed: 1
exit:     0

cmd:      for i in 1 2 3 4 5; do /usr/bin/grep -H '^status:' .beans/modus-{0009,0011,0012,0013,0014,0015,0016,0017,0018,0019,0020,0021,0022,0030,0065}--*.md | sed -E 's|\.beans/modus-([0-9]{4})--[^:]*:status: |\1 |' | tr '\n' ' '; echo; done | sort -u | grep -c .
shell:    any — the binary is named by absolute path, so the shim is bypassed whatever
          the shell; the control for the first fence, and 1 under both
observed: 1
exit:     0
```

Under the shim: five runs, five orders, none of them ascending. With `| sort`, or with
`/usr/bin/grep`, one order — and it is the ascending one the fence above records. So the
block this bean shipped was correct on the shell that captured it and irreproducible on the
shell most agents read it from, and nothing distinguishes the two from the page. That is what
the `| sort` buys: it makes the fence's answer independent of which `grep` the reader has,
which is the only version of *reproducible* worth the word.

**The figure stays three.** It was three with `0065` in flight and it is three with `0065`
closed, because closing an item that had already left `todo` moves no item: of the fifteen ids
this fence reads, `0009`, `0030` and `0065` are outside `todo` before the close and the same
three are outside it after. What the close falsifies is the sentence that named one of the
three as unfinished, and that sentence is corrected below rather than left to be read as
current.

`bean:0009`, `bean:0030` and `bean:0065` are `completed`. The six bounded contexts, the store
adapter, the REST layer, auth, the runner, SSE and the live backoffice are all still `todo`.

### What sprint 3 is

1. **One consolidation of the evidence family by cause, not twenty-four instance fixes.** The
   causes are a naive fence state, check 14 gated on numbering, the vantage point a resolver
   reads from, and transcript fidelity. Fix the causes, close the instances as evidence, and
   resist raising a bean per instance found on the way — including the ones this change
   discharges, which are listed above and are closures rather than work.
2. **Then the walking skeleton**: `bean:0013`, `bean:0017`, `bean:0018` — the shortest path to
   a system that runs end to end, which is also the only thing that will exercise the
   product-side rules the evidence machinery cannot reach.
3. **Do not open a stack again without pricing N−1 rebases up front.** This sprint's stack of
   five cost four. It is a repository setting and not a mistake: `main` accepts squash merges
   only, so every merge rewrites the base of everything above it. The decision to stack is
   where that price is paid or avoided.

### The two items that need a human, and have all sprint

- **Revoke the leaked `GITHUB_TOKEN`.** It is invalid, it shadows the working keyring
  credential, and every agent brief this sprint carried the `GITHUB_TOKEN= gh …` workaround
  because of it (`AGENTS.md`, Commands).
- **`bean:0047`** — the `gh api -X PUT` that adds `required_status_checks` with
  `context: "gate"`. The ruleset was re-verified for this handoff (E5): `deletion`,
  `non_fast_forward`, `pull_request`, and no status check at all.

**Every one of sprint 2's twenty-three merges landed under a gate that was watched and never
required.** `gate` reporting green blocked nothing; a red pull request would have merged. That
is `doc:00-constitution#observed-failing`'s own case — a mechanism that is real, correct,
observed failing, and still not run — and it is the most important sentence in this handoff.

```
cmd:      git log --oneline 8181726..05939b8 | grep -c .
observed: 23
```

```
cmd:      git log --oneline 8181726..05939b8 | grep -oE '\(#[0-9]+\)' | sort -u | tr '\n' ' '
observed: (#44) (#45) (#46) (#47) (#48) (#49) (#50) (#51) (#52) (#53) (#54) (#55) (#56) (#57) (#58) (#59) (#60) (#61) (#62) (#63) (#64) (#65) (#66) 
```

```
cmd:      git grep -h '^status:' 05939b8 -- .beans | sort | uniq -c
observed:   29 status: completed
             4 status: in-progress
            62 status: todo
```

## Closed — merged as PR #67, squashed onto `main` as `9adb8af`

A bean cannot close itself: its evidence includes its own merge, so the close is a separate
change (`doc:00-constitution` §7.2.1). Criteria 1 to 7 are answered by E1 to E7 above. What
this section adds is that the two documents and the three bean files those blocks describe are
on `main`, and the gate run for the change that closes all five of this sprint's shipped-but-open
beans.

```
cmd:      git log --oneline -1 9adb8af
observed: 9adb8af docs(50,80,beans): encode sprint 2's findings and hand off to sprint 3 (#67)
exit:     0
```

E1's two anchors and E2's five rules, read out of the merged blobs rather than out of the
working tree, because a document is the artefact most likely to be edited by the next change:

```
cmd:      git grep -n 'id="capturing"' 9adb8af -- documentation/50-memory-and-evidence.md
observed: 9adb8af:documentation/50-memory-and-evidence.md:189:### 2.6 Capturing a transcript <a id="capturing"></a>
exit:     0

cmd:      git grep -n 'id="corpus-figures"' 9adb8af -- documentation/50-memory-and-evidence.md
observed: 9adb8af:documentation/50-memory-and-evidence.md:209:### 2.7 A figure whose subject is this repository <a id="corpus-figures"></a>
exit:     0

cmd:      git grep -c '^| 0\.[5-9] |' 9adb8af -- documentation/80-agent-operating-procedure.md
observed: 9adb8af:documentation/80-agent-operating-procedure.md:5
exit:     0
```

`189` and `209` are what E1 records, and this is the run E1's own note says the block should
have been: a locator carrying the command that produced it, at the tree it describes
(`doc:50-memory-and-evidence#capturing`).

Criterion 4's three files:

```
cmd:      git ls-tree -r --name-only 9adb8af -- .beans/modus-0113--a-close-that-rewrites-its-criteria-is-indistinguishable.md .beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md .beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
observed: .beans/modus-0113--a-close-that-rewrites-its-criteria-is-indistinguishable.md
          .beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md
          .beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
exit:     0
```

`0115` was taken as the next id free on `origin/main` rather than centrally, and check 13c is
what would have said so before the merge. It merged clean, so no sibling had taken it — the
residual `bean:0051` accepts did not fire.

### E7's sweep, re-run because this change edits the corpus it searches

`bean:0105`'s sweep runs over `.beans` and `documentation`, and this close edits five files
under `.beans`. Re-running it is this change's job, not that bean's, and not the job of the
merge that last ran it (`doc:50-memory-and-evidence#corpus-figures`).

```
cmd:      grep -rl 'not firing\|does not fire\|fires on every\|never fires' .beans documentation | sort
observed: .beans/modus-0068--encode-sprint-1-findings.md
          .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          .beans/modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
          .beans/modus-0089--anchors-cited-by-completed-beans-pin-a-document.md
          .beans/modus-0090--constants-that-must-match-an-authority.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          .beans/modus-0110--dispatching-a-review-and-an-edit-against-one-head.md
          .beans/modus-0112--a-sweep-for-a-wording-read-as-a-sweep-for-a-rule.md
          .beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md
          .beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
          documentation/80-agent-operating-procedure.md
exit:     0
```

### The gate for this close

`./gradlew qualityCheck` is the gate `rule:ci/build` runs, and `tools/docs-lint.sh` is inside
it. The `docs-lint` counts describe the tree that carries them, so both captures are taken by
the sentinel method (`doc:50-memory-and-evidence#corpus-figures`): the asserted strings were
absent from the tree at capture time, standing as `@@sp3gate@@` and `@@sp3sweep@@`, and each
was re-run after insertion and diffed against its first capture. The `docs-lint` counts line
itself is `bean:0102`'s criterion 8 and lives there, not here — quoted once, and marked
`[same]` here rather than repeated (`bean:0091`, `doc:05-authoring-for-agents#one-fact-one-place`).
Nothing is elided: `:docsLint` is the last task before `:qualityCheck`, and the block below is
every line from the first to the build result, with only the `docs-lint` counts line replaced
by `[same]`. **The last two lines are the ones a re-runner will not match**, and that is a
property of the lines and not a defect in the capture: the duration is a duration, and the
executed-versus-up-to-date split depends on configuration-cache and build-cache state
(`bean:0065` criterion 10 records the same thing and for the same reason — an earlier run of
this same gate on this same branch reported `168 actionable tasks: 55 executed, 113 from
cache`). `BUILD SUCCESSFUL` and the exit code are the reproducible half. The `docs-lint`
line inside this run is reproducible, and its fixed point is established at `bean:0102` F6
rather than here — where it is also the check that this gate run and that standalone run
read the same tree.

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLint
          [same as bean:0102 F6]

          > Task :qualityCheck

          BUILD SUCCESSFUL in 21s
          159 actionable tasks: 5 executed, 154 up-to-date
exit:     0
```
