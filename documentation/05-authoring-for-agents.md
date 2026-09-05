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

Evidence — PR #1 at review: 13 files (12 documents, 1 bean), +3458 lines, 8 review threads. Six were one fact living in two to four places and drifting apart, or agreeing on something false:

| restated fact | copies | outcome |
|---|---|---|
| per-model `effort` support | `60` §4.1, `60` §4.4, `70` §3.7 | the specified benchmark grid was four API calls that all return 400 |
| non-domain-scoped route allowlist | `10` §4.3, `30` §4, `00` §8 | `/domains` missing from two copies; the derived rule failed the build |
| skill-extraction threshold | `00` §5, `60` §1.4, `60` §5.3, `70` §2.1 | two versus three occurrences; the file with precedence held the wrong number |
| `@Disabled` enforcement claim | `30` §7 (twice), `80` step 6 | three documents asserted a check that cannot exist, so nobody checked it |
| the canonical aggregate example | `20` §2.1.4, `20` §7.2, `30` §4 | the most-copied snippet in the package violated three of its own rules |
| context consumed versus published | `10` §3, `10` §3.1 | the table required the imports the rule forbade |

Fix pattern: name the fact, give it one anchor in the document that owns the subject, replace every other copy with a reference.

**Removing a copy and restating it in the same change is how the duplication comes back.**
"For the reader's convenience" is the phrasing it arrives in, and it was reported repeatedly
from one sprint (`bean:0068`). The reader who needs the fact follows the reference; the
reader who does not, does not need the copy either. A deduplicating change that leaves the
fact stated twice has relocated a copy, not removed one — and the relocated copy is the one
nobody will think to check.

**A pointer that carries content its target does not is an unowned rule.** A reference that
resolves is not a reference that says what the citing sentence says it says: check 6 sees the
first and no check can see the second. `bean:0058` shipped a pointer stating three things the
anchor it named was silent on; by this section's own resolution rule the source wins, so the
listing was derived from nothing and the rules in it belonged to no document. Move the
content to the anchor and own it there, or state it where it stands and put it in `provides`.

**A count restated outside the thing it counts is a drift generator**, and this repository
has already produced two live instances. `bean:0035` found `tools/docs-lint.sh`'s header
saying "the eleven mechanical checks" and `build.gradle.kts`'s comment saying "The nine
checks", while §6's table had eleven rows — three statements, two wrong, none of which any
check could see. A comment that counts rows in another file is a copy of that file's length,
and lengths change. **Delete the count; cite the anchor.** Code comments are as bound by this
section as documents are — they are simply the copies nobody greps.

**A count is a drift generator inside the thing it counts as well as outside it.** A heading
that numbers its own table, or an opening that says "all four", is invalidated by the row that
the next change adds — and the change need not come from another author or another branch.
`doc:50-memory-and-evidence#unverified-shapes` was introduced with four rows and a heading
saying four, and gained a fifth row two pull requests later in the same stack. **A set that
can grow is named, never counted.**

**Restatement's second cost is not drift. It is that being wrong once costs N corrections,
and copies that agree hide the error that drift would have exposed.** Where copies disagree
the disagreement is itself the signal; where they agree there is none, and the defect waits
for someone to reason about the claim.

Row 4 above is the prior instance: three documents asserted a `@Disabled` check that cannot
exist, agreed perfectly, and were found by realising that comments do not survive into
bytecode — not by comparing the copies against each other. `bean:0066` is the second: one
over-scoped scope clause at three sites — a rule in `doc:20-ddd-practices` §4.1.8, the
dispatch port's KDoc, and the handler contract's — all three in agreement, so nothing looked
wrong to a reader or to check 6, and it was found by a reviewer reasoning about the rule.
**Copies of a belief are as much a defect as copies of a fact, and they are the harder half:
a fact that drifts announces itself, and a belief restated faithfully never does.**

**The table's third column records what each fact was wrong about, not how it was found**, so
no row of it evidences a discovery mechanism, and a claim about one may not cite it. An
earlier draft of this passage asserted that every instance in the table was caught by two
copies disagreeing; three of the six were not — rows 1 and 5 were caught by exercising the
claim, and row 4 is the counter-example the paragraph above now rests on.

**The instance that carries the highest cost is the one on the type other authors implement.**
Of those three sites, the correction that mattered was the handler contract's, because four
bounded contexts will write against it and inherit whatever it claims. Rank restatement sites
by how many authors read them, not by how normative they look: the document is authoritative,
the interface is what gets copied.

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

**Flagging a doubt about a rule you are writing is not narrowing it.** A rule is read by
people who never see the pull-request body, the bean, or the review thread it was doubted in,
so a doubt recorded anywhere but in the rule's own text has no effect on what the rule
requires. `bean:0066` wrote `doc:20-ddd-practices` §4.1.8, asked in the same pull request
whether it was over-scoped, and shipped it over-scoped; the doubt was correct and changed
nothing. **State the narrow rule you believe, and cite the bean for the part you are unsure
of.** A rule stated wider than its author believes it is a rule nobody may rely on and
everybody must obey.

**The author of a rule is the worst-placed reader of its scope**, which is why the previous
paragraph is a rule and not advice. An author who has just built one implementation writes
the clause that implementation satisfies; the second implementation is the one that discovers
the clause forbids it. `doc:20-ddd-practices` §4.1.8 bound every dispatcher to propagate a
handler's exception to its caller, which the only existing dispatcher did and an asynchronous
one cannot. **Before stating a rule over a set, name the member of that set you have not
built, check the rule against it, and name it in the rule's own text or in the pull-request
body.** The naming is what makes this reviewable rather than advisory: an obligation
discharged in the author's head leaves a reviewer unable to tell it from one skipped. §4.1.8's
closing sentence is the form — it names the asynchronous dispatcher inside the rule, and the
scope clause it justifies cannot be read without meeting it.

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
| 14 | a bean closes without evidence | a bean that is `completed` in the change and was not `completed` on the merge base carries no evidence section, an evidence section holding no entry, a numbered table in an evidence section with no evidence column, an unanswered numbered criterion — where a citation answers only from a structural site, inside an evidence section or from a `## ` heading of its own, with something under it, and never from a row's evidence cell — an evidence cell that is empty or holds only a name from `doc:50-memory-and-evidence#evidence-kinds`, or a fenced block that is never closed |

**Enforced by:** `tools/docs-lint.sh`, run by the `docsLint` task inside `qualityCheck`
(`rule:ci/build`). Each check has been observed rejecting a planted violation; check 11's
four rejections and its one accepted amendment are recorded in `bean:0038`, check 12's
six rejections and its one negative control in `bean:0035`, check 13's three in `bean:0051`,
check 14's six, its negative control and its observed CI failure in `bean:0055` and `bean:0063`.
The citation rule below was observed rejecting a criterion cited from top-level prose, from a
raw `<pre>`, from an HTML comment, from a `<details>` wrapper, from a line-initial inline code
span and from a backtick info string, against negative controls for each of its two structural
sites, in `bean:0093`. In every one of those the citation stood on a line of PROSE, which is
what was rejected; the container was not, and the same containers holding a heading-shaped or
row-shaped line are accepted. That is stated in full below and owned by `bean:0129`. The
three conditions beyond shape — region, emptiness and the evidence cell — were each observed
rejecting a planted violation, against a negative control for each, in `bean:0121`.

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

Every condition in check 14's row above is structural. Whether the output in an evidence cell
was ever produced, whether the command beside it reproduces that output, and whether either
bears on the criterion the cell is filed under are outside what the check can decide. Scope
compounds it: `doc:00-constitution#bean-lifecycle` holds a bean `in-progress` for the whole
life of its own pull request, so on the pull request that implements a bean, that bean is
never a candidate. `0 closing transitions` on the `OK` line is that statement — no bean's
evidence was examined, and the zero beside it under `criteria checked` follows from the empty
candidate set, not from a bean inspected and found bare. A non-zero pair comes from a bean the
change *closes*, whose implementation merged earlier and was reviewed elsewhere. A green check
14 therefore establishes the shape of the evidence in the beans a change closes, and nothing
at all about the implementation under review (`bean:0096`).

The definitions the check depends on, stated here because they are rules and not
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
  `criteria N–M` citation standing at a **structural site**: a heading, or a row of a table.
  Running prose is not a citation site, whatever it renders as. Write the citation as an
  evidence sub-heading — `### Criterion 3` — or in a cell of the evidence table other than
  the **evidence cell** itself.

  Shape is necessary and not sufficient. Three further conditions apply, each decided from
  state the analyser already holds and each written to fail closed — a citation that does not
  satisfy them is not read, and its criterion is reported unanswered (`bean:0121`):

  | condition | a citation is not read from |
  |---|---|
  | region | a sub-heading or a row standing outside an **evidence** section — `## Evidence`, or the combined `## Success criteria and evidence`. `### Criterion 3 was not attempted` under `## Not in scope` answers nothing, and neither does `### Criterion 2 cannot be met as written` under `## Success criteria`: a criteria section is not an evidence section, and evidence is what this condition is about. A `## ` heading is exempt, because `region` is what a `## ` heading sets: a top-level section devoted to one criterion is that criterion's evidence home. Four completed beans write that shape — `bean:0038`, `bean:0049`, `bean:0051` and `bean:0063` — and `bean:0038` is the one that loses a criterion if `## ` is bound too |
  | emptiness | a citing heading with nothing under it before the next heading at its own level or shallower. `### Criteria 1-5` as the whole of a five-criterion bean's `## Evidence` answers nothing. Content is any **non-blank line**, which is deliberately weaker than the *entry* defined above: an entry rule refuses `### Criterion 2 cannot be met as written` followed by the ruling and its reason, which this section accepts below and `bean:0038` writes |
  | cell | the **evidence cell** of a row. The rest of the row is read either side of a **barrier** standing where the cell was, so `\| 3 \| criteria 1-5 \| … \|` still answers; the cell where output is pasted does not. The cost is a row that names, in its evidence cell, a span its own run genuinely covers — write that span in any **other** column of the row instead, since the cut is one column wide. Not necessarily the first: in a table whose rows are numbered, the first cell is the criterion number, and a span written there stops the row being numbered and so stops it answering its own criterion |

  The cell condition applies to every row and not only to a numbered one. Masking only a
  numbered row's cell leaves the identical laundering one column over, in the evidence cell of
  an unnumbered row; both forms give the same verdict on every bean in `.beans/`, so the
  corpus does not choose between them and the reasoning does.

  A row's **cells** are its pipe-separated fields, and two shapes GFM permits are cells all
  the same: a row may omit its trailing pipe, and a cell may hold an escaped `\|`. Both were
  measured getting past the cut before `bean:0121`'s review — the first left the evidence cell
  as the last field and so outside the cut, the second shifted every field after it and moved
  the cut onto the wrong column. The condition is unconditional, so both are cells now.

  The cut **replaces** the evidence cell; it does not delete it, and that distinction is a
  rule rather than an implementation detail. Deleting the cell makes its two neighbours
  adjacent, and the matcher — which skips any run of characters that are neither digit nor
  letter between `criterion` and its number — reads straight across the seam. So a row whose
  claim column ends `covers both criteria` and whose next-but-one column begins `3 runs`
  answered **criterion 3** from a citation standing in no cell of the file, with the evidence
  cell that separated them being the one thing the condition says not to read. The barrier that
  replaces the cell is a lowercase letter for the same reason the seam existed: the matcher's
  own gap class is what has to exclude it, and that class admits every character outside
  `[0-9a-z]`. This condition is therefore the one place where the check writes a character of
  its own into what it reads, and `bean:0121` records why the alternative — a separator no
  author could type — is measurably not enough.

  Three edges of **emptiness**, stated because each is a rule and not an accident. A line of
  only spaces or tabs is **blank**, so it is not content and does not save a citing heading.
  A fenced block IS content even when it is empty, which follows from an *entry* being a
  fenced block rather than the text inside one. And a citing heading immediately followed by
  a **sibling** heading heads nothing, so of two adjacent `### Criterion N` headings sharing
  one paragraph, only the second is answered — give each citing heading its own content.

  The rule names where a citation may stand and enumerates no container, because an
  enumeration would be an allowlist and would fail on the first container nobody named,
  which is how this rule was got past twice. The reason it exists at all: in this repository
  a bean's pasted output quotes this check's own `criterion N is not answered` message, and
  the matcher reads the presence of a number and never the polarity of the claim around it,
  so any rule that reads running prose lets pasted output answer the criterion it reports as
  unanswered (`bean:0061`, `bean:0063`, `bean:0093`).

  **What the rule is not.** It is a test of a line's SHAPE, not a model of containers, and
  the difference is load-bearing. The analyser recognises a fence and its own table state and
  nothing else; it holds no raw-HTML-block state. So a container is refused only insofar as
  its contents are neither heading- nor row-shaped, and a heading-shaped line inside `<pre>`,
  inside `<details><pre>`, or inside an HTML comment IS read as a citation, as is a Markdown
  table pasted inside `<pre>`, whose delimiter row enters the table like any other. A fence
  is the one container the analyser does model: it is an entry, and it is not a citation
  site. `bean:0129` owns the residual and states why closing it needs an enumeration of HTML
  tag names — and why "inside a container" is not the same set as "not rendered as a
  heading", since a `#` heading inside `<details>` with blank lines around it renders as a
  heading. What the rule does close is the shape it was written for: pasted check output at
  column zero is neither a heading nor a table row, wherever it is pasted. The three
  conditions above do not reach it either: the container stands inside `## Evidence`, under a
  heading with content, which is where evidence belongs.

  **`At column zero` was a qualifier with a price, and `bean:0121` paid it off.** A citation
  used to be read from the whole of a site line, so the same output pasted into the evidence
  **cell** of a row was read: the cell is not at column zero, but the row around it is
  row-shaped. A row reading `| 2 | two | FAIL check 14 …: criterion 3 is not answered in the
  evidence |` answered criterion 3, and a three-criterion bean whose table numbered only rows
  1 and 2 closed green. Nothing there was written by hand — the tool's own stdout arriving
  through the site the rule kept — so it was laundering of exactly the kind `bean:0093`
  closed, reached by a different route. The cell condition above closes it, and the rejection
  is pinned as a verdict in `tools/docs-lint-test.sh` where the acceptance used to be.
  **Quote a transcript inside a fence, never inside a cell** stands unchanged as advice: a
  fence is an entry and is not a citation site, so the same paste under one answers nothing.

  A **heading** here is an ATX heading: `#` characters at the start of the line. A
  **bold line** is not one — `**Criterion 2** — …` is running prose to the analyser and to
  CommonMark alike — and neither is a **Setext** heading, a line underlined with `=` or `-`,
  which this analyser has never tracked. Both are named because authors write the first: of
  the 143 lines in `.beans/` that lost citation-site status at `bean:0093` while carrying a
  matcher hit, 131 are running prose (the intent), **10 are bold pseudo-headings**, 2 are
  ordered-list items, and none is a bullet or a Setext heading. Write `### Criterion 3`.

  The same rule refuses a **mention** — a sentence about a criterion number that was never
  meant to answer it — for the same reason and without a second mechanism: it too is running
  prose. That is a narrowing with a measured cost, not a free one. Two beans `completed` on
  `main` cite criteria only from prose and would be reported unanswered if they closed again;
  check 11 freezes them and check 14 never re-reads a bean the base already closed, so
  neither fails today, and both are named with the measurement in `bean:0093`.

  A criterion whose evidence is a section that never names it is unanswered however long that
  section is, because `adr:0005-evidence-lives-in-the-work-item#evidence-home` puts the
  evidence beside the criterion it satisfies and a reader must be able to find the pairing.
  **The converse is now checked, and recommending the sub-heading above is why it had to
  be.** The citing heading used to be required to be neither inside the evidence region nor
  followed by anything: `### Criteria 1-5` as the whole of a five-criterion bean's
  `## Evidence` closed it, and `### Criterion 3 was not attempted` under `## Not in scope`
  answered criterion 3. The evidence-table path had `EMPTYCELL` and `HOLLOW`; the heading path
  had no analogue. The region and emptiness conditions above are that analogue — `EMPTYCELL`'s
  and not `HOLLOW`'s, since a heading over a paragraph is not hollow (`bean:0121`).

  The check reads the number, never the polarity of the claim around it, and neither
  condition changes that — both put the number somewhere polarity does not arise. So a
  heading answers the criterion it names whatever it says about it: `### Criterion 2 cannot
  be met as written`, under `## Evidence` and followed by the ruling, answers criterion 2,
  because the section under that heading is that criterion's evidence home and a ruling with
  its reason is an answer. That is the boundary of the rule, and it is accepted rather than
  closed: a heading is authored, not pasted. It is also load-bearing rather than theoretical
  — it is what fixes the two conditions at the strength they have, since the stricter form of
  either takes criterion 7 off `bean:0038`, which closed on a `## Criterion 7 is dropped,
  deliberately` section with the reason in prose under it.

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
