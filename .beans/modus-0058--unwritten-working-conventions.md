---
# modus-0058
title: Two working conventions every agent must derive by being bitten
status: in-progress
type: fix
priority: high
order: AO
created_at: 2026-08-29T00:00:00Z
---

# Two working conventions every agent must derive by being bitten

Neither is written where the agent that needs it will read it, and both cost a detour
per agent, per session.

**The `gh` credential trap.** A stale `GITHUB_TOKEN` in the environment shadows the keyring
credential and surfaces as `HTTP 401: Bad credentials` on an unrelated command. It is stated
in `.claude/skills/modus-work-package` step 0, which an agent sees only if that skill
triggers.

**The shared root.** The repository root is one checkout that several agents hold at once.
`doc:80-agent-operating-procedure#orchestrating` rule 0.3 said "run independent work
concurrently, in isolated worktrees" inside a table headed *If you are the orchestrator*, and
§0's preamble tells an implementing agent that steps 1-9 are its loop and step 0 is not. The
rule existed and addressed the wrong actor.

## Scope

Owned: `AGENTS.md`, `documentation/80-agent-operating-procedure.md`,
`.claude/skills/modus-work-package/SKILL.md`, `.claude/skills/modus-review/SKILL.md`,
this bean.

Not owned: `.claude/settings.json`. No code, no build files.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `AGENTS.md` states the credential trap, the working form and the diagnostic, in the Commands block every agent reads | command |
| 2 | The form `AGENTS.md` prescribes is the one that works, and the form it warns against is named as such | command |
| 3 | The trap is stated once: the copy in `.claude/skills/modus-work-package` becomes a reference | citation |
| 4 | The worktree rule is stated where an implementing agent is addressed, with its own anchor, and rule 0.3 cites it rather than restating it | citation |
| 5 | `AGENTS.md` workflow step 2 is a pointer, not a second statement of the rule | citation |
| 6 | Both documents stay inside `docs-lint` check 8's line budget | test-run |
| 7 | `./gradlew qualityCheck` green | test-run |

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | the Commands block carries it | `AGENTS.md` — `GITHUB_TOKEN= gh <args>` in the fence, and three lines naming `HTTP 401: Bad credentials` and `gh auth status` |
| 2 | the prescribed form works and the warned-against one is named | the three runs below |
| 3 | one statement, one place | `.claude/skills/modus-work-package/SKILL.md` step 0 now reads "`AGENTS.md`'s Commands block states the trap, the working form and the diagnostic"; `.claude/skills/modus-review/SKILL.md` line 26 uses the working form |
| 4 | the rule addresses the implementer | `doc:80-agent-operating-procedure#worktree-per-agent`, a section outside the orchestrator-only step 0; rule 0.3 now reads "the isolation rule is `#worktree-per-agent` and it binds the agents you spawn, not only you" |
| 5 | step 2 is a pointer | `AGENTS.md` step 2 names the anchor and states no rule of its own |
| 6 | line budgets | `docs-lint` check 8 green: `AGENTS.md` 76 of 120, `doc:80` 456 of 500 |
| 7 | the gate | `BUILD SUCCESSFUL`, `docs-lint: OK — 19 documents, 106 anchors, …` — below |

### Criterion 2 — the three forms, run against this repository

```
cmd:      gh auth status                       (with the stale GITHUB_TOKEN in the env)
observed: github.com
            X Failed to log in to github.com using token (GITHUB_TOKEN)
            - Active account: true
            - The token in GITHUB_TOKEN is invalid.
exit:     1

cmd:      GITHUB_TOKEN= gh auth status
observed: github.com
            ✓ Logged in to github.com account m4xy (keyring)
            - Token scopes: 'gist', 'read:org', 'repo'
exit:     0

cmd:      env -u GITHUB_TOKEN gh auth status
observed: github.com
            ✓ Logged in to github.com account m4xy (keyring)
exit:     0
```

The third form is equivalent **to `gh`** and is the one to avoid anyway: it is refused by
default in an agent sandbox, which cannot verify what `env` does to the command it wraps.
`.claude/skills/modus-evidence/SKILL.md` recorded that refusal and `AGENTS.md` was still
written to prescribe it, which is the shape of the problem this bean is about — the fact
existed in a file the agent that needed it did not open.

### Criterion 4 — why rule 0.3 could not simply be cited

The first version of this change pointed `AGENTS.md` step 2 at rule 0.3 and added
`git worktree add`, "never `git checkout -b` in the shared root", and "which several agents
hold at once`". None of that was at the anchor. That makes the pointer new normative content
wearing a citation that does not carry it, and `doc:05-authoring-for-agents#one-fact-one-place`
resolves a disagreement in favour of the source — which was silent, so the listing was an
unowned rule. Review caught it.

The rule now lives at `doc:80-agent-operating-procedure#worktree-per-agent`, addressed to
every agent, and both `AGENTS.md` step 2 and rule 0.3 are pointers.

### Criteria 6 and 7 — the gate

```
cmd:      bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 106 anchors, 830 references, 55 beans,
          26 graph edges, 16 selectable, 55 bean ids, 0 introduced, 55 on origin/main.
exit:     0

cmd:      wc -l AGENTS.md documentation/80-agent-operating-procedure.md
observed:  76 AGENTS.md
          456 documentation/80-agent-operating-procedure.md
```

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLint
          docs-lint: OK — 19 documents, 106 anchors, 835 references, 56 beans,
          26 graph edges, 16 selectable, 56 bean ids, 1 introduced, 55 on origin/main.
          BUILD SUCCESSFUL in 14s
          158 actionable tasks: 4 executed, 154 up-to-date
exit:     0
```

## Deliberately not done

No mechanical check that a branch was cut in a worktree. It is not decidable from repository
contents — the evidence is in a shell session, not a tree — so it is a rule enforced by
review and by the collision asserting itself, which is the honest status
(`doc:00-constitution#observed-failing`).
