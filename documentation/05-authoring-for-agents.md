---
id: doc:05-authoring-for-agents
title: Authoring for agents
status: active
superseded_by: null
read_when:
  - path: documentation/**
  - path: AGENTS.md
  - path: .beans/**
  - path: .github/pull_request_template.md
  - task: (write|edit|review).{0,30}(document|doc|spec|bean|front-matter|pr body)
provides:
  - doc:05-authoring-for-agents#front-matter
  - doc:05-authoring-for-agents#read-when
  - doc:05-authoring-for-agents#reference-syntax
  - doc:05-authoring-for-agents#one-fact-one-place
  - doc:05-authoring-for-agents#prose-ban
  - doc:05-authoring-for-agents#bean-split
  - doc:05-authoring-for-agents#checks
depends_on: [doc:00-constitution]
---

# 05 — Authoring for agents

Applies to: `documentation/**`, `AGENTS.md`, `.beans/**`, `.github/pull_request_template.md`.
Primary consumer: an agent under the 300k context ceiling (`doc:00-constitution` §6). Humans are served by the backoffice, not by prose here.
MUST / SHOULD / MAY, `Enforced by:` and `Enforcement gap:` are defined in `documentation/README.md` and are not restated here.

## 1. Front-matter <a id="front-matter"></a>

Every file under `documentation/` MUST open with a YAML front-matter block. Unknown keys MUST NOT appear.

| field | type | required | rule |
|---|---|---|---|
| `id` | `doc:<NN>-<slug>` | yes | MUST equal the filename without `.md`, prefixed `doc:`. |
| `title` | string | yes | Noun phrase. The only human label for the document; nothing else may carry a second one. |
| `status` | `active` \| `draft` \| `superseded` | yes | An `active` document MUST NOT cite a `draft` one. |
| `superseded_by` | `doc:` reference or `null` | yes | Non-null if and only if `status: superseded`. |
| `read_when` | `always`, or a list of predicates | yes | §1.1. |
| `provides` | list of `<id>#<anchor>` | yes | The anchors this document owns. May be empty only when `status: draft`. |
| `depends_on` | list of `doc:` ids | yes | Documents this one references. Not a reading list — §1.2. |

Example — the front-matter of `doc:40-durability`:

```yaml
---
id: doc:40-durability
title: Durability model
status: active
superseded_by: null
read_when:
  - path: adapters/adapter-persistence-flatfile/**
  - path: "**/*.ndjson"
  - task: on-disk format|atomic write|fsync|append-only|file lock
provides:
  - doc:40-durability#atomic-write
  - doc:40-durability#append-only-log
depends_on: [doc:00-constitution]
---
```

### 1.1 `read_when` <a id="read-when"></a>

`read_when` is the field that lets an agent skip a document and hold the context budget. Every predicate MUST be matchable by a program.

| predicate | value | matched against |
|---|---|---|
| `path:` | one repo-relative glob | every path in the task's changed-file set, planned or actual |
| `task:` | one lowercase regex, unanchored | the task description, lowercased |

The whole field MUST be the scalar `always` when the document is unconditionally required. `path: "**"` MUST NOT be used.

Selection algorithm. An agent MUST run this before reading any document:

1. Load the front-matter of every `documentation/*.md`. Cost: front-matter only, never a body.
2. Select every document whose `read_when` is `always`.
3. Select every document with at least one matching predicate.
4. Read the selected set. Read nothing else.
5. A fact needed from an unselected document is reached by resolving one reference (§2) and reading that anchor's section — not its file.

Predicate rules:

- A predicate MUST name an observable: a path glob, or terms that appear in a task brief. "when you are being careful" is not a predicate.
- A predicate MUST NOT reference repository state the agent has not been given.
- Adding a predicate that matches every task is a defect; use `always` or split the document.

### 1.2 `depends_on` is not a reading list

`depends_on` declares resolvability, not inclusion. Reading a document MUST NOT pull in its dependencies. Transitive expansion selects the whole package, which is the measured 268k-of-300k failure (`bean:0004`).

## 2. Reference syntax <a id="reference-syntax"></a>

One scheme: `kind:name`, optionally `#anchor`. It MUST be used inline, in front-matter, in beans and in PR bodies. A bare file path MUST NOT be used where a reference exists.

| reference | resolves to | notes |
|---|---|---|
| `doc:40-durability` | `documentation/40-durability.md` | |
| `doc:40-durability#atomic-write` | the anchor tag of that name in that file | MUST appear in that file's `provides` |
| `bean:0004` | `.beans/<prefix>0004--*.md` | exactly one match; `<prefix>` is `.beans.yml`'s `beans.prefix` |
| `adr:0002` | `documentation/adr/0002-*.md` | exactly one match |
| `rule:archunit/domainIsFrameworkFree` | the `ArchRule` declared under that identifier in `architecture-tests/` | identifier verbatim, camelCase |
| `rule:detekt/CyclomaticComplexMethod` | the rule id in `config/detekt/detekt.yml` | id verbatim; a rule `doc:30-code-style#custom-detekt-rules` specifies but `config/detekt/detekt.yml` does not yet declare is not a target |
| `rule:ci/build` | the job id in `.github/workflows/ci.yml` | |

Resolution: `kind` selects the directory; `name` MUST include the fixed-width numeric id in full (2 digits for `doc:`, 4 digits for `bean:` and `adr:`), then selects the file by prefix-glob; `#anchor` selects the owning heading. A reference resolving to anything other than exactly one target is broken (§6 check 6). An agent that resolves a reference to zero or to more than one target MUST stop and report the ambiguity — it MUST NOT guess or continue on a partial match.

## 3. One fact, one place <a id="one-fact-one-place"></a>

- A fact MUST have exactly one owning anchor, declared in that document's `provides`.
- Two documents MUST NOT `provide` the same anchor.
- Every other mention MUST be a reference (§2), never a copy. Restating the fact — accurately, in different words, "for convenience" — is a defect.
- Summaries, recaps, convenience tables and examples that re-encode a rule are restatement.
- A derived listing (for example the routing table in `AGENTS.md`) is permitted only when it is marked derived and names its normative source. On disagreement the source wins and the listing is the bug.

Evidence — PR #1 at review: 13 files (12 documents, 1 bean), +3458 lines, 8 review threads. Six were one fact living in two to four places and drifting apart:

| drifted fact | copies | outcome |
|---|---|---|
| per-model `effort` support | `60` §4.1, `60` §4.4, `70` §3.7 | the specified benchmark grid was four API calls that all return 400 |
| non-domain-scoped route allowlist | `10` §4.3, `30` §4, `00` §8 | `/domains` missing from two copies; the derived rule failed the build |
| skill-extraction threshold | `00` §5, `60` §1.4, `60` §5.3, `70` §2.1 | two versus three occurrences; the file with precedence held the wrong number |
| `@Disabled` enforcement claim | `30` §7 (twice), `80` step 6 | three documents asserted a check that cannot exist, so nobody checked it |
| the canonical aggregate example | `20` §2.1.4, `20` §7.2, `30` §4 | the most-copied snippet in the package violated three of its own rules |
| context consumed versus published | `10` §3, `10` §3.1 | the table required the imports the rule forbade |

Fix pattern: name the fact, give it one anchor in the document that owns the subject, replace every other copy with a reference.

**A count restated outside the thing it counts is a drift generator**, and this repository
has already produced two live instances. `bean:0035` found `tools/docs-lint.sh`'s header
saying "the eleven mechanical checks" and `build.gradle.kts`'s comment saying "The nine
checks", while §6's table had eleven rows — three statements, two wrong, none of which any
check could see. A comment that counts rows in another file is a copy of that file's length,
and lengths change. **Delete the count; cite the anchor.** Code comments are as bound by this
section as documents are — they are simply the copies nobody greps.

## 4. Prose ban <a id="prose-ban"></a>

| banned | write instead |
|---|---|
| motivation and rationale paragraphs | the rule; rationale belongs in an ADR and is referenced (`adr:NNNN`) |
| history — "previously", "this was changed because" | nothing; git holds history |
| restating the brief, or previewing the section below | the content |
| rhetorical framing, address to the reader, aspiration ("clean", "robust") | a measurable predicate |
| hedging — "generally", "usually", "should probably", "in most cases" | MUST / SHOULD / MAY |
| summaries, recaps, "in short", closing paragraphs | deletion; the reader is not skimming |
| emphasis on a rule — "critical", "very important" | ordering; every stated rule is required |
| an example that re-encodes a rule already stated | one example that exercises the rule |

Required forms:

- A table for any set of three or more parallel items.
- Typed `key: value` fields for anything a checker reads.
- Imperative rules, one per line, subject first.
- `Pre:` / `Post:` / `Invariant:` lines for any operation that touches state.
- Fenced blocks for a command and its verbatim output.

## 5. Documentation, beans and ADRs <a id="bean-split"></a>

| question | source of truth | reference |
|---|---|---|
| how must things be? | `documentation/` | `doc:…` |
| what is being done, by whom, with what evidence? | `.beans/` | `bean:NNNN` |
| why was this chosen over the alternatives? | `documentation/adr/` | `adr:NNNN` |

- A document MUST NOT describe work status, progress, plans or ownership. It states the present rule and cites `bean:NNNN` for anything in flight.
- An `Enforcement gap:` line MUST name the bean that closes it.
- A bean MUST NOT restate a rule; it references the anchor and records evidence.
- When a bean and a document disagree, the document is the rule and the bean is the work. If the rule is wrong, the same PR fixes the document.

**A bean's front-matter carries free-form `#` comments as well as its id marker.**
`.beans/modus-0047`'s second comment line records why it is blocked on a human. So "the id
marker is line 2" and "the id marker is the only `#` line" are both wrong, and a parser
built on either silently reads the wrong value — found by check 13 failing on the real tree
before any plant was written (`bean:0051`). Match `# <prefix><digits>` and require exactly
one.

## 6. Mechanical checks <a id="checks"></a>

Each check is decidable from repository contents alone.

| # | check | fails when |
|---|---|---|
| 1 | front-matter present | a `documentation/*.md` has no YAML block |
| 2 | front-matter valid | a required field is absent or mistyped, or an unknown key appears |
| 3 | supersession | `status: superseded` with `superseded_by: null`, or a non-null `superseded_by` on any other status |
| 4 | anchor ownership | one anchor appears in two documents' `provides` |
| 5 | anchor declared | a `provides` anchor has no `<a id="…">` in its own file, or an `<a id>` is not in `provides` |
| 6 | references resolve | a `doc:` / `bean:` / `adr:` / `rule:` reference matches other than exactly one target |
| 7 | predicate shape | a `read_when` entry is neither the scalar `always` nor a single `path:` or `task:` key |
| 8 | line budget | a `documentation/*.md` is outside the line range `documentation/README.md` states, or `AGENTS.md` exceeds 120 lines |
| 9 | derived listings | a row in `AGENTS.md` marked derived omits the `doc:` id it derives from, or itself states a `path:`/`task:` predicate value instead of citing that id |
| 10 | no bare bean paths | a bare `beans/NNNN` or `.beans/NNNN` path appears in `documentation/*.md`, `AGENTS.md` or `CLAUDE.md` prose, instead of a typed `bean:NNNN` reference (§2) |
| 11 | completed beans are final | a bean that was `completed` on the merge base changes in any way other than gaining entries under a trailing `## Amendments` section, or an amendment omits its date, its authoring bean, `**Claimed:**`, `**Found:**` or `**Evidence:**` (`adr:0005-evidence-lives-in-the-work-item#amendments`) |
| 12 | bean graph | a `blocked_by` or `parent` id matches other than exactly one bean file, a `blocked_by` edge names a `type: epic` bean, the `blocked_by` graph has a cycle, two beans that reach `AGENTS.md` step 1's tiebreak together share an `order` value, or no bean is selectable at all |
| 13 | bean id uniqueness | a bean id names two files in the tree, a filename's id and its front-matter `# <id>` marker disagree, a bean filename is not `<prefix><id>--<slug>.md` at `.beans.yml`'s `id_length`, or an id this branch **introduces** already exists on `origin/main` |
| 14 | a bean closes without evidence | a bean that is `completed` in the change and was not `completed` on the merge base carries no evidence section, an evidence section holding no entry, a numbered table in an evidence section with no evidence column, an unanswered numbered criterion, an evidence cell that is empty or holds only a name from `doc:50-memory-and-evidence#evidence-kinds`, or a fenced block that is never closed |

**Enforced by:** `tools/docs-lint.sh`, run by the `docsLint` task inside `qualityCheck`
(`rule:ci/build`). Each check has been observed rejecting a planted violation; check 11's
four rejections and its one accepted amendment are recorded in `bean:0038`, check 12's
six rejections and its one negative control in `bean:0035`, check 13's three in `bean:0051`,
check 14's six, its negative control and its observed CI failure in `bean:0055` and `bean:0063`.

Check 11 classifies by the `status:` on the **merge base**, not on the branch, and diffs the
base against the **working tree**. A bean moving `in-progress` → `completed` in the change
under review is a legal edit to a not-yet-completed bean; the identical edit to one already
`completed` is not. Reading the branch would block every closure; reading only committed
content would pass locally and fail in CI.

Check 12's *selectable* set is exactly what `AGENTS.md` step 1 returns: `status: todo`, not
`type: epic`, every `blocked_by` id resolving to a `completed` bean. The `order` collision is
scoped to that set grouped by `priority`, because only beans that reach the tiebreak together
can be tied by it; a bean with no `order` is not a collision, since absence is itself a
defined position. The counts on the `OK` line — beans, graph edges, selectable — are the
check's vacuity assertion: a run that parsed nothing reports zero rather than success.

Check 14 classifies a bean as *closing* when it is `completed` in the working tree and was
not `completed` on the merge base — check 11's diff shape, one status earlier. It therefore
never re-reads a bean the base already closed, which is what keeps it off the corpus check 11
has already frozen. Two evidence shapes are accepted because the corpus writes both: one
`## Success criteria and evidence` table with an `evidence` column, and a separate
`## Evidence` section beside a criteria section.

Three definitions the check depends on, stated here because they are rules and not
conventions:

- An **entry** in an evidence section is a table row, a sub-heading, or a fenced block. A
  section of prose alone is not an entry; evidence is `doc:00-constitution#evidence-rule`'s
  command, expectation and verbatim output, and none of those is a paragraph.
- An **evidence column** is one headed `evidence`, `observed`, `output` or `result`. An
  `evidence kind` column names what will be produced, not what was, so a numbered table in an
  evidence section that carries no evidence column restates its criteria and answers none of
  them. Check 14 rejects that table rather than letting its rows stand as their own evidence.
- A **fence** opens on a line of three or more backticks or tildes, indented at most three
  columns, and closes only on a line carrying at least as many of the SAME character and
  then nothing but whitespace (CommonMark §4.5). A backtick fence's info string MUST NOT
  contain a backtick. A transcript that quotes a fence marker MUST sit inside a longer
  fence, which is what makes the quoted marker content rather than a delimiter.
- A fenced block that is never closed fails check 14, naming the line it opened on. Which
  marker is content and which is a delimiter is not decidable from a file that leaves one
  open, and a check that guesses reads every line after it with its inside/outside sense
  reversed — in both directions, so a bean quoting this check's own output answers its own
  criteria and a filled evidence table is reported absent (`bean:0063`).
- A criterion is **answered** by an evidence row bearing its number, or by a `criterion N` or
  `criteria N–M` citation standing in the bean's **top-level Markdown prose** — a line that
  carries the citation as prose and is inside no container of any kind. The rule is stated
  positively and the containers it excludes are deliberately **not** enumerated: a fence, a
  block quote, an indented chunk, a raw HTML block, an HTML comment, a link-reference or
  footnote definition and anything else that renders as code, as a container or as nothing
  all fail it by construction rather than by being listed. An enumeration would be an
  allowlist and would fail on the first container nobody named, which is how this rule was
  already got past once. The reason the rule exists at all: in this repository a bean's
  pasted output quotes this check's own `criterion N is not answered` message, so counting a
  citation inside a container lets pasted output answer the criterion it reports as
  unanswered (`bean:0061`, `bean:0063`). A fence is an entry but is not a citation site; no
  other container is either.

  **Enforcement gap:** the rule above is a property; the check is not. `citation_site()`
  blocks exactly two things — four or more columns of indent, and a `>` on the citation's
  own line — and treats every other line as top-level prose. So a citation inside any
  container that does not put one of those two on the citing line itself answers its
  criterion today, including a lazy block-quote continuation (CommonMark §5.1 puts an
  unprefixed paragraph line inside the quote), a raw HTML block, front matter, and a list
  item. Stated as the mechanism rather than as a list of containers, because a list here
  would go stale as containers are found and would be the same enumeration the rule above
  exists to avoid. `bean:0061` owns the citation matcher and carries the work.
  A criterion whose evidence is a section that
  never names it is unanswered however long that section is, because
  `adr:0005-evidence-lives-in-the-work-item#evidence-home` puts the evidence beside the
  criterion it satisfies and a reader must be able to find the pairing.

A criterion the bean does not number is outside the per-criterion condition; the `OK` line's
`closing transitions`, `criteria checked` and `unnumbered` counts are what say how much was
examined, and every one of them reads `-` rather than `0` when there is no base.

Check 13's third condition is the only one in this table that reads a ref this branch does
not contain. `.beans/` **is** the id allocator, it is read at branch time, and nothing
serialises two readers, so two agents in parallel worktrees can both take the next free id
and both be right within their own tree (`bean:0051`). Allocate against `origin/main` — the
next id free there, fetched, not the next free in the worktree — and check 13 will say so
if a sibling branch got there first. The check compares the ids the merge base carries
against the ids `origin/main` carries; with no `origin/main` it is inert by construction and
reports `-` for both counts rather than `0`, so an inert run is distinguishable from a clean
one. It cannot see a *second unmerged* branch: two open branches still collide until one
merges, which is the accepted residual of detecting rather than preventing.
