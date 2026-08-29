---
name: modus-work-package
description: Take one Modus work item from selection to a merge-ready pull request — pick the bean, branch, implement, prove every check fires, record evidence in the bean, and open the PR. Use when asked to work the next bean, implement a specific bean, or continue Modus development. Not for reviewing someone else's PR.
---

# modus-work-package — one bean, branch to pull request

| field | value |
|---|---|
| `id` | `modus-work-package` |
| `version` | `0.1.0` |
| `owner` | domain `modus` · `@m4xy` |
| `status` | `draft` — no measured cost profile yet (`doc:70-skills` §3.7) |

## Trigger

"Work the next bean", "implement `bean:NNNN`", "keep going" on Modus development.

**Anti-trigger.** Reviewing a pull request → `modus-review`. Proving one mechanism fires →
`modus-evidence`. A question answerable by reading → just read; this skill opens a branch.

## Preconditions

```
git status --porcelain                  # empty
gh auth status                          # see step 0 — the token trap
./gradlew qualityCheck                  # green on main before you start
```

## Procedure

0. **Clear `GITHUB_TOKEN` on every `gh` call** — `AGENTS.md`'s Commands block states the
   trap, the working form and the diagnostic. `deterministic`

> **In an isolated worktree the prefix form is refused.** The sandbox rejects
> `env -u GITHUB_TOKEN gh …` — it cannot verify what `env` does to the command it wraps —
> and also rejects compound commands containing a redirect into a non-literal target. Write
> a script into the scratchpad that does `cd <worktree>; unset GITHUB_TOKEN; gh …` and run
> `bash <path>`. Found by `bean:0035`'s implementation, which lost two attempts to it.

1. **Select the bean.** `AGENTS.md` workflow step 1 is normative: skip `type: epic`; among
   `status: todo` beans whose every `blocked_by` is `completed`, highest `priority`, ties on
   `order` ascending, absent `order` last. Derive it, do not guess — following your own plan
   instead of the rule is how the wrong bean gets worked. `deterministic`

2. **Read exactly:** `AGENTS.md` whole, the bean whole (it is the only thing you read
   whole), `doc:00-constitution` and `doc:80-agent-operating-procedure` (both
   `read_when: always`), then only the documents the bean's own references name.
   *Failure branch:* past 40k tokens here, you read too much — restart (`doc:80` step 1).

3. **Restate the criteria in the bean before writing code** (`doc:80` step 2). If a
   criterion is not checkable, sharpen it now. If the bean will not fit 300k tokens, split
   it into children under an epic — that is normal, not exceptional
   (`doc:00-constitution` §6.2).

4. **Branch** `<kind>/<slug>`, `<kind>` ∈ feat|fix|docs|chore|refactor|test|perf|build.

5. **Implement the smallest correct change.** Domain first, then use cases, then adapters.
   No drive-by fixes — note them as beans.

6. **Prove every check fires** → `modus-evidence`, once per mechanism. Non-negotiable: this
   repository has shipped an inert check, a check that fired for the wrong reason, and a
   test that passed for the wrong reason.

7. **Record the evidence in the bean**, beside each criterion
   (`adr:0005-evidence-lives-in-the-work-item`). A `completed` bean is final afterwards —
   corrections are `## Amendments` entries, enforced by `docs-lint` check 11.

8. **Run the gate:** `./gradlew ktlintFormat`, then `./gradlew qualityCheck`, then
   `./gradlew e2eTest` if `backoffice/` or `e2e/` changed. `deterministic`
   *Failure branch:* ktlint rejects a KDoc inside a `.kts` block — use `//` there.

9. **Commit.** Conventional commit; the body says *why*, not what. Trailers:
   `Co-Authored-By:` naming the model, and `Claude-Session:`.

10. **Open the pull request** with `.github/pull_request_template.md`. The `verify` block
    names the bean; it does not restate the evidence. `review_focus` must contain real
    questions — it is what a reviewer answers.

11. **After merge, close the bean** in a separate change: `status: completed`, `updated_at`
    = the merge timestamp, and a `## Summary of Changes`. A bean cannot close itself inside
    its own pull request, so this is always the next session's first act.

## Traps this repository has actually sprung

| trap | consequence |
|---|---|
| `gh pr merge --delete-branch` on a PR that is the *base* of another | GitHub **closes** the child; it cannot be reopened without its base, nor retargeted (`bean:0037`) |
| Trusting an `Enforced by:` line | 27 of 34 were false when audited (`bean:0010`); verify before relying on one |
| Assuming CI is a merge gate | the ruleset has no `required_status_checks`; red CI has never blocked a merge (`bean:0047`) |
| Writing evidence in the PR body | it belongs in the bean; two copies drift and did (`adr:0005`) |

## Success criteria

- [ ] `./gradlew qualityCheck` exits 0 — evidence kind `test-run`
- [ ] Every mechanism claimed as `Enforced by:` was observed failing — `test-run` per mechanism
- [ ] Every criterion in the bean has evidence beside it — `citation` or `test-run`
- [ ] The PR body names the bean and restates no evidence — `citation`
- [ ] `bash tools/docs-lint.sh` exits 0 — `command`

## Validation

```yaml
validation:
  argv: ["./gradlew", "qualityCheck", "--console=plain"]
  cwd: "."
  timeoutSeconds: 900
  successExitCode: 0
```

## Context budget

Peak 200k, ceiling 300k (`doc:00-constitution` §6). Checkpoint at 100k and 200k. At 200k
with work remaining, hand off: commit what is green, update the bean, open a draft PR.

## Evidence produced

`test-run` per mechanism and for the gate; `citation` for placement and contract claims;
`diff` for the merge commit at closure.
