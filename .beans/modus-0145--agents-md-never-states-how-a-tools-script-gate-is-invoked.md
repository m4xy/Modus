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
because that is the form the tree writes everywhere it writes a form at all — including a
checklist item in a live skill. Bare `bash` resolves through `PATH`, so it is not the
interpreter `build.gradle.kts` pins for the same script, and one of the four pinned scripts
gives a **different verdict** under the two. `AGENTS.md` names neither the form nor the pin.

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

## The hand-run form gives a false green on the gate the pin exists to protect

This is the fact that carries the bean, and it is not about portability. Line 81 above runs
`tools/bash-compat-lint.sh` through `gateShell`, and that script's PARSE half invokes the
**currently running** interpreter:

```
cmd:      sed -n '35p;101,103p' tools/bash-compat-lint.sh
observed: #   PARSE   `$BASH -n` over every script, under the interpreter the gate pinned. Fails
          for f in $TARGETS; do
            if ! "${BASH:-bash}" -n "$f" 2> "$TMP/parse.err"; then
                fail "$f does not parse under ${BASH:-bash} (bash ${BASH_VERSION:-unknown}): $(cat "$TMP/parse.err")"
```

Run it as `bash tools/bash-compat-lint.sh` and `$BASH` is 5.3.9, so every `tools/*.sh` is
parsed by bash 5's parser — which accepts the syntax-level bash 4 features the pinned
3.2.57 parser rejects, and which the script's own header says the PARSE half exists to
catch "whether or not anyone thought to name it". The hand-run reports green on a script the
gate would reject. An agent that runs a gate by hand to save the price of `qualityCheck`, in
the form the tree taught it, gets the one answer the pin was introduced to prevent.

The wrong interpreter is not invisible, and that is a mitigation rather than a defence.
Line 98 prints it, unconditionally, before the parse runs:

```
cmd:      sed -n '98p' tools/bash-compat-lint.sh
observed: printf 'bash-compat: interpreter %s (bash %s)\n' "${BASH:-unknown}" "${BASH_VERSION:-unknown}"
```

Nothing fails on that line. It is a figure in the output that a reader must notice, compare
against `gateShell` and act on — which is exactly the shape
`doc:00-constitution#mechanical-enforcement` prefers a check to replace.

## The refused-shape claim is a report, and this bean says so

```
cmd:      bash tools/docs-lint.sh; echo "exit=$?"
          (this agent's own sandbox, 2026-09-05, worktree at 7731d13)
observed: docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans,
          43 graph edges, 49 selectable, 112 bean ids, 0 introduced,
          112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          exit=0
```

An agent reported the sandbox refusing this shape and retrying it until its work package was
exhausted. It was **not** reproduced here. Sandbox policy is per-session harness
configuration, not repository state, so no check in `doc:05-authoring-for-agents#checks` can
decide it and no document may state it as a property of the tree. The bean rests on the two
sections above instead, both decidable from the tree, and the report is recorded as a report.

## The wrong form is prescribed, not merely unmentioned

```
cmd:      grep -rn 'bash tools/' .claude/skills AGENTS.md documentation
observed: .claude/skills/modus-work-package/SKILL.md:96:- [ ] `bash tools/docs-lint.sh`
            exits 0 — `command`
          (no match in AGENTS.md or under documentation/)

cmd:      git grep -l 'bash tools/' origin/main -- '.beans/*.md' | wc -l
observed: 25
cmd:      git grep -l 'bash tools/' origin/main -- '.beans/*.md' | sed 's|^origin/main:||' > /tmp/f
          xargs -I{} grep -m1 -H '^status:' {} < /tmp/f | sed 's/.*status: //' | sort | uniq -c
observed:   14 completed
             3 in-progress
             8 todo
```

A checklist item an agent ticks off is the strongest prescription in the tree, and it writes
the form. The bean hits are a different matter: 14 of the 25 are already `completed`, which
check 11 freezes, and a transcript corrected after the fact is a falsified one. The other 11
are live. The fix is forward — the skill and `AGENTS.md` — and reaches no completed bean.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `AGENTS.md`'s Commands block states a literal command an agent can type to run one `tools/*.sh` gate, beside the Gradle entry points it already lists | command |
| 2 | The bare-`bash`-through-`PATH` form is named in the refused-shapes paragraph that already names `env -u GITHUB_TOKEN`, rather than in a section of its own | citation |
| 3 | The reason stated is one decidable from the tree — the interpreter is unpinned, and `bash-compat-lint`'s PARSE half gives a different verdict under it — and the sandbox-refusal report is attributed as a report, not asserted as repository state | diff |
| 4 | Criterion 1's literal necessarily writes `/bin/bash` a second time, since `gateShell` is a Kotlin value no document can reference. That copy is named as a `bean:0090` instance and either gains a mechanism that compares the two, or is recorded as an accepted residual with the reason | diff |
| 5 | `.claude/skills/modus-work-package`'s checklist names the prescribed form or cites `AGENTS.md`'s Commands block, so the tree prescribes one form and not two | command |
| 6 | Whatever `AGENTS.md` gains leaves it inside check 8's 120-line ceiling, measured rather than assumed — it is 102 lines at the head this was written on | test-run |
| 7 | No bean already `completed` is edited | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The pin itself and the `bashCompatLint` task that gates it (`bean:0049`), both shipped.
  This bean adds no check; it states how to invoke the ones that exist.
- Whether an agent sandbox should refuse the shape. That is harness configuration; no check
  reads it, and `doc:00-constitution#mechanical-enforcement` is about what the tree can
  decide.
- The `gh` credential trap and the three refused shapes already stated (`bean:0058`).
- The gate command list itself, which `doc:00-constitution#workflow` §7.2.4 states once and
  `AGENTS.md` derives from (`bean:0028`). This bean adds the invocation form for a script
  the gate already runs; it must not grow a second gate list.
