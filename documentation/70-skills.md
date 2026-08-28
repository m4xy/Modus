# 70 — Skills

> **Always make skills for recurring tasks.**

A skill is Modus's unit of encoded competence: a named, versioned, reusable procedure
with success criteria, a validation command, and a measured cost profile. Skills are how
expensive judgement becomes cheap procedure.

---

## 1. What a skill is, and is not

| A skill **is** | A skill **is not** |
|---|---|
| A named procedure with a defined trigger | A prompt fragment |
| Something with success criteria you can check | A description of good intentions |
| Something with a validation command that exits non-zero on failure | Something judged by reading the output |
| Something with a measured cost profile | Something whose cost is assumed |
| Versioned, reviewed, and owned like code | An ad-hoc instruction pasted into a chat |
| Autonomous — it validates its own output | A step that always needs a human to check it |

If it has no validation command, it is a **checklist**, not a skill. Checklists are
allowed; they just do not get the name, and they do not get the cost claims.

---

## 2. When to extract a skill

### 2.1 The triggers

Extract when **any** of these fires. Most come from `module-cost` automatically, which
raises an `extract-skill` action.

**The thresholds live in `60-cost-model.md` §5.3 and only there.** That table is what
`module-cost` measures, so it is the normative one; this table gives the rationale for
each trigger, which is the part you need when deciding whether a fired trigger is worth
acting on. It deliberately carries no numbers — an earlier draft carried a second copy of
them, and the two had already drifted.

| Trigger | Rationale |
|---|---|
| **Repetition** | Third time is the charm; the second time you notice, the third you act |
| **Aggregate spend** | Straight payback arithmetic |
| **Variance** | Unpredictable execution means the procedure is not understood |
| **Rediscovery** | Agents are re-learning the same thing every time |
| **Inconsistency** | Divergence produces defects and unreviewable diffs |
| **Escaped defect** | The step needs to be mechanical |
| **Human repetition** | That explanation is a skill trying to be born |

### 2.2 When *not* to extract

- The task happened twice and will not happen again. Wait for the third — this is the
  threshold the whole package uses, including `00-constitution.md` §5
  (`60-cost-model.md` §5.3).
- The variance is inherent, not procedural. Genuinely novel design work does not
  proceduralise; forcing it into a skill produces a skill that lies about what it covers.
- A **memory** would fix it. If the expensive part was *finding out a fact*, record an
  evidence-backed memory (`50-memory-and-evidence.md`), not a skill. Facts are memories;
  procedures are skills. Confusing the two produces bloated skills that go stale.
- A **lint rule** would fix it. If the recurring problem is "people keep doing X wrong",
  the answer is Detekt or ArchUnit (`30-code-style.md`), which costs nothing per run.
  Reach for a skill only when a judgement call remains after the mechanical part is
  automated.

**Escalation order — always try in this order:**

```
1. Make it impossible        (a type, an API shape that has no wrong option)
2. Make it mechanically caught (ktlint / Detekt / ArchUnit / schema validation / a test)
3. Make it a fact            (an evidence-backed memory)
4. Make it a skill           (a procedure with validation)
5. Make it documentation     (a rule a reader must remember)
6. Ask a human               (last resort — 00-constitution.md §4)
```

Every step down that list is more expensive per use than the step above it. A skill is
step 4 for a reason: it is cheap compared to human attention, but not free.

---

## 3. What a skill must contain

Every skill is a document (`40-durability.md` §2.1) at
`domains/<domainId>/skills/<skillId>.md`, or in the repository's `.claude/skills/` for
skills that operate on this repository. The required sections:

### 3.1 Identity

| Field | Required | Notes |
|---|---|---|
| `id` | yes | Stable, kebab-case, never renamed. Renaming breaks the cost history. |
| `name` | yes | Short verb phrase: "add a REST endpoint", "extract an ADR" |
| `version` | yes | Semver. A behaviour change is a minor bump; an incompatible interface change is major. |
| `owner` | yes | The domain, and the actor accountable for it |
| `status` | yes | `draft` \| `active` \| `deprecated` \| `retired` |

### 3.2 Trigger — when this skill applies

State it precisely enough that an agent can decide in one read whether this is the skill
for the job. Include the **anti-trigger**: the near-miss cases where it does *not* apply
and what to use instead. A skill without an anti-trigger gets misapplied.

### 3.3 Preconditions

What must be true before starting: branch state, required tools, required permissions,
required inputs. Each precondition is **checkable by a command**. The skill fails fast if
a precondition is unmet; it never proceeds hopefully.

### 3.4 Procedure

Numbered, imperative steps. For each step:

- The exact command, where the step is a command (`argv`, not a shell string).
- The expected observable outcome.
- What to do when it is not observed. **Every step has a failure branch.** A procedure
  with only a happy path is a wish.

Steps that need no model call are marked `deterministic`. Maximising these is the whole
cost game (`60-cost-model.md` §5.4).

### 3.5 Success criteria — mandatory

Explicit, checkable, binary. Not "the code is clean" but:

```
- [ ] `./gradlew check` exits 0
- [ ] The new endpoint's path begins with /domains/{domainId}
- [ ] An integration test asserts 404 for an actor without a grant on that domain
- [ ] The OpenAPI document regenerates with no unrelated diff
- [ ] Every criterion above has an evidence record attached to the work item
```

Each criterion names the evidence kind that satisfies it (`50-memory-and-evidence.md`
§2.1). A criterion nobody can check is not a criterion.

### 3.6 Validation command — mandatory

**A single command whose exit code is the verdict.**

```
validation:
  argv: ["./gradlew", "check", "--console=plain"]
  cwd: "."
  timeoutSeconds: 900
  successExitCode: 0
```

Rules:

| # | Rule |
|---|---|
| 3.6.1 | Exactly one command. If you need several, wrap them in a script that is itself in the repository. |
| 3.6.2 | Exit code is the verdict. Parsing stdout for "looks fine" is forbidden — it is unreliable and it invites a model to grade its own homework. |
| 3.6.3 | It must be runnable by an agent with no human present, no interactive prompt, no TTY. |
| 3.6.4 | It must be deterministic. A flaky validation command is worse than none: it teaches agents to retry until green. |
| 3.6.5 | Its output becomes a `command` or `test-run` evidence record on the work item. |
| 3.6.6 | It must fail when it should. Every skill's tests include a **negative case**: deliberately broken output that the validation command rejects. A validation command nobody has watched fail is unproven. |

### 3.7 Cost profile — mandatory for `active` skills

The measured grid from `60-cost-model.md` §4.1, plus the recommended cell:

```yaml
costProfile:
  measuredAt: 2026-08-20
  sampleSize: 12
  recommended: { model: claude-sonnet-5, effort: medium }
  grid:
    # Haiku 4.5 takes no effort parameter — it rejects output_config.effort with a 400.
    # The grid is ragged by model; see 60-cost-model.md §2 and §4.1.
    - { model: claude-haiku-4-5,  effort: n/a,    meanCostUsd: 0.0412, successRate: 0.58, attempts: 1.9, effectiveCostUsd: 0.0783 }
    - { model: claude-sonnet-5,   effort: medium, meanCostUsd: 0.1140, successRate: 0.92, attempts: 1.1, effectiveCostUsd: 0.1254 }
    - { model: claude-opus-5,     effort: high,   meanCostUsd: 0.4820, successRate: 0.97, attempts: 1.0, effectiveCostUsd: 0.4820 }
  evidence: [ 01JB..., 01JB... ]
```

A `draft` skill may carry an estimate, clearly labelled. A skill goes `active` only once
it has a measured profile. Profiles go stale after 90 days or on a price-book change
(`60-cost-model.md` §4.3).

### 3.8 Context budget

The skill states its expected peak context and its ceiling. A skill that cannot be
executed inside its stated ceiling is a bug in the skill, not in the agent
(`00-constitution.md` §6).

### 3.9 Evidence produced

Which evidence records the skill emits, of which kinds, and which success criteria they
satisfy. This is what makes a skill's output auditable rather than merely plausible.

---

## 4. Celebrity skills

> A **celebrity skill** is one that is well-known, well-named, heavily used, and trusted
> enough that agents reach for it by name without being told to.

Modus deliberately optimises for a **small number of famous skills** rather than a large
number of obscure ones.

### 4.1 Why

| Problem with a long tail | How celebrity skills fix it |
|---|---|
| An agent cannot find the right skill among 200 | It knows the 15 by name |
| Each rarely-used skill goes stale unnoticed | Heavy use surfaces breakage immediately |
| Cost profiles on rare skills are statistically meaningless | High-volume skills get real distributions |
| Discovery costs context on every task | A short, memorable roster costs almost nothing to hold |
| Overlapping skills produce inconsistent output | One canonical way per task shape |

### 4.2 What makes a skill a celebrity

| Property | Detail |
|---|---|
| **Memorable name** | A verb phrase an agent would guess: `add-rest-endpoint`, `extract-adr`, `add-archunit-rule`, `profile-task-cost`, `harden-flaky-test`. Never `helper`, `util`, `misc`, `process-v2`. |
| **Broad applicability** | Covers a whole task *shape*, not one instance. `add-rest-endpoint`, not `add-work-item-list-endpoint`. |
| **High usage** | Used at least monthly. Usage is measured (`module-cost` attributes spend by `skillId`). |
| **Strong validation** | Its validation command genuinely catches its failure modes, proven by negative-case tests. |
| **Stable interface** | Its trigger and inputs change rarely. Callers can rely on it. |
| **Composable** | It does one thing and can be invoked from within another skill. |
| **Documented failure modes** | It says what it does badly, so agents know when to stop using it. |

### 4.3 Curation — actively fight the long tail

- **Merge overlaps.** Two skills covering the same shape become one; the loser is
  `deprecated` with a pointer.
- **Retire the unused.** A skill with no invocations in 180 days goes `deprecated`, then
  `retired` after another 90. Retired skills are kept for history, never loaded.
- **Cap the roster.** A domain's active skill roster should stay under ~25. Past that,
  discovery cost exceeds the benefit and consolidation is overdue.
- **Promote deliberately.** When a niche skill turns out to generalise, generalise its
  trigger and rename it — a rename is a new `id` plus a `supersedes` pointer, so the cost
  history follows.

**Enforced by:** `module-cost` raises `retire-skill` and `merge-skills` actions into the
domain's action list, with usage numbers as evidence.

---

## 5. The autonomous test-and-validate contract

The defining property of a Modus skill: **it proves its own output correct without a
human in the loop.** This is what makes remote orchestration possible.

### 5.1 The contract

A skill invocation, once started, MUST:

1. **Check preconditions**, and abort with a clear reason if unmet. Never proceed hopefully.
2. **Execute the procedure**, recording evidence at each step that produces an
   observable outcome.
3. **Run its validation command** and capture the result as a `command` or `test-run`
   evidence record.
4. **Self-assess against each success criterion**, attaching the specific evidence record
   that satisfies it. A criterion with no evidence counts as unmet.
5. **Report a binary verdict**: `passed` or `failed`. Never "mostly done", never "should
   be fine". Partial completion is `failed` with a list of unmet criteria.
6. **On failure, retry within its bounded retry policy** (default: 2 retries), and if
   still failing, **stop and escalate** with: what it tried, the validation output, the
   unmet criteria, and its best hypothesis. It does not thrash, and it does not weaken
   its own criteria to reach green.
7. **Record its spend** against the work item with `skillId` set (`60-cost-model.md` §3.2).
8. **Never modify the validation command or the success criteria to make itself pass.**
   This is the cardinal sin. Changing a skill's criteria is a separate work item, a
   separate pull request, and a human-reviewed decision.

Point 8 is enforced structurally: an agent executing a skill runs with the skill
definition as **read-only input**. A change to a skill file inside a run that invoked that
skill fails the pull request. **Enforced by:** a CI check comparing the skill files
touched by a diff against the skills invoked during the run.

### 5.2 Setting success criteria autonomously

Skills may set success criteria for work they generate — that is the point of
"skills that can set success criteria". The constraints:

- Criteria are set **before** the work starts and recorded on the work item. Criteria
  invented afterwards describe what happened, not what was required.
- Every criterion names its evidence kind.
- Criteria may be **added** during a run when investigation reveals a genuine requirement;
  the addition is recorded with a reason. Criteria may **never be removed or weakened**
  by the agent doing the work.
- Criteria inherit from the domain's definition of done (`00-constitution.md` §8) and may
  strengthen it but never weaken it.

### 5.3 What a skill may not decide alone

Even a fully autonomous skill stops and asks when it hits `00-constitution.md` §4's short
list: a genuine product decision, a credential or spend approval, a destructive or
irreversible action, or a conflict between two ratified rules. Everything else it works
out by investigation.

---

## 6. Skill lifecycle

| Phase | Entry condition | Exit condition |
|---|---|---|
| `draft` | An extraction trigger fired and someone wrote it | Validation command proven (positive **and** negative cases), profile measured |
| `active` | Above met, reviewed and merged | Superseded, unused for 180 days, or persistently failing |
| `deprecated` | Superseded or unused | 90 days elapsed, or all callers migrated |
| `retired` | Above | Never loaded again; kept for cost history |

Changing an `active` skill follows the ordinary workflow (`00-constitution.md` §7): work
item, branch, pull request, review. A skill change that alters behaviour requires
re-running the negative-case test and re-measuring the cost profile in the same pull
request.

---

## 7. Skill authoring checklist

- [ ] Steps 1–3 of the escalation order (§2.2) were tried first and are unavailable
- [ ] The name is a verb phrase an agent would guess
- [ ] The trigger **and** the anti-trigger are stated
- [ ] Every precondition is checkable by a command
- [ ] Every step has an expected outcome and a failure branch
- [ ] Deterministic steps are marked; model calls are justified step by step
- [ ] Success criteria are binary, checkable, and each names an evidence kind
- [ ] There is exactly one validation command whose exit code is the verdict
- [ ] A negative-case test proves the validation command fails when it should
- [ ] The cost profile is measured (or explicitly labelled an estimate on a `draft`)
- [ ] The context budget and ceiling are stated
- [ ] The skill does not overlap an existing skill; if it does, they are merged
- [ ] The skill cannot modify its own criteria (§5.1 point 8)
