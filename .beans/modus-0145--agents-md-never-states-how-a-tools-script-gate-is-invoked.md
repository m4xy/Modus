---
# modus-0145
title: AGENTS.md never states how a tools/*.sh gate is invoked
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# `AGENTS.md` never states how a `tools/*.sh` gate is invoked

Why: `AGENTS.md`'s Commands block lists Gradle entry points and nothing else. An agent that
wants one gate rather than the whole of `qualityCheck` types `bash tools/docs-lint.sh`,
because that is the form the tree writes everywhere it writes a form at all. Two things are
wrong with it and `AGENTS.md` names neither. Bare `bash` resolves through `PATH`, so it is
not the interpreter `build.gradle.kts` pins for the same script. And an agent sandbox may
refuse the shape outright, because it cannot verify what an unpinned `PATH` lookup resolves
to — one agent reported retrying the refused form until its work package was exhausted.

**The gap is adjacent to a section that is otherwise thorough**, which is why it reads as
absent rather than as unwritten. `AGENTS.md` lines 37-47 state the `gh` credential trap,
name `env -u GITHUB_TOKEN` as refused with the reason, and enumerate three further refused
shapes — a compound command redirecting into a non-literal target, a multi-statement
`for … do … done` loop that pipes, and writing new files with the Write tool. An agent that
opens the section written to answer exactly this class of question is told, by its
completeness, that the class is covered.

## The interpreter is pinned everywhere except where the agent reads

```
cmd:      grep -n 'gateShell' build.gradle.kts
observed: 28:val gateShell = "/bin/bash"
          37:    commandLine(gateShell, "tools/docs-lint.sh")
          48:    commandLine(gateShell, "tools/docs-lint-test.sh")
          62:// gateShell is passed as the interpreter, so the pinned 3.2.57 is always one of the ones
          69:    commandLine(gateShell, "tools/docs-lint-gate-test.sh")
          81:    commandLine(gateShell, "tools/bash-compat-lint.sh")
exit:     0
```

The build pins the interpreter for every `tools/*.sh` task and states its reason in place
(`bean:0049`). Nothing carries that reason to the agent typing the command by hand:

```
cmd:      grep -rn 'gateShell\|interpreter' documentation/*.md AGENTS.md
observed: (no match)

cmd:      command -v bash
observed: /opt/homebrew/bin/bash
cmd:      bash --version | head -1
observed: GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0)
cmd:      /bin/bash --version | head -1
observed: GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)
```

A gate run through bare `bash` on this machine exercises 5.3.9. The pinned run exercises
3.2.57. `bean:0049` closed that hole for the build and left it open for the hand, because
the hand reads `AGENTS.md`.

## The refused-shape claim is a report, and this bean says so

```
cmd:      bash tools/docs-lint.sh; echo "exit=$?"
          (this agent's own sandbox, 2026-09-05, worktree at 7731d13)
observed: docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans,
          43 graph edges, 49 selectable, 112 bean ids, 0 introduced,
          112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          exit=0
```

The refusal was **not** reproduced here. Sandbox policy is per-session harness
configuration, not repository state, so no check in `doc:05-authoring-for-agents#checks` can
decide it and no document may state it as a property of the tree. The argument that survives
that is the interpreter one above: the pinned form is right in every sandbox, the bare form
is wrong in at least one, and a rule stated on the ground that holds everywhere does not
need the ground that does not.

## The wrong form is prescribed, not merely unmentioned

```
cmd:      grep -rn 'bash tools/' .claude/skills AGENTS.md documentation
observed: .claude/skills/modus-work-package/SKILL.md:96:- [ ] `bash tools/docs-lint.sh`
            exits 0 — `command`
          (no match in AGENTS.md or under documentation/)

cmd:      git grep -l 'bash tools/' origin/main -- '.beans/*.md' | wc -l
observed: 25
```

A checklist item an agent ticks off is the strongest prescription in the tree, and it writes
the form. The bean hits are a different matter: most stand in evidence transcripts of beans
already `completed`, which check 11 freezes, and a transcript corrected after the fact is a
falsified one. The fix is forward — the live skill and `AGENTS.md` — never the corpus.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `AGENTS.md`'s Commands block states the form for running one `tools/*.sh` gate by hand, beside the Gradle entry points an agent already reads | command |
| 2 | The bare-`bash`-through-`PATH` form is named in the refused-shapes paragraph that already names `env -u GITHUB_TOKEN`, rather than in a section of its own | citation |
| 3 | The reason stated is the one decidable from the tree — the interpreter is unpinned and disagrees with `gateShell` — and any sandbox-refusal claim is attributed as a report, not asserted as repository state | diff |
| 4 | The interpreter literal is not a second uncompared copy of `gateShell`, or the copy is defended against `bean:0090` with what would notice the two disagreeing | citation |
| 5 | `.claude/skills/modus-work-package`'s checklist names the prescribed form or cites `AGENTS.md`'s Commands block, so the tree prescribes one form and not two | command |
| 6 | Whatever `AGENTS.md` gains leaves it inside check 8's 120-line ceiling, measured rather than assumed — it is 102 lines at the head this was written on | test-run |
| 7 | Evidence transcripts in beans already `completed` are unchanged | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The pin itself and the `bashCompatLint` task that gates it (`bean:0049`), both shipped.
- Whether an agent sandbox should refuse the shape. That is harness configuration; no check
  reads it, and `doc:00-constitution#mechanical-enforcement` is about what the tree can
  decide.
- The `gh` credential trap and the three refused shapes already stated (`bean:0058`).
- The gate command list itself, which `doc:00-constitution#workflow` §7.2.4 states once and
  `AGENTS.md` derives from (`bean:0028`). This bean adds the invocation form for a script
  the gate already runs; it must not grow a second gate list.
