---
# modus-0062
title: docs-lint does not scan .claude/, so skills drift silently
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# docs-lint does not scan `.claude/`, so skills drift silently

`doc:05-authoring-for-agents#one-fact-one-place` is a MUST: a fact has exactly one owning
anchor and every other mention is a reference. `tools/docs-lint.sh` enforces the rules that
are mechanically decidable — over `documentation/`, `.beans/`, `AGENTS.md`, `CLAUDE.md` and
the pull-request template. `.claude/` is in none of those sets, and the skills under it are
the highest-traffic instructions in the repository: every agent that runs a work package or a
review reads one.

## Observed

```
cmd:      grep -n '\.claude\|FM_FILES=\|REF_FILES=' tools/docs-lint.sh
observed: 30: FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
          31: REF_FILES="$FM_FILES $(ls .beans/*.md 2>/dev/null) AGENTS.md CLAUDE.md
                          .github/pull_request_template.md"
exit:     0
```

No occurrence of `.claude` anywhere in the file. Checks 1–10 read `FM_FILES` or `REF_FILES`;
checks 11–14 read `.beans/`. Nothing reads a skill.

## What drifted while nothing was looking

The fix is `fix(skills): correct the workarounds that drifted from the merged conventions`,
raised as a separate change. This bean is the observation it produced.

| fact | copies | remedies |
|---|---|---|
| the `GITHUB_TOKEN` credential trap | `AGENTS.md`'s Commands block, `.claude/skills/modus-work-package/SKILL.md`, `.claude/skills/modus-review/SKILL.md`, `.claude/skills/modus-evidence/SKILL.md` | **two, disagreeing**: `AGENTS.md` and `modus-evidence` said clear it inline (`GITHUB_TOKEN= gh`); `modus-work-package` and `modus-review` said write a scratchpad script that runs `unset GITHUB_TOKEN` |
| every agent works in a worktree of its own | `doc:80-agent-operating-procedure#worktree-per-agent`, `.claude/skills/modus-review/SKILL.md`, `.claude/skills/modus-work-package/SKILL.md` | the anchor is **unconditional**; `modus-review` restated it as "when reviewing in parallel with other work", and `modus-work-package` step 4 restated the branch rule while dropping the worktree clause entirely |

**Four** copies of one fact with two different remedies, and an unconditional rule restated
once as conditional and once with its condition dropped. Both are
`doc:05-authoring-for-agents#one-fact-one-place` violations of a kind checks 6, 9 and 10
already decide for documents, and none of it was visible to any check. The gate was green
before the fix and green after it — over a file set that does not include the changed files.

The fourth copy is the sharpest part of the finding, because the first pass at the fix
**missed it and then asserted it was not there**: that pull request's `out_of_scope` claimed
`modus-evidence` "carries neither copy", in a change whose whole subject is removing duplicate
copies. A second reviewer found it. `modus-evidence:76-78` also carried two facts that lived
nowhere else — the sandbox's refusal of a piping `for … do … done` loop, and `mv`, run, `mv`
back needing three separate calls — so deleting the paragraph would have destroyed them. It
moved to `AGENTS.md`'s Commands block instead, and all three skills now point there.

The pull request that fixed the drift is itself the measurement: the drift was found by a
human reviewer reading two skills side by side, which is the failure mode
`doc:00-constitution#mechanical-enforcement` exists to remove.

## The tension this bean must resolve

Pointing `docs-lint` at `.claude/` is the obvious move and is probably wrong.

- A skill is not a document. Its front-matter is `name` + `description` — the schema
  Claude Code reads — not `id`/`status`/`read_when`/`provides`/`depends_on`. Check 2 would
  reject every skill in the tree on its first run, and check 1 before it.
- Skills carry a second, overlapping contract in `doc:70-skills` (`version`, `owner`,
  `status`, a validation block), which is a different set of required fields again.
- The `provides`/anchor model does not apply: a skill owns no anchors and should own none —
  it is a consumer of documented facts, which is the whole point.

So the enforceable subset is narrower than "lint the skills". Candidates, none chosen here:

| option | what it would catch | cost |
|---|---|---|
| add `.claude/skills/**/SKILL.md` to `REF_FILES` only — check 6 (references resolve) and check 10 (no bare bean paths), not checks 1, 2, 5, 8 | a skill citing a `doc:`/`bean:`/`adr:` that no longer exists, and bare `.beans/NNNN` paths | small; needs check 6's file loop to tolerate a file with no front-matter. **It would find nothing today** — below |
| a check that no line under `.claude/` restates a rule an anchor owns | the drift actually found | free-text comparison; `doc:05` §3's own rejected alternative in `adr:0005` |
| a check that a skill's normative statements are references — every imperative in a skill cites an anchor or is skill-local procedure | the conditional restatement of `#worktree-per-agent` | needs a definition of "normative statement" that a program can decide |
| a skill-specific front-matter check against `doc:70-skills`' contract, separate from checks 1 and 2 | a malformed or unversioned skill | a second schema to maintain |
| leave it; skills are reviewed by the humans and agents who read them | nothing | the status quo, which produced the drift above |

**The narrowest option would find nothing today, and that is an argument about it rather than
against it.** Every typed reference in the skills resolves:

```
cmd:      resolve each doc:/bean:/adr:/rule: reference in .claude/skills/*/SKILL.md the way
          check 6 does — prefix-glob the kind's directory, then require any `#anchor` to be
          declared by an `<a id>` in the matched file
observed: typed refs in .claude/skills/**: 21   distinct: 13
          unresolved: []
          anchor missing: []
exit:     0
```

Measured at 21 on `origin/main` and at 21 again after the skills fix; a reviewer's count of 23
is two high. No skill carries a `rule:` reference at all. So check 6 over the skills is a
**regression guard**, not a repair: it goes green on day one and catches the next `bean:NNNN`
that a renamed or split bean invalidates. Whether a guard that finds nothing today earns its
wiring is the decision this bean owes, and the count is what makes it a decision rather than
a guess.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Whether `.claude/` comes under a mechanical check is decided, with the option chosen and the rejected ones named | citation |
| 2 | If a check is added, it is observed rejecting a planted violation of exactly the drift found here — a skill citing a dead reference, or restating a fact an anchor owns | test-run per condition |
| 3 | If no check is added, the gap carries an `Enforcement gap:` line naming this bean, per `documentation/README.md` | citation |
| 4 | Adding `.claude/` to any file set does not fail the three skills already in the tree | test-run |
| 5 | `./gradlew qualityCheck` green | test-run |

Criteria 1, 2 and 3 are each satisfiable by acting **or** by recording the refusal as an
`Enforcement gap:`. That is deliberate for a decision bean rather than a loose predicate: what
this bean owes is a decision on the record, and `documentation/README.md` already makes an
admitted gap a first-class result. Silence satisfies none of the three.

## Not in scope

- The drift itself. Fixed in the skills change above; this bean is the missing gate, not the
  two edits.
- `.claude/settings.json`, hooks, and anything under `.claude/` that is configuration rather
  than instructions to an agent.
- `doc:70-skills`' own contract. It is a document and is already checked as one.
