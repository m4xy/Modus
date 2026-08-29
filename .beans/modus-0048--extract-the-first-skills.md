---
# modus-0048
title: Extract the first skills, and correct the path that would have made them inert
status: completed
type: feature
priority: high
order: AA
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
---

# Extract the first skills, and correct the path that would have made them inert

`doc:70-skills` has specified skills since the first merge and **nothing has ever been
extracted**. `doc:00-constitution` §5 says the third time you do something you extract a
skill; `doc:README#ownership` names `.claude/skills/` as the registry. It is empty.

The cost of that is not theoretical. Every agent this repository has run — including five
reviewers and three implementers in one session — has re-derived the same procedure from
`doc:80` and the same traps from scar tissue, and each one paid for it again.

## What is extracted, and why these three

`doc:70-skills#celebrity-skills` says prefer a small number of heavily reused skills over a
long tail. Three, each crossing the §2.1 threshold several times over in one session:

| skill | times performed | why it is not one skill with the others |
|---|---|---|
| `modus-evidence` | ~30 | used *inside* the other two; inlining it would be one fact in three places |
| `modus-work-package` | ~12 | the bean-to-pull-request loop |
| `modus-review` | 5 | a different actor with a different context budget and an explicit ban on style findings |

Each carries what `doc:70-skills` §3 requires — identity, trigger **and anti-trigger**,
command-checkable preconditions, a numbered procedure with a failure branch per step,
binary success criteria naming evidence kinds, a single-command validation whose exit code
is the verdict, a context budget, and the evidence it produces.

Each also carries a **traps** table naming defects this repository actually shipped, with
the bean that found them. That is the part `doc:80` cannot hold: the SOP is the general
loop, and these are the specific ways this codebase bites.

## The path in `doc:70-skills` §3 would have produced inert files

`doc:70-skills` §3 states skills live at `.claude/skills/<skillId>.md`. The harness that
runs agents here loads a skill only from `.claude/skills/<name>/SKILL.md`, with YAML
front-matter carrying `name` and `description` — a flat `<skillId>.md` is never discovered
and never offered to an agent.

So the documented path yields a document that reads like a skill, is registered nowhere,
and is invoked by nobody. Written to spec, these three would have been three more files
that no agent ever opens — which is precisely the failure `doc:00-constitution#observed-failing`
describes one level up: a mechanism that cannot fire.

§3 is corrected to the loadable path, with the reason, so the next skill is not written to
the inert one.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Three skills exist at the path the harness loads, each with the front-matter it requires | `ls .claude/skills/*/SKILL.md` |
| 2 | Each carries every section `doc:70-skills` §3 makes mandatory | `citation`, per skill |
| 3 | Each states an **anti-trigger**; §3.2 says a skill without one gets misapplied | `citation` |
| 4 | Every procedure step that can fail carries its failure branch; §3.4 calls a happy-path-only procedure a wish | `citation` |
| 5 | Each validation is one command whose exit code is the verdict, per §3.6.1–3.6.3 | `citation` |
| 6 | All three are `status: draft`, not `active` — §3.7 makes a measured cost profile the condition for `active`, and no cost measurement exists in this repository (`module-cost` is a stub) | `citation` |
| 7 | `doc:70-skills` §3 names the path the harness actually loads | `diff` |
| 8 | `./gradlew qualityCheck` green | `test-run` |

## Deliberately not done

**No cost profile.** §3.7 requires a measured grid for `active`, and measuring it needs
`module-cost`, which is an empty placeholder. Estimating one and labelling it measured would
be exactly the class of claim this repository keeps finding and deleting. They stay `draft`
until `bean:0016` can measure them.
