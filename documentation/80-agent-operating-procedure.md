---
id: doc:80-agent-operating-procedure
title: Agent operating procedure
status: active
superseded_by: null
read_when: always
provides:
  - doc:80-agent-operating-procedure#pick-up-the-work-item
  - doc:80-agent-operating-procedure#restate-success-criteria
  - doc:80-agent-operating-procedure#plan-and-budget
  - doc:80-agent-operating-procedure#investigate
  - doc:80-agent-operating-procedure#implement
  - doc:80-agent-operating-procedure#self-validate
  - doc:80-agent-operating-procedure#encode-the-learnings
  - doc:80-agent-operating-procedure#open-the-pull-request
  - doc:80-agent-operating-procedure#respond-to-review
depends_on: [doc:00-constitution, doc:10-architecture, doc:20-ddd-practices, doc:30-code-style, doc:50-memory-and-evidence, doc:60-cost-model, doc:70-skills]
---

# 80 — Agent Operating Procedure

**If you are an agent working in this repository, this is your loop.** Read
`00-constitution.md` and this file. Read everything else on demand, driven by the index
in `documentation/README.md`.

The SOP is nine steps. Do them in order. Do not skip step 2.

```
1. Pick up          → load the work item, the memories, the domain's rules
2. Restate          → success criteria, in your own words, before doing anything
3. Plan & budget    → approach, context budget, model/effort, cost estimate
4. Investigate      → find out; never ask what you can discover
5. Implement        → smallest correct change
6. Self-validate    → run the gate; gather evidence per criterion
7. Encode learnings → memories and documentation
8. Open the PR      → claims with evidence attached
9. Respond to review→ change, encode, or decline — each with a reason
```

At every step: hold the 300k context budget (`00-constitution.md` §6) and record spend
(`60-cost-model.md` §3).

---

## Step 1 — Pick up the work item <a id="pick-up-the-work-item"></a>

### Do

1. Read the work item file in `beans/` **whole**. It is the only thing you read whole.
2. Load the memory set for its scopes (`50-memory-and-evidence.md` §5):
   - all `active` **domain**-scoped memories,
   - all `active` **epic**-scoped memories for the parent epic,
   - all `active` **story**-scoped memories for this item.
   Read assertions and evidence *summaries*. Do not expand evidence payloads yet.
3. Read the domain's process definition: its states, its definition of done, its required
   evidence kinds, its model/effort policy. **Do not assume the default process** — every
   domain has its own way of working (`00-constitution.md` §8).
4. Check for a **skill** that covers this task shape (`70-skills.md`). If one exists, use
   it. If one nearly exists, note the gap for step 7.
5. Note the parent epic and any linked work items, by id. Do not read them yet.

### Do not

- Do not read the whole `documentation/` package. Use the index table.
- Do not read the whole repository "to get oriented". Orientation comes from memories and
  from targeted search.
- Do not expand every evidence payload. Summaries first; payloads only for a memory you
  intend to rely on heavily or challenge.

### Refuse to start if

- There is no work item. Create one first (`00-constitution.md` §7.2).
- The work item has no success criteria. Draft them and get them agreed before working —
  criteria written after the fact describe what happened, not what was required.
- You are on `main`. Create the branch.

**Typical context after step 1: 10k–25k tokens.** If you are past 40k here, you read too
much. Restart.

---

## Step 2 — Restate the success criteria <a id="restate-success-criteria"></a>

**Never skip this step.** It is the cheapest step and it prevents the most expensive
failures.

Write, into the work item:

1. **The criteria in your own words** — numbered, binary, checkable. If you cannot restate
   a criterion as something checkable, it is not yet a criterion; sharpen it now.
2. **The evidence kind that will satisfy each one** (`50-memory-and-evidence.md` §2.1).
   Write it down before you know how you will get it — that is what makes it honest.
3. **What is explicitly out of scope.** Scope creep is the second most common cause of a
   context blowout.
4. **Your sizing judgement.** Can a competent agent finish this inside 300k tokens? If not,
   **stop and split the work item now** (`00-constitution.md` §6.2). Splitting costs
   minutes at step 2 and costs a whole abandoned run at step 5.

### The two questions

- *"If I did exactly this, would the requester consider it done?"* If unsure, the criteria
  are wrong, not the work.
- *"How would someone prove me wrong?"* That is your evidence plan.

If a criterion genuinely requires a product decision with more than one defensible answer,
this is the one moment where asking is correct (`00-constitution.md` §4). Ask once, with
options and a recommended default, and continue on everything else meanwhile.

---

## Step 3 — Plan and budget <a id="plan-and-budget"></a>

Record in the work item, briefly:

| Item | Detail |
|---|---|
| **Approach** | 3–8 bullets. Which modules change, which ports, which tests. Not an essay. |
| **Context budget** | Expected peak; checkpoints at 100k and 200k |
| **Model & effort** | From the domain policy or the skill's cost profile (`60-cost-model.md` §4.4). Record the **rationale** — required, non-empty. |
| **Delegation plan** | Which investigations go to subagents (their tokens are not yours) |
| **Cost estimate** | Rough dollars. Label it `estimated`; never write an estimate into a spend record. |
| **Risks** | What could make this take 3× longer, and the early signal for each |

**Layering check before you write any code:** for each thing you intend to change, name
the layer it belongs to using `10-architecture.md` §2.1. If you cannot place it, you have
found an architecture gap — resolve it now, not in review.

---

## Step 4 — Investigate <a id="investigate"></a>

> **Investigate; do not ask** (`00-constitution.md` §4). Before you may ask a human, you
> must have searched the repository, read the relevant documentation, read the memories,
> run the thing, and read the upstream source.

### The investigation loop

```
narrow the question  →  cheapest tool that could answer it  →  capture evidence  →  repeat
```

### Tool ladder — cheapest first, always

| Cost | Tool | Use for |
|---|---|---|
| ~free | `rg -l 'pattern'` | Does it exist? Where? |
| ~free | `rg -n 'pattern'` | Where exactly? |
| cheap | Read a **line range** around a hit | What does it say? |
| cheap | `git log --oneline -- <path>`, `git blame -L` | Why is it like this? |
| cheap | `./gradlew :module:test --tests X` | What does it actually do? |
| moderate | Read a whole file (only under ~500 lines) | The file is genuinely the unit |
| moderate | **Delegate to a subagent** | Fan-out across many files; you want the conclusion, not the corpus |
| expensive | Read many files yourself | Almost never correct — delegate instead |
| most expensive | **Ask a human** | Only the four cases in `00-constitution.md` §4 |

### Evidence discipline

Capture evidence **as you go**, not reconstructed at the end. Reconstructed evidence is
unreliable and often subtly false. Every non-obvious finding gets an evidence record
(`50-memory-and-evidence.md` §3) with a ≤ 200-character summary.

When you find something surprising, ask whether a **memory** already should have told
you. If yes and none exists, that is a step 7 action. If a memory exists and is wrong,
that is a contradiction — handle it now (`50-memory-and-evidence.md` §6.3).

### Delegation rules

- Give the subagent a **narrow question and a required output shape**. "Find every place X
  is validated; return file:line and the rule enforced." Not "look into X".
- Require evidence in the return. A subagent's summary with no citations does not compose
  (`50-memory-and-evidence.md` §2.2).
- Never delegate the decision. Delegate the search; keep the judgement.

### Context checkpoint at 100k

Stop. Write the current state of your understanding into the work item, in under 300
words. Re-read your restated criteria from step 2. Ask: *am I still working on the thing I
was asked to do?* Then continue.

---

## Step 5 — Implement <a id="implement"></a>

| # | Rule |
|---|---|
| 5.1 | **Smallest correct change.** No drive-by refactors, no unrelated fixes, no "while I was in here". Note them as follow-up work items instead. |
| 5.2 | **Layer discipline.** Domain first (aggregates, VOs, ports), then use cases, then adapters, then wiring. If you are writing an adapter before the port exists, you are designing outside-in and will leak infrastructure inwards. |
| 5.3 | **Test alongside, not after.** Every invariant gets an accepting test and a rejecting test (`20-ddd-practices.md` §7.3). |
| 5.4 | **Run the fast gate frequently.** `./gradlew :core-domain:check` should stay under 10 seconds. Fail fast and locally. Project names are flat (`settings.gradle.kts`), so the path is `:core-domain`, not `:core:core-domain` — the latter fails with `project 'core' is ambiguous`. |
| 5.5 | **Format continuously.** `./gradlew ktlintFormat` before every commit. Never think about formatting. |
| 5.6 | **No `TODO`, no commented-out code, no `@Suppress` without a reason** — the build enforces all three (`30-code-style.md`). |
| 5.7 | **Commit in logical increments**, conventional-commit messages, on your branch. A commit that does not compile is acceptable mid-branch; the PR head must be green. |
| 5.8 | **If the approach is failing, stop.** Two failed attempts at the same thing means the approach is wrong. Return to step 3, record what did not work as evidence, and pick a different approach. Do not brute-force — that is what `overhead` spend measures, and it will show. |

### Context checkpoint at 200k

Stop. Summarise progress into the work item: criteria met, criteria remaining, evidence
collected, what is left. If you cannot finish the remainder in 100k tokens, **hand off**:
commit what is green, record your learnings as evidence-backed memories, update the work
item, and open the PR as a draft explaining what remains. Handing off cleanly at 200k is
professional; blowing the budget at 300k and losing coherence is not.

---

## Step 6 — Self-validate <a id="self-validate"></a>

**You validate your own work before anyone else looks at it.** A pull request that fails
the gate wastes a reviewer's attention, which is the most expensive resource in the
system.

### The gate

**The gate is `00-constitution.md` §7.2.4.** Run exactly what that block says — this step
does not carry its own command list, because a gate written down in three places is a gate
that gets run in three different ways. In short: `ktlintFormat`, then `qualityCheck`. That
block also carries the `Enforcement gap:` for the backoffice and Playwright checks, which
no task runs; read it before assuming a green `qualityCheck` covered `backoffice/`.

If a **skill** governs this task, its validation command is the gate (`70-skills.md`
§3.6), and its exit code is the verdict.

### Then, per criterion

For each success criterion from step 2:

1. Run the thing that proves it.
2. Capture the evidence record — correct kind, within the size caps
   (`50-memory-and-evidence.md` §3.3).
3. Attach it to the criterion on the work item.
4. A criterion with no evidence is **unmet**. There is no third state.

### Rules

- **Never weaken a criterion to reach green.** Changing criteria is a separate work item
  and a human decision (`70-skills.md` §5.1 point 8).
- **Never claim a criterion is met because the code looks right.** A `citation` proves the
  code says something; only a `test-run` proves it does something
  (`50-memory-and-evidence.md` §2.3).
- **Read the failure tail, not the whole output.** Test output is a context sink.
- A flaky test is a failing test. Fix it, or open a work item and put its id in the
  `@Disabled` **annotation value** — `@Disabled("bean:NNNN: reason")`. ArchUnit reads the
  annotation value and fails the build without it (`30-code-style.md` §5.1). A comment
  beside the annotation is not enough and never was: comments are not in bytecode.
- Re-run the full gate after your **last** change. "It passed before that last tweak" is
  how broken PRs get opened.

---

## Step 7 — Encode the learnings <a id="encode-the-learnings"></a>

This is the step agents skip, and skipping it is what makes the next agent pay again.

| What you learned | Where it goes |
|---|---|
| A durable fact about a system, with evidence | A **memory**, at the narrowest true scope (`50-memory-and-evidence.md` §1.1) |
| A rule about how this repository works | **`documentation/`**, in the same PR |
| A rule a tool should catch | A **ktlint/Detekt/ArchUnit rule** — implemented now, or a work item that says so |
| A procedure you followed and would follow again | A **skill**, or an improvement to an existing one (`70-skills.md` §2) |
| An architecture decision you took | An **ADR** in `documentation/adr/` |
| Something you noticed but did not fix | A **work item** in `beans/`. Never a `TODO` comment. |
| A memory you found to be wrong | **Invalidate it**, with the disproving evidence attached |

Also record: your **peak context**, your **spend by stage**, and the **model and effort**
you used with the rationale (`60-cost-model.md` §3.2). If your `overhead` share exceeded
25%, say why — that is a finding, and it raises an action.

---

## Step 8 — Open the pull request <a id="open-the-pull-request"></a>

### Title

Conventional commit: `<type>(<scope>): <subject>` — imperative, no trailing period,
≤ 72 characters. This becomes the squash-merge commit subject.

### Body — required structure

```markdown
## What

One paragraph. What changed and why. Not a file list — the diff is the file list.

## Work item

`beans/NNNN-slug.md`

## Success criteria and evidence

| # | Criterion | Evidence | Result |
|---|-----------|----------|--------|
| 1 | `./gradlew qualityCheck` exits 0 | test-run `01JB…`: 412 passed, 0 failed, exit 0 | met |
| 2 | Cross-domain access returns 404 | test-run `01JB…`: `DomainIsolationIT` 18 passed | met |
| 3 | ADR recorded for the storage choice | diff `01JB…`: `documentation/adr/0002-…` added | met |

## Decisions

Any judgement call a reviewer should check, and the alternative you rejected.
Anything that should have been an ADR but you judged too small — say so, so the
reviewer can overrule you.

## Learnings encoded

- Memory `01JB…` (epic scope): <assertion>
- `documentation/30-code-style.md` §4: new custom Detekt rule row
- Follow-up work item `beans/NNNN`: <what it covers>

## Cost and budget

Peak context: 148k / 300k. Spend: $2.14 (investigate $1.31, implement $0.52,
validate $0.19, overhead $0.12). Model: claude-opus-5 @ xhigh — long-horizon
agentic implementation per the domain policy.
```

### Rules

- **Every claim carries its evidence.** A body with a claim and no evidence is rejected
  without further review (`00-constitution.md` §3).
- **Never claim something you did not verify.** "Should work" is not a status. If you did
  not run it, say you did not run it and say why.
- **Surface your own doubts.** The thing you are least sure about goes in `## Decisions`,
  at the top of it. Reviewer attention spent where you point it is worth ten times
  reviewer attention spent hunting.
- Push the branch. **Do not merge your own PR.**
- Draft PRs are for incomplete work handed off at a checkpoint — mark them draft and say
  what remains.

---

## Step 9 — Respond to review <a id="respond-to-review"></a>

### The three valid resolutions

Every comment resolves as exactly one of:

1. **Change the code.** Make the change; reply naming the commit.
2. **Encode the rule.** If the comment is a rule that will recur, add it to
   `documentation/` or to a tool **in this PR**, and say so. This is the encoding rule
   (`documentation/README.md`) doing its job.
3. **Decline, with a reason.** Disagreeing is legitimate. State the reason and the
   evidence. "Won't do because X" is a complete answer; silence is not.

### Rules

| # | Rule |
|---|---|
| 9.1 | **A style comment is a toolchain bug.** Do not just fix the code. Reply noting that ktlint/Detekt/ArchUnit should have caught it, and open the work item (`30-code-style.md`). |
| 9.2 | **Do not agree reflexively.** If the reviewer is wrong, gather evidence and say so. An agent that capitulates to every comment is useless as a check. |
| 9.3 | **Do not expand scope.** A review comment asking for adjacent work becomes a follow-up work item, referenced in the thread. |
| 9.4 | **Re-run the full gate after every change**, and update the evidence table in the PR body. Stale evidence is worse than none. |
| 9.5 | **Repeated comments become rules.** A defect class caught twice in review becomes a tool rule (`60-cost-model.md` §6.7). Raise it. |
| 9.6 | **Both halves of the round trip are attributed, to different stages.** Performing the review bills to `review`; *responding* to it — this step — bills to `revise` (`60-cost-model.md` §6.6, §3.1). Splitting them is what makes "what did checking this change cost?" answerable separately from "what did fixing it cost?". |
| 9.7 | **Leave no thread unresolved.** Every thread ends in one of the three resolutions above. |

---

## Failure modes — recognise these in yourself

| Symptom | What is actually happening | Do this |
|---|---|---|
| Context past 200k and still investigating | The work item is mis-sized, or you are reading instead of searching | Checkpoint, split, hand off |
| Third attempt at the same fix | The approach is wrong, not the details | Stop; return to step 3; record the failures as evidence |
| About to ask a human a factual question | You skipped the investigation ladder | `rg` it. Read it. Run it. |
| About to write "should work" | You are asserting without evidence | Run it, or label it a hypothesis |
| Fixing things you noticed on the way | Scope creep | Follow-up work items |
| Criteria feel unreachable | The criteria were wrong, or the sizing was | Escalate at step 2's standard, not by quietly weakening them |
| Reading whole files repeatedly | Wrong tool on the ladder | `rg -n`, then a line range |
| Same lookup three times | Missing memory | Write one, with evidence |
| Same procedure three times | Missing skill | Extract one (`70-skills.md` §2) |
| Reviewer is telling you about formatting | Missing tool rule | Fix the tool, not just the code |
| Spend dominated by `overhead` | Retries and abandoned approaches | Say so in the PR; it raises an `investigate-overhead` action |

---

## The one-paragraph version

Pick up the work item and its memories. Restate the success criteria and the evidence that
will satisfy each — before anything else. Plan the approach, the context budget, and the
model and effort, and record why. Investigate rather than ask, cheapest tool first,
delegating fan-out to subagents, capturing evidence as you go. Implement the smallest
correct change, domain outwards, tests alongside. Validate yourself against every
criterion and attach the evidence. Encode what you learned as memories, documentation,
tool rules, or skills. Open a pull request whose every claim carries its receipt. Resolve
every review comment by changing the code, encoding the rule, or declining with a reason.
