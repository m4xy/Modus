---
# modus-0058
title: Two working conventions every agent must derive by being bitten
status: completed
type: fix
priority: high
order: AO
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T22:00:00Z
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

Merged as PR #37, squashed onto `main` as `f39f100`. Every `observed` cell below was
re-taken at `8181726` — the commit `main` carries today — because a convention that was true
on the branch and has since been edited by another change is not a convention that merged.

| # | criterion | command | expectation | observed |
|---|---|---|---|---|
| 1 | the Commands block states the trap, the working form and the diagnostic | `sed -n '34p;37,41p' AGENTS.md` | the working form is in the fence every agent reads, and the symptom and the diagnostic are named beside it | `GITHUB_TOKEN= gh <args>              # every gh call — the credential trap below`, then ``otherwise use, and surfaces as `HTTP 401: Bad credentials` on an unrelated command.`` and ``` `gh auth status` names which credential is in use. Clear it inline, as above — ``` — all six lines in the fence below |
| 2 | the prescribed form works, and the warned-against one is named as such | `gh auth status` then `GITHUB_TOKEN= gh auth status` | the bare form fails on the stale token and the prescribed form authenticates from the keyring | bare: `X Failed to log in to github.com using token (GITHUB_TOKEN)`, `- The token in GITHUB_TOKEN is invalid.`, exit 1; prescribed: `✓ Logged in to github.com account m4xy (keyring)`, `- Active account: true`, exit 0 — full transcripts below |
| 3 | the trap is stated once; the skill copies become references | `grep -rn "Commands block" .claude/skills/` | no skill restates the trap; each names `AGENTS.md`'s Commands block as the place it is stated | three hits, no fourth: `modus-work-package/SKILL.md:32`, `modus-review/SKILL.md:31`, `modus-evidence/SKILL.md:76`, each of the form "`AGENTS.md`'s Commands block states …" — verbatim in the fence below |
| 4 | the worktree rule is stated where an implementing agent is addressed, with its own anchor, and rule 0.3 cites it | `grep -n "worktree-per-agent" documentation/80-agent-operating-procedure.md` | the anchor owns a section of its own, outside the orchestrator-only step 0, and rule 0.3 points at it | `49:## Working tree <a id="worktree-per-agent"></a>` — §0 begins at line 56, so the section precedes it — and `65:` rule 0.3 reads "the isolation rule is `#worktree-per-agent` and it binds the agents you spawn, not only you" |
| 5 | `AGENTS.md` workflow step 2 is a pointer, not a second statement of the rule | `sed -n '67,68p' AGENTS.md` | step 2 names the anchor and states no rule of its own | two lines, the second being ``   (`doc:80-agent-operating-procedure#worktree-per-agent`). No direct commits to `main`.`` — the first names the branch kinds and the phrase "in a worktree of your own" and carries no rule text of its own; both in the fence below |
| 6 | both documents stay inside `docs-lint` check 8's line budget | `wc -l AGENTS.md documentation/80-agent-operating-procedure.md`, and `bash tools/docs-lint.sh` | under the 120-line `AGENTS.md` ceiling check 8 hard-codes and the 500-line `max_lines` `documentation/README.md` states | at `f39f100`: `76 AGENTS.md`, `456 documentation/80-agent-operating-procedure.md`; at `8181726`: `82 AGENTS.md`, `456` — later merges grew `AGENTS.md` by six lines and it is still 38 under the ceiling. Check 8 is green in both runs of the gate below |
| 7 | `./gradlew qualityCheck` green | `./gradlew qualityCheck` | green with `docsLint` inside it, both before and after this closure is written | clean tree: `BUILD SUCCESSFUL in 19s`, `167 actionable tasks: 54 executed, 113 from cache`, `0 closing transitions`. With the four closures in place: `BUILD SUCCESSFUL in 15s`, `158 actionable tasks: 4 executed, 154 up-to-date`, `4 closing transitions, 31 criteria checked, 0 unnumbered` — both transcripts below |

### Criteria 1 to 5 — the closing runs at `8181726`

What the two documents and the three skills say today, verbatim:

```
cmd:      sed -n '34p;37,41p' AGENTS.md
observed: GITHUB_TOKEN= gh <args>              # every gh call — the credential trap below
          A stale `GITHUB_TOKEN` in the environment shadows the keyring credential `gh` would
          otherwise use, and surfaces as `HTTP 401: Bad credentials` on an unrelated command.
          `gh auth status` names which credential is in use. Clear it inline, as above —
          `env -u GITHUB_TOKEN gh …` is equivalent and is refused by default in an agent sandbox,
          which cannot verify what `env` does to the command it wraps.

cmd:      grep -rn "Commands block" .claude/skills/
observed: .claude/skills/modus-review/SKILL.md:31:Commands block, which states the trap, the
            working form, the diagnostic, and the other
          .claude/skills/modus-evidence/SKILL.md:76:`AGENTS.md`'s Commands block states which
            command shapes the sandbox refuses and what to
          .claude/skills/modus-work-package/SKILL.md:32:0. **Clear `GITHUB_TOKEN` on every `gh`
            call** — `AGENTS.md`'s Commands block states the

cmd:      grep -n "worktree-per-agent" documentation/80-agent-operating-procedure.md
observed: 8:  - doc:80-agent-operating-procedure#worktree-per-agent
          49:## Working tree <a id="worktree-per-agent"></a>
          65:| 0.3 | Run independent work concurrently. Two agents editing one tree is a merge
             conflict you scheduled; the isolation rule is `#worktree-per-agent` and it binds
             the agents you spawn, not only you. |

cmd:      grep -n "^## Step 0" documentation/80-agent-operating-procedure.md
observed: 56:## Step 0 — If you are the orchestrator <a id="orchestrating"></a>

cmd:      sed -n '67,68p' AGENTS.md
observed: 2. Branch from `main` (`feat|fix|docs|chore/…`), in a worktree of your own
             (`doc:80-agent-operating-procedure#worktree-per-agent`). No direct commits to `main`.
```

Line 49 against line 56 is criterion 4's whole point: the anchor owns a section that
**precedes** step 0, so an implementing agent told that step 0 is not its loop still reads
the rule. Rule 0.3 at line 65 cites the anchor rather than restating it.

The two `gh` forms, re-run in this worktree with the same stale `GITHUB_TOKEN` in the
environment that produced the original report:

Both transcripts complete, nothing elided:

```
cmd:      gh auth status
observed: github.com
            X Failed to log in to github.com using token (GITHUB_TOKEN)
            - Active account: true
            - The token in GITHUB_TOKEN is invalid.

            ✓ Logged in to github.com account m4xy (keyring)
            - Active account: false
            - Git operations protocol: ssh
            - Token: gho_************************************
            - Token scopes: 'gist', 'read:org', 'repo'
exit:     1

cmd:      GITHUB_TOKEN= gh auth status
observed: github.com
            ✓ Logged in to github.com account m4xy (keyring)
            - Active account: true
            - Git operations protocol: ssh
            - Token: gho_************************************
            - Token scopes: 'gist', 'read:org', 'repo'
exit:     0
```

`- Active account:` is the line that carries the whole finding, and it is why both
transcripts are quoted whole rather than trimmed to the first three lines: the keyring
credential is present and usable in **both** runs. Nothing is broken about the account. The
stale environment variable simply wins the selection, and clearing it inline moves
`Active account: true` from the invalid credential to the working one. `gh` masks the token
itself, so the complete output carries nothing secret.

The third form, `env -u GITHUB_TOKEN gh …`, was **not** re-run here and does not need to be:
`AGENTS.md` names it as the form to avoid, and the reason is that the sandbox refuses it —
so an agent following the document never reaches it. The run below, from the implementing
session, is the record that it is equivalent *to `gh`* and therefore refused for a reason
about the sandbox rather than about `gh`.

### Criterion 7 — the gate

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLint
          docs-lint: OK — 19 documents, 106 anchors, 914 references, 64 beans,
          28 graph edges, 19 selectable, 64 bean ids, 0 introduced, 64 on origin/main,
          0 closing transitions, 0 criteria checked, 0 unnumbered.
          > Task :qualityCheck
          BUILD SUCCESSFUL in 19s
          167 actionable tasks: 54 executed, 113 from cache
exit:     0
```

`0 closing transitions` is that run's honest report of itself: it was taken before this bean
was set `completed`, so check 14 had nothing to read. The same command with the four closures
in place:

```
cmd:      ./gradlew ktlintFormat && ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 2s
          57 actionable tasks: 57 up-to-date            (ktlintFormat; the tree is unchanged)
          > Task :docsLint
          docs-lint: OK — [... nine corpus counts, elided: they move with every edit to
          this change, including this transcript. Verbatim for an immutable tree in
          `bean:0055`, against the CI run of `b643f08` ...]
          4 closing transitions, 31 criteria checked, 0 unnumbered.
          > Task :qualityCheck
          BUILD SUCCESSFUL in 15s
          158 actionable tasks: 4 executed, 154 up-to-date
exit:     0
```

The elision is marked rather than silent, which is this bean's own promise and the rule
`bean:0091` now carries. What the line-budget row needs from that line is that `docsLint` ran
and reported nothing against check 8, and that is not in the elided part.

`wc -l` at the two commits, which is what criterion 6 reads:

```
cmd:      git show f39f100:AGENTS.md and the same for doc:80, each piped to wc -l
observed: 76 AGENTS.md
          456 documentation/80-agent-operating-procedure.md

cmd:      wc -l AGENTS.md documentation/80-agent-operating-procedure.md   (at 8181726)
observed:  82 AGENTS.md
          456 documentation/80-agent-operating-procedure.md
          538 total
```

The 120-line ceiling is check 8's own constant and the 500-line one is `max_lines` in the
documentation index; both documents are inside both, at the commit this merged on and at the
commit it is being closed on. The run that reads all four closures in full is the one in
`bean:0055`, which is the bean that owns check 14.

### Criterion 2 — the three forms, run against this repository during implementation

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
