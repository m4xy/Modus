---
# modus-0051
title: Two parallel agents allocated the same bean id
status: completed
type: fix
priority: high
order: AL
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
---

# Two parallel agents allocated the same bean id

Three agents worked three beans concurrently in isolated worktrees, each branched from the
same `main`. Two of them needed to raise a follow-up, both took the next free id by reading
`.beans/`, and both got **0048**:

| bean:0048 | raised by | subject |
|---|---|---|
| `extract-the-first-skills` | the orchestrator | the first three skills |
| `adr-immutability-versus-deferred-detail` | `bean:0041`'s implementer | `adr:0001` §3 versus a deferred ADR row |

Neither was wrong. `.beans/` is the allocator, it is read at branch time, and nothing
serialises two readers. The collision surfaced only when the second pull request failed to
merge — `docs-lint` was green on both branches, because on each branch the id *is* unique.
Check 12 validates the graph within one tree and cannot see a sibling branch.

This is the first defect that is a property of **parallel execution** rather than of any
change, and it will recur every time two agents raise a bean at once — which is the normal
case once Modus schedules its own work (`doc:00-constitution` §12).

## Success criteria

- A collision is refused rather than discovered at merge. The check runs where both sides
  are visible — a `main`-side hook, a CI job comparing against the base, or an allocation
  that cannot collide by construction.
- Decide between **detect** and **prevent**, and record why:
  - *detect*: `docs-lint` compares ids against the merge base and fails when a bean id it
    introduces already exists on `origin/main`. Cheap, catches it at push, still lets two
    open branches collide until one merges.
  - *prevent*: ids stop being sequential. `hmans/beans` upstream generates a short nanoid
    (`pkg/bean/id.go`, `NewID`) using `beans.id_length` for exactly this reason, and
    `bean:0008` recorded that Modus chose numeric ids deliberately — this is the cost of
    that choice arriving. Revisit it with the evidence now available rather than re-deciding
    from taste.
- Whichever is chosen, observed rejecting a planted collision before it is claimed
  (`doc:00-constitution#observed-failing`).
- The orchestration guidance says how an agent should allocate an id when it cannot see its
  siblings — currently nothing does, and "read `.beans/` and add one" is what failed.

## Restated criteria

1. **A collision is refused rather than discovered at merge.** `docs-lint` check 13,
   condition (c), fails when a bean id this branch introduces already exists on
   `origin/main`. Evidence kind: `test-run` — the check observed rejecting a planted
   collision, and observed accepting a free id (negative control).
2. **Within-tree uniqueness.** Condition (a): an id names exactly one file. Evidence kind:
   `test-run` on an *unreferenced* duplicate — the case check 6 cannot see.
3. **Filename and marker agree.** Condition (b). Evidence kind: `test-run`.
4. **Non-vacuity.** A run that parsed no beans, or fewer beans than are on disk, fails
   rather than exiting 0; the `OK` line carries the counts. Evidence kind: `test-run`.
5. **Detect versus prevent decided and recorded**, as a recommendation for a human, against
   `bean:0008`'s reasoning rather than from taste.
6. **The allocation rule is written down** where an agent creating a bean will select it.

Out of scope: renumbering any bean; changing the id scheme; a `main`-side hook or a CI job
separate from `docsLint`; making two *unmerged* branches visible to each other.

## Decision — detect, not prevent

**Implemented: detect.** `bean:0008` §"Decision — keep numeric ids" chose numeric ids on two
grounds, both of which still hold and neither of which the new evidence touches:

- Upstream does not require its own id shape on import. `ParseFilename` splits the filename
  on `--` and takes whatever text precedes it, with no length or charset check; `Core.Load`
  (`pkg/beancore/core.go:203`) takes that id verbatim. `modus-0049` is exactly as valid an
  id to the `beans` CLI/TUI/GraphQL tooling as `jwy7`. So sequential ids cost no upstream
  compatibility.
- Every merged PR body, every `bean:NNNN` reference across `documentation/`, and check 6's
  reference regex (`bean:[0-9]{4}`) are built on the numeric sequence. Switching to nanoids
  breaks all of them for no functional gain.

What the new evidence adds is a **price tag**, not a counter-argument: one collision
(`modus-0048`), caught at merge, costing one rebase and one renumber. Prevention would have
avoided that at the cost of every existing citation. Detection removes the same failure at
push time for the cost of one `git ls-tree`.

**Recommendation for a human.** Keep numeric ids. Revisit only if the residual bites: check
13 cannot see a *second unmerged* branch, so two open branches still collide until one
merges — the collision is then refused on the second branch's next push rather than
discovered in a conflicted merge, which is strictly better but not zero. The signal to
revisit is a **second** collision that check 13 refuses; one refusal is the check working,
two is the allocator being wrong. If it comes, the cheap middle option is not nanoids but a
reserved band per concurrent agent, which keeps every existing citation resolvable.

## Decision — where the allocation rule lives

`doc:05-authoring-for-agents#checks` §6, in the prose under the checks table, beside the
check that enforces it. Not `AGENTS.md`: that file states no rule of its own, and a copy
there is the drift `doc:05-authoring-for-agents#one-fact-one-place` forbids. An agent
creating a bean writes under `.beans/`, and `doc:05`'s `read_when` already carries
`path: .beans/**`, so the selection algorithm (§1.1) puts the rule in front of exactly the
agent who needs it, with no routing row to maintain.

## Evidence

Mechanism: `bash tools/docs-lint.sh`, check 13. Procedure: `modus-evidence` skill —
plant, observe, revert. Tree clean before each plant.

### Pre

```
$ bash tools/docs-lint.sh
docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
$ /bin/bash --version | head -1
GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)
$ /bin/bash tools/docs-lint.sh
docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
```

### Criterion 2 — within the tree, an id names exactly one file

```
planted:  .beans/modus-0049--planted-duplicate.md, a second file carrying id modus-0049.
          modus-0049 is referenced by no prose and named by no parent/blocked_by edge, so
          neither check 6 nor check 12 can see it — the exact shape that passes today.
observed: $ bash tools/docs-lint.sh; echo "exit=$?"
          FAIL check 13 bean id 'modus-0049' names more than one file: .beans/modus-0049--bash-32-claim-is-unenforced.md .beans/modus-0049--planted-duplicate.md
          docs-lint: 1 failure(s).
          exit=1
reverted: $ rm .beans/modus-0049--planted-duplicate.md
          $ bash tools/docs-lint.sh
          docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
```

Negative control on the same plant, with a *referenced* id: the first attempt used
`modus-0040`, which three beans name as `parent`. Check 12 fired three times alongside
check 13 — confirming that today's coverage of a duplicate id is incidental to it being
referenced, which is why check 13 plants an unreferenced one:

```
FAIL check 12 .beans/modus-0041--split-architecture-document.md: parent 'modus-0040' resolves to 2 bean files, expected exactly 1
FAIL check 12 .beans/modus-0042--tier-front-matter.md: parent 'modus-0040' resolves to 2 bean files, expected exactly 1
FAIL check 12 .beans/modus-0043--docs-lint-behind-the-module-boundary.md: parent 'modus-0040' resolves to 2 bean files, expected exactly 1
FAIL check 13 bean id 'modus-0040' names more than one file: .beans/modus-0040--planted-duplicate.md .beans/modus-0040--tier-one-surface.md
```

### Criterion 3 — filename and marker agree

```
planted:  .beans/modus-0049--bash-32-claim-is-unenforced.md line 2, `# modus-0049` ->
          `# modus-0048` — a rename that updated one side and not the other. Upstream never
          reads the marker back (`Parse` ignores it; the id comes from the filename,
          bean:0008), so nothing else in the toolchain can see this.
observed: $ bash tools/docs-lint.sh; echo "exit=$?"
          FAIL check 13 .beans/modus-0049--bash-32-claim-is-unenforced.md: front-matter marker '# modus-0048' does not match the filename id 'modus-0049'
          docs-lint: 1 failure(s).
          exit=1
reverted: $ git checkout -- .beans/modus-0049--bash-32-claim-is-unenforced.md
          $ bash tools/docs-lint.sh
          docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
```

An unplanned first observation, on the real tree, before any plant: requiring the marker to
be the only `#` line in the front-matter rejected `.beans/modus-0047--require-the-gate-check.md`,
which carries a second free-form comment. The condition was narrowed to comment lines shaped
like an id marker (`# modus-…`) — recorded because it is the reason the check is not simply
"line 2":

```
FAIL check 13 .beans/modus-0047--require-the-gate-check.md: front-matter marker '# modus-0047
# Blocked on a human: modifying branch protection is refused to the agent by the harness.' does not match the filename id 'modus-0047'
```

### Criterion 1 — across branches, against `origin/main`

The plant is the real scenario, not a simulation of it: a branch cut *before*
`b217c0ee` ("docs: encode the learnings bean:0035's implementation returned (#28)"), which
is the commit that introduced `modus-0049` on `main`. On that branch `modus-0049` is free in
the worktree and taken on `origin/main` — identical in every respect to the `modus-0048`
collision this bean records.

```
planted:  $ git checkout -b sim-parallel-allocation b217c0ee9413f730f541fe9ef0112fe633368d23^
          $ git checkout fix/bean-id-uniqueness -- tools/docs-lint.sh
          $ ls .beans/ | grep -c 0049
          0
          then wrote .beans/modus-0049--planted-parallel-allocation.md
observed: $ git merge-base origin/main HEAD
          634702235fd889acb50fb8ae87907fc04eb97534
          $ bash tools/docs-lint.sh; echo "exit=$?"
          FAIL check 13 bean id 'modus-0049' is introduced by this branch (.beans/modus-0049--planted-parallel-allocation.md) but already exists on origin/main (.beans/modus-0049--bash-32-claim-is-unenforced.md); a sibling branch allocated it first — take the next id free on origin/main, not the next free in this worktree (bean:0051)
          docs-lint: 1 failure(s).
          exit=1
reverted: $ rm .beans/modus-0049--planted-parallel-allocation.md
          $ bash tools/docs-lint.sh
          docs-lint: OK — 18 documents, 96 anchors, 696 references, 48 beans, 25 graph edges, 13 selectable, 48 bean ids, 0 introduced, 51 on origin/main.
```

**Negative control — a free id must be accepted, not merely counted.** On the same older
base, a new bean whose id no sibling holds. The `1 introduced` count is what proves the
comparison ran on it rather than skipping it, which is the failure mode check 11 shipped
with:

```
$ # wrote .beans/modus-0060--negative-control-free-id.md on sim-parallel-allocation
$ bash tools/docs-lint.sh; echo "exit=$?"
docs-lint: OK — 18 documents, 96 anchors, 696 references, 49 beans, 25 graph edges, 14 selectable, 49 bean ids, 1 introduced, 51 on origin/main.
exit=0
```

### Criterion 4 — non-vacuity

Two plants. First, a bean file the front-matter parser cannot read — invisible to check 12's
TSV and therefore to conditions (a) and (c), and previously invisible to the counts:

```
planted:  .beans/modus-0061--unparsed-bean.md, no front-matter block
observed: $ bash tools/docs-lint.sh; echo "exit=$?"
          FAIL check 13 .beans/modus-0061--unparsed-bean.md: front-matter carries 0 '# modus-…' id markers, expected exactly 1
          FAIL check 13 50 bean file(s) on disk but 49 parsed; a bean with no front-matter block is invisible to checks 12 and 13
          docs-lint: 2 failure(s).
          exit=1
reverted: $ rm .beans/modus-0061--unparsed-bean.md
```

Second, the degenerate case: no beans at all. Output filtered to check 13's lines and the
count, because the same condition also breaks 76 `bean:` references (check 6) and check 12's
selectable assertion:

```
planted:  $ mv .beans .beans-hidden
observed: $ bash tools/docs-lint.sh | grep -E "check 13|failure\(s\)"
          FAIL check 13 .beans/ holds no bean files; checks 12 and 13 examined nothing
          docs-lint: 76 failure(s).
reverted: $ mv .beans-hidden .beans
```

### Post

```
$ git checkout -f fix/bean-id-uniqueness
$ git branch -D sim-parallel-allocation
Deleted branch sim-parallel-allocation (was 6347022).
$ git status --porcelain
$ bash tools/docs-lint.sh
docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
```

### The gate

```
$ ./gradlew ktlintFormat
BUILD SUCCESSFUL in 3s
$ ./gradlew qualityCheck
> Task :docsLint
docs-lint: OK — 19 documents, 101 anchors, 768 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.

> Task :qualityCheck

BUILD SUCCESSFUL in 15s
167 actionable tasks: 54 executed, 113 from cache
```

## Criterion 4, continued — the counts found a live gap on their first CI run

The vacuity counts were added because check 11 shipped inert. They found check 11 still
inert, in CI, on the first run of this branch. The gate job's `actions/checkout` used the
default depth of 1, which creates no `refs/remotes/origin/main`, so
`git rev-parse --verify origin/main` fails and both check 11 and check 13's third condition
skip themselves while `docs-lint` exits 0.

```
observed: $ gh run view --job 99121072357 --log | grep -i "docs-lint:"
          docs-lint: OK — 19 documents, 101 anchors, 772 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, - introduced, - on origin/main.
```

`- introduced, - on origin/main` against the local run's `0 introduced, 51 on origin/main`
is the whole signal. Without the counts the two runs are indistinguishable: both print
`docs-lint: OK` and exit 0.

Fixed by giving the `build` job `fetch-depth: 0`, matching the `changes` job that already
has it (`.github/workflows/ci.yml`). Confirmed on the next run of the same pull request:

```
$ gh run view --job 99121455384 --log | grep -i "docs-lint:"
docs-lint: OK — 19 documents, 101 anchors, 772 references, 51 beans, 25 graph edges, 12 selectable, 51 bean ids, 0 introduced, 51 on origin/main.
```

`0 introduced` is correct on a pull-request run: GitHub checks out the `refs/pull/N/merge`
ref, so the merge base with `origin/main` is `main`'s tip and this branch introduces nothing
relative to it. That is also why the *cross-branch* condition is a **push-time** check, not
a CI one: on the merge ref a real collision has already become two files with one id in a
single tree, which condition (a) catches. Condition (c) is what refuses it earlier, on the
branch, before a reviewer is spent.

## Learnings

- **A count is the only difference between an inert check and a passing one.** Two CI runs
  of the same commit, one with the comparison skipped and one with it live, both print
  `docs-lint: OK` and exit 0. The `-` versus `0` in the counts is the entire signal, and it
  is the second time in this repository that a diff-shaped check was found inert only
  because someone printed what it examined (`bean:0038`, and here).
- **`Enforced by:` claims about diff-shaped checks are claims about the checkout, too.** A
  check that resolves `origin/main` enforces nothing under `actions/checkout@v4`'s default
  `fetch-depth: 1`. The mechanism was observed failing locally and was still inert where it
  mattered.
- **Bean front-matter carries free-form `#` comments.**
  `.beans/modus-0047--require-the-gate-check.md` has a second one, so "the marker is the
  only `#` line" and "the marker is line 2" are both wrong rules; the marker is the unique
  front-matter comment shaped `# <prefix>…`.

## Amendments

### 2026-09-05 · bean:0147

*The signal this bean named for revisiting the allocator has fired.*

**Claimed:** under "Recommendation for a human" — "Keep numeric ids. Revisit only if the
residual bites: check 13 cannot see a *second unmerged* branch, so two open branches still
collide until one merges. … The signal to revisit is a **second** collision that check 13
refuses; one refusal is the check working, two is the allocator being wrong. If it comes,
the cheap middle option is not nanoids but a reserved band per concurrent agent, which keeps
every existing citation resolvable."

**Found:** three concurrent agents collided on **three** ids at once, not one — the residual
this bean predicted, at the scale it did not. Working from `main` at `7731d13`, whose highest
bean is `modus-0129`, each agent took the next free ids and all three took `0130` onwards.
`bean:0147` allocated `0130`–`0133`; pull request #83 allocated `0130`–`0131`; pull request
#82 allocated `0132` and `0140`–`0146`. Check 13 refused none of them, and could not: its
cross-branch condition compares against `origin/main`, where every one of those ids is free,
and this bean already recorded that a *second unmerged* branch is outside what it can see.
The collision surfaced when a human compared the three open pull requests.

Two things this bean did not anticipate. First, the residual is not bounded at one id: an
agent that raises four follow-ups takes a **block**, and two agents each taking a block from
the same base overlap across the whole block. Second, the cost is not "one rebase and one
renumber": the losing branch renames every file, rewrites every `parent:` and `blocked_by:`
edge, and rewrites every `bean:NNNN` citation in its own prose and in the beans it had
already re-pointed — five files renamed and eleven rewritten here, with `docs-lint` check 6
as the only thing standing between a missed citation and a broken build.

The remedy this bean already names is the right one and is now due rather than hypothetical:
**a reserved band per concurrent agent**, allocated by whoever dispatches them. Prevention
by construction, no change to the id scheme, and every existing citation still resolves.
Raising the bean that implements it is out of this amendment's scope; `bean:0147` is an
implementation bean and its author is not the orchestrator that would own the dispatch rule.

**Evidence:**

```
cmd:      git log --oneline -1 origin/main
observed: 7731d13 fix(docs-lint): record the runtime failures no check inspects (#80)

cmd:      GITHUB_TOKEN= gh api repos/m4xy/Modus/contents/.beans --jq '.[].name' | tail -1
observed: modus-0129--a-heading-or-row-inside-a-raw-html-block-is-still-a-citation-site.md

cmd:      bash tools/docs-lint.sh          # on this branch, before the renumber
observed: docs-lint: OK — 19 documents, 111 anchors, 1787 references, 116 beans,
            57 graph edges, 48 selectable, 116 bean ids, 4 introduced,
            112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          exit=0

          `4 introduced` is the whole finding: check 13 saw four ids introduced against
          origin/main, found all four free there, and passed — while two sibling branches
          held three of them. The check worked exactly as specified and the specification
          is what does not cover this.

resolved: modus-0130 -> modus-0147, modus-0131 -> modus-0148, modus-0132 -> modus-0149,
          modus-0133 -> modus-0150, and the unwritten 0134 -> modus-0151. Renumbering fell
          to this branch because the other two pull requests were further along.
```
