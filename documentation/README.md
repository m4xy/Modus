# Modus Documentation Package

This directory is the **constitution** of the Modus repository. Every agent and every
human contributor reads it before touching code. It is written for an agent audience
first and rendered for humans second — the same principle Modus applies to its own
product surface.

If a rule here conflicts with a comment, a Slack message, a review remark, or your
own prior belief, **this package wins**. If this package is wrong, fix this package in
the same pull request that fixes the behaviour.

---

## Reading order

Read in the order below on your first pass. On subsequent tasks, read only what the
"Read this when" column tells you to.

| # | File | Read this when |
|---|------|----------------|
| — | [`README.md`](README.md) | You are new to the repository. Start here. |
| 00 | [`00-constitution.md`](00-constitution.md) | **Always.** Non-negotiable rules. ~10 min. |
| 10 | [`10-architecture.md`](10-architecture.md) | You are adding a module, a class, or an endpoint. |
| 20 | [`20-ddd-practices.md`](20-ddd-practices.md) | You are writing anything in `core/`. |
| 30 | [`30-code-style.md`](30-code-style.md) | You are writing Kotlin or TypeScript, or a build check fails. |
| 40 | [`40-durability.md`](40-durability.md) | You are touching persistence, file IO, or locking. |
| 50 | [`50-memory-and-evidence.md`](50-memory-and-evidence.md) | You are recording a conclusion, or reading one. |
| 60 | [`60-cost-model.md`](60-cost-model.md) | You are choosing a model, an effort level, or reporting spend. |
| 70 | [`70-skills.md`](70-skills.md) | You have done a task more than once. |
| 80 | [`80-agent-operating-procedure.md`](80-agent-operating-procedure.md) | **Always, if you are an agent.** This is your loop. |
| 90 | `90-work-items.md` | *Owned separately.* The on-disk work-item ("bean") spec. Not yet ratified. |
| — | [`adr/`](adr/) | You want to know *why* a decision was made, or you are making a new one. |

### The minimum an agent must read

`00-constitution.md` + `80-agent-operating-procedure.md`. Everything else is
read-on-demand, driven by the table above. Reading the whole package on every task
is a context-budget violation (see `00-constitution.md` §6).

---

## What is owned where

| Concern | Owner | Location |
|---|---|---|
| Methodology, rules, conventions | This package | `documentation/` |
| Architecture decisions with alternatives considered | ADR log | `documentation/adr/` |
| Work-item on-disk schema | Separate work package | `documentation/90-work-items.md` |
| Actual work items | Beans | `beans/` |
| Build, style and arch enforcement | Convention plugins | `build-logic/` |
| Skills | Skill registry | `.claude/skills/` and per-domain skill stores |

This package does **not** contain build files, source code, or CI configuration.
Documentation changes that require a build change must reference the follow-up work
item that makes the enforcement real.

---

## The encoding rule

> **Every learning is encoded back into this package.**

This is the single most important operating rule in Modus, and it is not optional.

1. If you discovered something non-obvious about this repository — a constraint, a
   gotcha, a naming convention, a tool invocation that works — **you write it down
   here in the same pull request**.
2. If you were corrected in review, the correction is encoded here so the next agent
   is never corrected for the same thing. A review comment that does not result in
   either a code change *plus* a rule, or a tool change, is a leak.
3. If a rule was enforced only by a human reading a diff, that is a bug. Open a work
   item to move the rule into ktlint, Detekt, ArchUnit, or a Playwright assertion.
   See `30-code-style.md`.
4. If the learning is durable and domain-specific rather than repository-wide, it is a
   **memory**, not a document — record it under `50-memory-and-evidence.md` with
   evidence attached.

Distinguishing the two: documentation states *how this repository works*; memory
states *what we found out while working on a specific domain, epic, or story*.
Documentation is global and versioned with the code. Memory is domain-scoped and
carries evidence.

---

## How to change this package

- Documentation changes go through the same branch → work item → PR → review → merge
  workflow as code. There are no direct commits to `main`. See `00-constitution.md` §7.
- A change that removes or weakens a rule requires an ADR. A change that adds or
  clarifies a rule does not.
- Keep each file in the 250–500 line range. If a file outgrows that, it is two files,
  or it contains material that belongs in an ADR or in a skill.
- Prefer tables and imperatives. Avoid aspiration: "the code should be clean" is not a
  rule. "Cyclomatic complexity above 10 fails the Detekt build" is a rule.

---

## Conventions used in this package

- **MUST / MUST NOT** — mechanically enforced, or slated for enforcement. Violating it
  fails a build, a check, or review.
- **SHOULD / SHOULD NOT** — strong default. Deviating requires a note in the pull
  request body explaining why.
- **MAY** — genuinely at your discretion.
- `Enforced by:` — names the tool that catches the violation. If a MUST has no
  `Enforced by:` line, it carries a `Enforcement gap:` line naming the work item that
  will close it.
