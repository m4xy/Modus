---
# modus-0114
title: A refs omission makes a true claim uncheckable, and nothing can see it
status: todo
type: fix
priority: normal
created_at: 2026-09-03T00:00:00Z
---

# A refs omission makes a true claim uncheckable, and nothing can see it

`AGENTS.md`'s routing table binds a reviewing agent to the pull-request body, the bean it
names, and the documents in its `refs:`. The template says the same where the author fills it
in:

```
cmd:      grep -n 'reads these documents and no others' .github/pull_request_template.md AGENTS.md
observed: .github/pull_request_template.md:55:<!-- The reviewer reads these documents and no others (AGENTS.md routing, last row). -->
```

So `refs:` is not a bibliography. It is the reviewer's whole reading list, and **an omission
does not make a review wrong — it makes one claim uncheckable.** It fails silently in the
worst direction: a reviewer that follows the routing exactly has no way to notice that the
document it needed was not on the list, because the absence is not visible from anything it is
allowed to read.

## The instance

PR #45 omitted `doc:80-agent-operating-procedure` from `refs:` through four rounds.
`modus-0105:187` discharges the ninth file of a corpus sweep by asserting what `doc:80` means
by a phrase:

```
cmd:      grep -n "doc:80" .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
observed: 187:merge order so that a gate does not fire, which is `doc:80`'s sense of the phrase and not
```

Under the routing, a compliant reviewer could not check the one claim that closes that
argument. It was caught in round five by a reviewer reading outside its brief, and the merged
body records both the omission and that the claim holds when checked. **The catch was
attention. Nothing mechanical was involved and nothing mechanical would have been.**

## What is missing

Nothing verifies that every typed reference in a pull request's changed files appears in that
pull request's `refs:`. The extraction half is already written in `tools/docs-lint.sh` — the
regex that recognises a reference at full fixed width:

```
cmd:      grep -n 'REF_RE=' tools/docs-lint.sh
observed: 209:REF_RE='(doc:[0-9]{2}[a-z0-9-]*|doc:README|bean:[0-9]{4}[a-z0-9-]*|adr:[0-9]{4}[a-z0-9-]*|rule:[a-z]+/[A-Za-z][A-Za-z0-9]*)(#[a-z0-9-]+)?'
```

and the per-file-unique dedup its resolver reads:

```
cmd:      grep -n 'sort -u "$TMP/refs.tsv"' tools/docs-lint.sh
observed: 225:sort -u "$TMP/refs.tsv" > "$TMP/refs.uniq"
```

The comparison is a set difference between that output, restricted to the changed files, and
the body's four lists.

## Why it cannot be a `docs-lint` check as things stand

```
cmd:      grep -n 'decidable from repository contents alone' documentation/05-authoring-for-agents.md
observed: 200:Each check is decidable from repository contents alone.
```

A pull-request body is not repository content, and locally there is often no pull request at
all. `bean:0098` reached the same wall and refused for the same reason:

```
cmd:      grep -n 'is not in the tree' .beans/modus-0098--pull-request-bodies-restate-evidence.md
observed: 98:repository contents alone, and a pull-request body is not in the tree — locally there is often
```

So this bean chooses a home rather than assuming one. Three candidates, none free:

| home | what it costs |
|---|---|
| a CI job that reads the body through the API | it is not a `docs-lint` check and must not print on `docs-lint`'s line; it also breaks `doc:00-constitution` §7.2 step 4's promise that a green local `qualityCheck` implies a green CI |
| a step in the reviewer's own brief | not a mechanism; `doc:80-agent-operating-procedure#orchestrating` R2 already makes *the routing resolve* the orchestrator's job, so this is a strengthening of an existing duty and inherits its failure mode — attention |
| widening what `doc:05-authoring-for-agents#checks` requires of a check | changes a contract that four other beans currently rely on |

## One thing the extraction must not inherit from check 6

Check 6 skips fenced blocks, because a fence holds the literal templates an author copies
rather than live references:

```
cmd:      grep -n 'fence = !fence' tools/docs-lint.sh
observed: 212:  awk '/^```/ { fence = !fence; next } !fence { print }' "$f" |
```

A completeness check must **not** skip them. PR #45's body records three entries that reach its
reading list only because they appear inside a verbatim transcript quoted in a bean, and a
reviewer following a reference into a quoted block still needs the document it names. The two
uses diverge at exactly this point, which is why sharing `REF_RE` is safe and sharing the
extraction is not.

## What the extraction must exclude, found by running it

A document's front-matter `depends_on` names what a reference in the file may resolve to. It is
**not** a reading list (`doc:05-authoring-for-agents#front-matter`), and pulling it into `refs:`
routes the reviewer at the whole package. Over the six changed files of the change that raises
this bean, that exclusion alone is the whole difference between the two counts below:

```
cmd:      RE=<check 6's REF_RE, tools/docs-lint.sh:209>
cmd:      git diff --name-only origin/main...HEAD | xargs grep -hoE "$RE" | sort -u | grep -c .
observed: 84
cmd:      git diff --name-only origin/main...HEAD | xargs grep -hv '^depends_on:' | grep -oE "$RE" | sort -u | grep -c .
observed: 75
```

A changed file also carries references in regions the change does not touch. Those stay in —
the rule is containment and a superset is the safe direction — but the cost is real and belongs
in whatever is adopted, because a reviewer routed at forty documents holds no budget for the
six that matter (`doc:00-constitution#context-budget`).

## Scope, and what this bean does not claim

Owned: the completeness rule, its home, and whatever mechanism or refusal is adopted. Not
owned: `bean:0098`'s subject — bodies that **restate** evidence — which is the opposite defect
in the same artefact; `bean:0086`'s fence handling inside check 6; and the routing rule itself,
which is `AGENTS.md`'s.

No claim about how many merged pull requests have an incomplete `refs:`. Measuring it is part
of the work, and the figure would be a count over a set that grows.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and
a cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | Whether the omission is caught by a mechanism or by the reviewer's brief is decided, and the rejected options are recorded with what each would have cost | |
| 2 | If a mechanism is built, it is observed rejecting a planted omission — a typed reference in a changed file absent from `refs:` — and observed silent on a body whose lists are a superset (`doc:00-constitution#observed-failing`) | |
| 3 | The extraction reads references inside fenced blocks, and its divergence from check 6 is stated where the next reader of `REF_RE` will meet it | |
| 4 | The rule is stated as containment, not equality: a `refs:` listing more than the changed files cite is not a failure, and PR #45's own superset argument is cited rather than restated | |
| 5 | Whatever is adopted names what it does not cover — a reference reached transitively, through a document the changed files cite, is not in the changed files at all | |
| 6 | `./gradlew qualityCheck` green | |
