---
id: doc:50-memory-and-evidence
title: Memory and evidence
status: active
superseded_by: null
read_when:
  - path: domains/**
  - path: core/core-domain/**/memory/**
  - task: memory|memories|evidence|assertion|hypothes|invalidat|record a (conclusion|finding)|what we (found|learned)
provides:
  - doc:50-memory-and-evidence#memory-scopes
  - doc:50-memory-and-evidence#evidence-kinds
  - doc:50-memory-and-evidence#primary-sources
  - doc:50-memory-and-evidence#unverified-shapes
  - doc:50-memory-and-evidence#evidence-record
  - doc:50-memory-and-evidence#writing-a-memory
  - doc:50-memory-and-evidence#invalidation
  - doc:50-memory-and-evidence#unevidenced-assertions
depends_on: [doc:00-constitution, doc:05-authoring-for-agents, doc:10-architecture, doc:35-testing, doc:40-durability, doc:80-agent-operating-procedure]
---

# 50 — Memory and Evidence

Durable memory is how Modus stops agents from re-learning the same thing at full price,
and how it stops them from confidently asserting things that are not true. Those two
goals pull in opposite directions, and **evidence** is what reconciles them.

> **The rule, in one line: nothing becomes a memory without evidence, and a memory dies
> when its evidence dies.**

---

## 1. What a memory is

A **memory** is a durable, scoped, evidence-backed assertion about the world that future
agents should not have to rediscover.

It is not:

- A summary of a conversation. (That is transcript; it is discarded.)
- A plan or an intention. (That is a work item.)
- A rule about how this repository works. (That is `documentation/`; see the README's
  encoding rule.)
- An unproven belief. (That is a **hypothesis**; see §7.)

A memory is a fact with a receipt.

### 1.1 The three scopes <a id="memory-scopes"></a>

| Scope | Subject | Lifetime | Typical content |
|---|---|---|---|
| **Domain** | A `DomainId` | Long — months to years | Stable facts about the domain's systems, conventions, environments, external dependencies, and constraints. "Deploys to staging require the `staging` label; the pipeline at `.github/workflows/deploy.yml:42` filters on it." |
| **Epic** | An `EpicId` | Medium — the epic's life plus a grace period | Facts discovered while working on this body of work that outlive any single story. "The legacy importer emits ISO weeks, not ISO dates; confirmed at `Importer.kt:88`." |
| **Story** | A `WorkItemId` | Short — the story's life | Facts that matter for finishing this specific piece of work. "Reproducing the bug requires `MODUS_STORE=/tmp/fixture-a`; without it the test passes." |

**Scope selection rule:** record at the **narrowest scope where the fact is true**, and
promote later if it proves durable. A story-scoped memory that three separate stories
turn out to need gets promoted to epic scope — the promotion is itself an evidenced act,
citing the three stories.

Over-scoping is the failure mode that poisons a domain: one wrong domain-scoped memory
misleads every future agent in that domain. Under-scoping merely wastes a little
rediscovery.

### 1.2 Where memories live

Files under the domain's store (`40-durability.md` §3):

```
domains/<domainId>/memories/domain/<memoryId>.md
domains/<domainId>/memories/epic/<epicId>/<memoryId>.md
domains/<domainId>/memories/story/<storyId>/<memoryId>.md
```

Markdown with YAML frontmatter, atomic write, optimistic concurrency — the ordinary
document rules from `40-durability.md`. The owning bounded context is `memory`
(`10-architecture.md` §3).

---

## 2. What counts as evidence

Evidence is **a record of an observation that another party could repeat**. That is the
entire test. If nobody — human or agent — could re-run it and see what you saw, it is not
evidence.

### 2.1 The accepted evidence kinds <a id="evidence-kinds"></a>

These six are the complete list. There is no `other`.

| Kind | Is | Must capture | Repeatable by |
|---|---|---|---|
| `command` | A command that was run and its result | The exact argv, working directory, environment deltas that matter, exit code, stdout/stderr tail (capped, see §3.3) | Running it again |
| `test-run` | A test execution | Test selector, framework, exit code, pass/fail counts, the failing assertion text if any | Running the selector again |
| `diff` | A change to the repository | Base commit sha, head commit sha or blob sha, the paths touched, `--stat` summary | `git diff base..head` |
| `citation` | A claim grounded in source | Repository-relative path, line range, the commit sha the lines were read at, and the quoted text | Opening the file at that sha |
| `fetch` | An external resource retrieved | URL, HTTP status, retrieval timestamp, content hash, and the quoted excerpt relied on | Re-fetching, or comparing the hash |
| `observation` | A recorded system behaviour that is not any of the above — an HTTP exchange, a screenshot, a log excerpt, a UI state | What was done, what was seen, and an artefact reference (path in the run's output log, screenshot path, `seq` range) | Repeating the interaction |

### 2.2 What is explicitly not evidence

| Not evidence | Why |
|---|---|
| "I know that…", "It is well known that…" | Model prior, not observation. May be a hypothesis (§7). |
| "The user said…" | Human statement. Record it as context on the work item, not as a memory. Preferences are not facts about the system. |
| A summary of another agent's conclusion with no underlying record | Evidence does not compose transitively through summaries. Cite the sub-agent's evidence records directly. |
| A screenshot with no description of how it was produced | Not repeatable. |
| A URL with no retrieval timestamp and no quote | The page changes; you have nothing. |
| "The build passed" with no command and no exit code | Which build? Which commit? |
| Code you wrote in this session, cited as proof it works | A `citation` proves the code says something. Only a `test-run` proves it does something. |
| A figure with no command | A number nobody can re-derive is a recollection, and it does not stay one: it is copied forward by the next reader and becomes load-bearing. Three agents cited a `122s` baseline that had never been measured; the runs that existed were 133s and 134s (`bean:0068`). |
| A count with no command and no tree — "51 references" | A count is an observation of one tree at one moment, and both halves are part of it. Without them nobody can get the same number, or find out why they got a different one. |
| Arithmetic over a table of figures | That the rows sum is a fact about the addition. It says nothing about whether any figure in them was ever observed, and checking it is not checking the measurement (`bean:0068`). |
| A citation that resolves but does not carry the claim made at it | The reference resolves, so every mechanical check passes and the reader stops at the pointer (`doc:05-authoring-for-agents#one-fact-one-place`). |
| A mechanism observed firing, never observed silent | Firing on every input is also firing. Enforcement is discrimination, so the claim needs **three** observations rather than one: the mechanism fires on the planted fault, it fires the expected *number* of times, and it is silent on the unmodified source. Any two of the three admit a mechanism that enforces nothing. The positive half is `doc:00-constitution#observed-failing`; `bean:0089` carries why the halves are not in one document. |

**A mechanism that retains a running "best" value corrupts the state it compares against.**
Once the retained value is itself the corrupt one, every later comparison is against the
retention rather than against the source, so one planted fault produces an unbounded run of
reports — a defect the mechanism has *because* it works. Nothing in a typecheck, a linter or
a reading reaches it; only the count does. Assert **how many** times a mechanism fires, never
that it fires: "the notice appears" passes on a mechanism that reports the same fault
forever. Reported from a live detector in this repository and recorded, unreproduced, in
`bean:0068`.

### 2.3 Strength

Evidence kinds are not equal. When they conflict, this is the precedence order:

```
test-run  >  command  >  observation  >  diff  >  citation  >  fetch
```

A `test-run` that contradicts a `citation` wins: the code does what it does, not what it
reads like. A `fetch` is weakest because external pages change and because documentation
lies about implementations more often than implementations lie about themselves.

**Corollary:** a memory asserting behaviour ("X returns 404 when Y") requires at least one
`test-run`, `command`, or `observation`. A `citation` alone is insufficient for a
behavioural claim; it is sufficient for a structural claim ("the config key is named
`store.root`").

### 2.4 A citation names a primary source, re-read <a id="primary-sources"></a>

- A `citation` or a `fetch` MUST name the artefact that decides the claim — the source
  file, the merged commit, the issue body and its resolution. An issue title, a changelog
  line, a search-result snippet or another reader's summary decides nothing.
- The cited artefact MUST be re-read at the moment it is relied on. A conclusion carried
  forward from an unread citation is a hypothesis (§7), whatever it was when first read.
- One verified reason MUST be preferred to several plausible ones. A reason that does not
  survive re-reading is recorded as struck, with what the source actually says, never
  silently dropped — otherwise the next reader finds the argument and cites it as settled.

Observed: a spike recommended rejecting mutation testing on four grounds, "each
sufficient". Re-read against the primary sources, one held; one collapsed, the cited
issue being an unmerged draft build-modernisation PR whose companion recorded the
opposite result; and two were overstated. The worked table is
`doc:35-testing#mutation-testing`.

### 2.5 The shapes in which a claim reads as verified <a id="unverified-shapes"></a>

Reported from one sprint (`bean:0068`). Derived listing: the rule for each row is at the
anchor in the last column and wins on disagreement. What this table adds is the **tell** — the
question that separates each shape from a claim that is actually verified. The set is open and
is deliberately not counted, here or in the heading: a claim quantified over a growing set is
stale on arrival, and this one grew by a row inside the stack that introduced it.

| shape | tell | rule |
|---|---|---|
| an `Enforced by:` line for a mechanism nobody has watched reject anything | who watched it fail, and what did it print? | `doc:00-constitution#observed-failing` |
| a figure with no command, or a count with no tree | what argv produced this number, and on what tree? | §2.2 |
| a citation that resolves but does not carry the claim made at it | does the cited anchor state this, or does it merely sit near it? | `doc:05-authoring-for-agents#one-fact-one-place` |
| a reason invented for a **declined** fix | was this reason weighed before the decline, or written after it? | `doc:80-agent-operating-procedure#respond-to-review` |
| a mechanism observed firing, never observed silent | did anything watch it *not* fire, and was the count asserted? | §2.2, and `doc:00-constitution#observed-failing` for the positive half |

**The invented reason for a declined fix is the worst of them**, named by its property rather
than by its position for the reason above. The others read as unfinished work and invite a
second look; a fabricated justification reads as a trade-off somebody made, and closes the
question for every later reader. A decline with no reason leaves a thread open, which is a defect with
a fix. A decline with an invented one leaves nothing to fix.

---

## 3. The evidence record <a id="evidence-record"></a>

### 3.1 Required fields — every kind

| Field | Type | Notes |
|---|---|---|
| `id` | `EvidenceId` | ULID; sortable by creation time |
| `kind` | one of the six in §2.1 | Closed set; a `sealed interface EvidenceKind` in `core-domain` |
| `capturedAt` | ISO-8601 UTC instant | From the `ClockPort`, never wall-clock in the domain |
| `capturedBy` | `ActorId` | The agent or human that observed it |
| `runId` | `RunId?` | The agent run during which it was captured, if any |
| `repoSha` | commit sha | The repository state at capture. Non-null for every kind except `fetch`. |
| `summary` | string, ≤ 200 chars | One line: what was observed |
| `payload` | kind-specific | See §3.2 |
| `artifactRef` | reference, optional | Pointer into the run's output log (`seq` range), or a path under the run directory, for anything too large to inline |

### 3.2 Kind-specific payloads

| Kind | Payload fields |
|---|---|
| `command` | `argv` (list of strings, not a shell string), `cwd`, `exitCode`, `stdoutTail`, `stderrTail`, `durationMs` |
| `test-run` | `selector`, `framework`, `exitCode`, `passed`, `failed`, `skipped`, `failureText?` |
| `diff` | `baseSha`, `headSha`, `paths` (list), `filesChanged`, `insertions`, `deletions` |
| `citation` | `path`, `startLine`, `endLine`, `sha`, `quote` |
| `fetch` | `url`, `httpStatus`, `fetchedAt`, `contentSha256`, `excerpt` |
| `observation` | `action`, `observed`, `artifactRef` (required for this kind) |

`argv` is a list, never a shell string: a shell string cannot be re-executed safely or
compared reliably.

### 3.3 Size discipline

Evidence must not blow the context budget (`00-constitution.md` §6) of the next agent
that reads it.

| Field | Cap | Overflow behaviour |
|---|---|---|
| `stdoutTail` / `stderrTail` | 4 KB, tail-biased | Full output stays in the run's output log; `artifactRef` points at the `seq` range |
| `quote` (citation) | 40 lines | Cite a narrower range, or split into two citations |
| `excerpt` (fetch) | 4 KB | Store the full body under the run directory; reference it |
| `failureText` | 8 KB | Same as stdout |
| Evidence records per memory | 10 | More than ten means the memory is really several memories |

**Enforcement gap:** the schema validation in `adapter-persistence-flatfile` that would
reject an over-cap write, rather than silently truncating it, does not exist —
`adapter-persistence-flatfile` is an empty placeholder with no tests today. `bean:0017`
carries it.

### 3.4 On disk

Evidence lives in the memory document's frontmatter under `evidence:`, so a memory and
its receipts are one atomic unit — you cannot read a memory and miss its evidence, and
you cannot write one without the other:

```yaml
---
id: 01JB2K9WQ8ZC7X3M4N5P6R7S8T
scope: epic
subject: EP-0007
assertion: >-
  The flat-file store rejects a second concurrent write to the same document
  with StaleWriteException rather than last-writer-wins.
status: active
recordedAt: 2026-08-28T09:31:44Z
recordedBy: agent:opus-5
evidence:
  - id: 01JB2K9WQ9A1B2C3D4E5F6G7H8
    kind: test-run
    capturedAt: 2026-08-28T09:30:12Z
    capturedBy: agent:opus-5
    runId: 01JB2K8...
    repoSha: 4f1c9e2
    summary: Concurrent-write suite passes, asserting StaleWriteException
    payload:
      selector: ":adapters:adapter-persistence-flatfile:test --tests *ConcurrentWriteTest"
      framework: kotest
      exitCode: 0
      passed: 7
      failed: 0
      skipped: 0
  - id: 01JB2K9WQ9A1B2C3D4E5F6G7H9
    kind: citation
    capturedAt: 2026-08-28T09:29:50Z
    capturedBy: agent:opus-5
    repoSha: 4f1c9e2
    summary: Version check before rename in AtomicFileWriter
    payload:
      path: adapters/adapter-persistence-flatfile/src/main/kotlin/.../AtomicFileWriter.kt
      startLine: 61
      endLine: 74
      sha: 4f1c9e2
      quote: |
        if (currentHash != expected.hash) throw StaleWriteException(path, expected, currentHash)
---

## Why this matters

Callers must handle 409 and retry. See work item 0042.
```

---

## 4. Writing a memory <a id="writing-a-memory"></a>

### 4.1 The five gates

A memory write is refused unless all five pass:

1. **Assertion is falsifiable.** It states something that could be shown false. "The
   store is well designed" is refused; "the store rejects a concurrent write with
   `StaleWriteException`" is accepted.
2. **Assertion is a single claim.** One memory, one fact. Conjunctions get split.
3. **Evidence is present, non-empty, and of a kind sufficient for the claim** (§2.3).
4. **Scope is the narrowest true scope** (§1.1).
5. **No active memory in the same scope contradicts it.** If one does, resolve the
   conflict first (§6.3) — do not stack contradictory memories.

**Enforcement gap:** neither the `RecordMemoryUseCase` in `core-application` nor the
adapter schema validation exists yet — the `memory` context is not built (`bean:0015`).
Once it is, only gate 3 is fully mechanical — presence and kind of evidence are structural
facts. Gates 1, 2, 4 and **5** are heuristic and are additionally checked at review time;
gate 5 is the weakest of them, since semantic contradiction between two free-text
assertions is not decidable, and the use case can do no more than surface the scope's
active memories for the writer to compare against. Gates 1, 2, 4 and 5 stay best-effort
checks plus review even then. Do not read "refused unless all five pass" as "a machine
proved all five".

### 4.2 Writing style for assertions

- Present tense, active voice, specific subject. "The importer emits ISO weeks."
- Include the anchor: a path, a command, an endpoint, a config key.
- No hedging. A hedged memory ("it seems that…") is a hypothesis; label it as one (§7).
- No temporal deixis. Never "currently", "now", "recently" — write the date into
  `recordedAt` and let the reader compute.
- ≤ 300 characters. If it needs more, the body of the document is where nuance goes; the
  assertion stays crisp because the assertion is what gets loaded into context.

### 4.3 When to write one

Write a memory when **all** of these hold:

- You spent real effort discovering it (a search, a run, a read of unfamiliar code).
- A future agent working in this scope would plausibly need it.
- It is not already in `documentation/` and does not belong there instead.
- You can attach evidence.

Do **not** write a memory for something a five-second `rg` would answer. A memory that is
cheaper to rediscover than to read is negative value: it costs context on every future
load.

---

## 5. Reading memories

- On starting a work item, an agent loads: all `active` domain-scoped memories, all
  `active` epic-scoped memories for the parent epic, and all `active` story-scoped
  memories for the item. This is SOP step 1 (`80-agent-operating-procedure.md`).
- Memories are loaded **assertion-first**. The assertion line and the evidence *summaries*
  are read; a full evidence payload is fetched only when the agent needs to verify or
  challenge that specific memory. This keeps memory loading at hundreds of tokens rather
  than tens of thousands.
- `superseded`, `expired` and `invalidated` memories are **not** loaded by default. They
  are available on request, which is what makes the history useful without making it
  costly.
- If the active memory set for a scope exceeds ~50 entries, that is a signal to
  consolidate (§6.4), not to raise the limit.

---

## 6. Invalidation <a id="invalidation"></a>

A memory is a claim about a world that changes. Memories that are never invalidated
become the most expensive kind of lie: the confidently-cited stale fact.

### 6.1 Statuses

| Status | Meaning | Loaded by default |
|---|---|---|
| `active` | Believed true; evidence stands | yes |
| `stale` | Evidence's anchor changed; needs re-verification before reuse | no — surfaced as an operator action |
| `invalidated` | Shown false; retained with the disproving evidence | no |
| `superseded` | Replaced by a more precise or more current memory; **carries `supersededBy`, always** | no |
| `expired` | Its subject is gone — the work item or epic it was scoped to is closed, and it was not promoted. Nothing replaced it | no |

`superseded` and `expired` are not interchangeable. `superseded` means "we know the better
version, here it is"; `expired` means "the thing this was about is over". A status that
promises a `supersededBy` pointer must be able to produce one, so a memory whose subject
merely ended is `expired`. **Enforcement gap:** the schema validation that would reject a
`superseded` memory with no `supersededBy` does not exist — the `memory` context is not
built (`bean:0015`).

Memories are **never deleted**. The history of what we believed and why we stopped
believing it is itself valuable, and deletion would let a bad memory disappear without
anyone learning why it was bad.

### 6.2 Invalidation triggers

| Trigger | Effect | Automatic? |
|---|---|---|
| A cited file's cited line range changes between `repoSha` and `HEAD` | → `stale` | Yes — a `git diff` check on every memory load for the scope |
| A cited path is deleted or renamed | → `stale` | Yes |
| A referenced `test-run` selector no longer exists | → `stale` | Yes |
| A `fetch` evidence record is older than 90 days | → `stale` | Yes |
| The referenced work item or epic is closed, for `story`/`epic` scope | → **`expired`** after a 30-day grace period, unless promoted to a wider scope. Not `superseded`: nothing replaced it, so there would be no `supersededBy` to set | Yes |
| An agent observes behaviour contradicting the assertion, with evidence | → `invalidated`, with the disproving evidence attached | No — requires an agent action, gate-checked |
| A more precise memory is recorded for the same claim | → `superseded`, `supersededBy` set | No |
| A human marks it wrong in the backoffice | → `invalidated`, with the human as `capturedBy` and their reason as an `observation` | No |

**Enforcement gap:** the `MemoryFreshnessChecker` that would run this on memory load and
on a schedule does not exist — the `memory` context is not built (`bean:0015`).

### 6.3 Handling a contradiction

When you find a memory that contradicts what you observe:

1. **Do not silently ignore it.** An ignored contradiction will mislead the next agent.
2. Gather evidence of the contradiction — the strongest kind you can (§2.3).
3. If your evidence is stronger, invalidate the memory, attaching your evidence and
   citing the memory id. Then record a replacement memory.
4. If your evidence is weaker or equal, mark the memory `stale` and record your
   observation as a hypothesis (§7). Do not invalidate on a hunch.
5. Either way, note it in the PR body. A contradiction discovered is a finding, not
   housekeeping.

### 6.4 Consolidation

When several active memories in a scope overlap, consolidate: write one memory whose
assertion covers them, carrying the union of the strongest evidence, and mark the
originals `superseded` with `supersededBy` pointing at it. Consolidation is an ordinary
work item and is itself evidenced (the diff is the evidence).

---

## 7. Hypotheses

A hypothesis is an unevidenced belief that is useful to write down.

- Hypotheses are stored on the **work item**, never in the memory store.
- They are always labelled: "Hypothesis: …".
- They may guide investigation. They may **never** be cited as a reason, quoted as a
  fact, or used to close a criterion.
- A hypothesis that gets evidence becomes a memory. A hypothesis that gets disproving
  evidence is recorded as a memory of the negative result — knowing that an approach does
  not work is genuinely valuable, and it is exactly the thing agents re-discover most
  expensively.

---

## 8. The prohibition on unevidenced assertions <a id="unevidenced-assertions"></a>

This restates `00-constitution.md` §3 with the enforcement detail.

| Where | Rule | Enforced by |
|---|---|---|
| Memory store | No memory without ≥ 1 evidence record of a sufficient kind | Schema validation at write; `RecordMemoryUseCase` gates |
| Work-item closure | Every success criterion carries ≥ 1 evidence record | Transition guard in the `work` context |
| Pull-request body | Every claim of completion names its evidence | Review; **enforcement gap** — a PR-body structural check is a follow-up work item |
| Agent output to a human | Claims are marked as evidenced or as hypotheses | The SOP (`80`); surfaced in the backoffice, which renders unevidenced claims visually distinct |
| Backoffice display | A memory is never rendered without its evidence being one click away | Playwright assertion in `e2e/` |

### 8.1 The phrase to use when you do not know

> "I have not verified this. Hypothesis: …. To confirm, I would run `<command>`."

That sentence is always acceptable. Asserting without evidence never is. An agent that
says "I don't know yet, here is how I'd find out" is behaving correctly; an agent that
guesses fluently is the single most expensive failure mode Modus exists to prevent.
