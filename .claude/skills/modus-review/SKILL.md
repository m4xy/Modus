---
name: modus-review
description: Adversarially review a Modus pull request — verify every claim by running it, attack the design, and report findings by severity. Use when asked to review a PR, check someone's work, or before merging a substantive change. Reviews design, correctness and evidence; never style.
---

# modus-review — verify by running, not by reading

| field | value |
|---|---|
| `id` | `modus-review` |
| `version` | `0.1.0` |
| `owner` | domain `modus` · `@m4xy` |
| `status` | `draft` — no measured cost profile yet (`doc:70-skills` §3.7) |

## Trigger

Reviewing a Modus pull request, or checking work before merging it.

**Anti-trigger.** Implementing → `modus-work-package`. Proving one mechanism → `modus-evidence`.
**Never** review style: that is the build's job, and a style comment in review is a defect in
the toolchain, not in the diff (`doc:00-constitution` §7.4).

## Preconditions

```
GITHUB_TOKEN= gh pr view <n> --json body,title          # the body is your primary context
git fetch origin && git checkout <branch>
```
Run in an isolated worktree when reviewing in parallel with other work.

> **In an isolated worktree the prefix form is refused.** The sandbox rejects
> `env -u GITHUB_TOKEN gh …` — it cannot verify what `env` does to the command it wraps —
> and also rejects compound commands containing a redirect into a non-literal target. Write
> a script into the scratchpad that does `cd <worktree>; unset GITHUB_TOKEN; gh …` and run
> `bash <path>`. Found by `bean:0035`'s implementation, which lost two attempts to it.

## Procedure

1. **Read the PR body, then the bean it names, whole, then only the documents in its
   `refs:`.** Nothing else (`AGENTS.md` routing). The context budget is a rule, not advice.

2. **Reproduce the evidence yourself.** The body claims plants were observed failing. Plant
   them again, run the named task, compare the output to what is quoted.
   *Failure branch:* **a claim you cannot reproduce is blocking**, regardless of how
   plausible it reads.

3. **Run the mechanism through the full gate, not just its own task.** A check can fail
   when invoked directly and never be reached by `qualityCheck`.

4. **Attack the design, in this order.** Findings come from the third row far more than the
   first:
   - **What the change claims** — is the mechanism the one that fires, or does it fire
     incidentally? (Three plants once fired via `data class` synthetics, not the rule.)
   - **What the change does not say** — a gate that runs, but on an empty set. A rule scoped
     by name where the language generates names the source never wrote.
   - **What is absent.** Every mutation targets a line that *exists*; both blocking defects
     in `bean:0030` were **missing guards**, and a 10/10 kill rate could not touch them.

5. **Hunt this repository's known defect classes**, by name:
   - exposed mutable state — a `public val` collection is a live alias; Kotlin's `Set` is a
     read-only *view*, not an immutable type. Shipped twice, in two contexts.
   - fixture uniformity — size-one collections degenerate (`Collections.singleton` throws on
     mutation), so a test can pass while proving nothing.
   - erasure — a `@JvmInline value class` leaves no bytecode edge.
   - stale `Enforced by:` — verify it names something that exists *and* fires.

6. **Check the documents the change touched are still internally consistent.** A rewritten
   section frequently leaves a sentence referring to a paragraph that no longer exists.

7. **Report by severity** — blocking / should-fix / nit — each with `file:line`, a concrete
   failure scenario (inputs → wrong outcome), and the output you observed. State explicitly
   which plants you reproduced. Fix nothing; merge nothing.

## Success criteria

- [ ] Every evidence claim in the PR body was independently reproduced, or reported as
      unreproducible — evidence kind `test-run`
- [ ] The full gate was run, not only the changed mechanism — `test-run`
- [ ] At least one genuine attempt to break the design is recorded, including the attempts
      that failed — `citation`
- [ ] The worktree is clean; every plant reverted — `command`
- [ ] No style finding appears in the report

## Validation

```yaml
validation:
  argv: ["git", "status", "--porcelain"]
  cwd: "."
  timeoutSeconds: 30
  successExitCode: 0   # and empty — a reviewer who leaves a plant behind has broken the branch
```

## Context budget

Peak 120k, ceiling 200k. The routing rule is what keeps it there: the PR body, one bean,
and the documents in `refs:`.

## Evidence produced

`test-run` per reproduced claim; `citation` per finding; an explicit statement of what was
attacked and survived, which is what makes "nothing blocking" mean something.
