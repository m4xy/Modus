---
name: modus-evidence
description: Prove a test, check or gate actually fires, by planting a violation, observing the named mechanism reject it, and reverting. Use before writing any "Enforced by:" line, before claiming a success criterion is met, and whenever adding a test, an ArchUnit rule, a Detekt rule, a docs-lint check or a CI step in the Modus repository.
---

# modus-evidence — plant, observe, revert

| field | value |
|---|---|
| `id` | `modus-evidence` |
| `version` | `0.1.0` |
| `owner` | domain `modus` · `@m4xy` |
| `status` | `draft` — no measured cost profile yet (`doc:70-skills` §3.7) |

## Trigger

Any claim that a mechanism enforces something. Writing an `Enforced by:` line, adding a
test, adding a rule to `architecture-tests`, adding a `docs-lint` check, adding a CI step,
or marking a success criterion met.

**Anti-trigger.** Not for proving *code* works — that is an ordinary test. This proves the
*checker* works. If you are not about to claim something is enforced, you do not need it.

## Preconditions

```
git status --porcelain          # must be empty before EVERY run; the revert destroys what it finds
./gradlew qualityCheck          # must be green BEFORE planting, or you cannot attribute the failure
```

**Empty before every invocation, not once before the first.** Step 5's revert is usually
`git checkout -- .beans`, which discards uncommitted edits to *tracked* files under that path
silently and at exit 0 — a new bean is untracked and survives, a bean you are closing,
amending or correcting does not. `doc:50-memory-and-evidence#corpus-figures`'s capture
procedure is run, paste, re-run, and the paste writes the figure back into a tracked bean, so
it re-dirties the tree *between* the runs and the second one arrives here with the
precondition unmet (`bean:0102`).

## Procedure

1. **`Pre:`** run the mechanism. It must pass. `deterministic`
   *Failure branch:* if it already fails, stop — you are debugging, not proving.
2. **Plant one violation of the specific behaviour the mechanism names.** Invert a
   condition, drop a guard, add the forbidden construct. Not a compile error, and not a
   different behaviour.
   *Failure branch:* if the plant does not compile, the mechanism was never going to see it.
3. **Run the mechanism alone**, by name — the single task or test, not the whole gate.
   Record the failure output **verbatim**.
   *Failure branch:* **the mechanism did not fail → you have found a real gap.** Do not
   adjust the plant until it fires. Demote the claim to an `Enforcement gap:` naming a bean
   (`doc:00-constitution#observed-failing`).
4. **Check the failure is the one the mechanism's name describes.** A test that fails with
   `NullPointerException` when its subject is broken has proved nothing
   (`doc:35-testing#load-bearing-evidence`).
5. **`Post:`** revert the plant. Re-run. It passes. `deterministic`
6. Write the `planted:` / `observed:` / `reverted:` block into the **bean**, beside the
   criterion (`adr:0005-evidence-lives-in-the-work-item`).

## The four traps this repository has actually fallen into

| trap | how it looked | how to avoid it |
|---|---|---|
| **The inert check** | `docs-lint` check 11 passed four plants because it compared the merge base to `HEAD`, and on a fresh branch those are equal | assert non-vacuity *in* the mechanism — fail if it scanned zero subjects |
| **Passing for the wrong reason** | a copy test on a one-element collection: `Collections.singleton` throws on mutation, so it passes without the fix | vary fixture size across 0, 1 and 2-or-more (`doc:35-testing#fixture-variation`) |
| **Firing for the wrong reason** | three `publishedLanguageIsLeaf` plants fired via `data class` synthetics, not the rule's own reason; the erased case passed | plant the *shape* the rule is about, not the convenient one |
| **`const val` inlining** | a planted violation at a constant leaves no trace in the referring class file | plant at call sites, never at a constant |

## Choosing what to plant

- **Plant something no other check also rejects**, or the observation is not isolated. A
  duplicate-bean-id plant on an id that is a `parent` fires check 12 alongside check 13
  (`bean:0051`). Pick a subject nothing else references: compare the ids in prose against
  the ids on disk and take one from the difference.
- **For a cross-branch check, use real history rather than forging a ref.** Branching from
  the commit before a bean was added makes that id free in the worktree and taken on
  `origin/main` — the exact shape the check is about, with no `git update-ref` that would
  mutate state shared with other worktrees.
- **Run the negative control too.** A check that fires on the plant and also fires on a
  legitimate change is not a gate, it is an obstacle. The counts in the success line are
  what prove the comparison ran rather than skipped.

## Sandbox

`AGENTS.md`'s Commands block states which command shapes the sandbox refuses and what to
write instead. Every one of them bites while planting and reverting a violation.

## Success criteria

- [ ] The mechanism was observed failing, and the output is recorded verbatim in the bean.
- [ ] The recorded failure is the assertion the mechanism's name describes.
- [ ] The tree is clean afterwards — `git status --porcelain` is empty. Empty proves the
      plant is gone and **not** that your own uncommitted work survived; the precondition
      above is what separates the two, and `## Validation` says why.
- [ ] Any mechanism that could not be made to fail is an `Enforcement gap:` naming a bean.

## Validation

```yaml
validation:
  argv: ["git", "status", "--porcelain"]
  cwd: "."
  timeoutSeconds: 30
  successExitCode: 0   # and empty output — an unreverted plant is a failed run
```

Empty output proves the plant is gone. It does **not** prove your own work survived: the
revert that removes the plant removes uncommitted tracked edits with it, and both outcomes
read as the same empty output. The safe case — your edits still present — is the one this
command fails. Commit before the run, so that the two are distinguishable (`bean:0102`).

## Context budget

Peak 15k, ceiling 30k. One mechanism, one plant, one revert. If it is costing more, the
mechanism is too coupled to test in isolation, which is itself the finding.

## Evidence produced

One `test-run` record per mechanism, attached to the criterion it satisfies.
