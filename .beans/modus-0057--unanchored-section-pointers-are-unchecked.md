---
# modus-0057
title: A section pointer written as prose is never resolved, and nothing says so
status: todo
type: fix
priority: normal
order: AO
created_at: 2026-08-29T00:00:00Z
---

# A section pointer written as prose is never resolved, and nothing says so

`docs-lint` check 6 resolves a reference's **document** and, if one is written, its
**anchor**. The anchor is optional. So a pointer that names a section in prose — ``
`doc:00-constitution` §7.4 `` — has only its file half checked, and a pointer written as a
bare filename — `` `00-constitution.md` §7.4 `` — is not a reference at all and has neither
half checked. Both forms are green with a section that does not exist, and both are green
with a *document* that does not exist in the second case.

Found while fixing `doc:80-agent-operating-procedure` §0 in PR #36: I wrote `` `doc:80` §0 ``
where the anchor form was `doc:80-agent-operating-procedure#orchestrating`, and docs-lint
passed. I recorded in that PR that the shortened form "does not resolve"; that was wrong and
is corrected below — it resolves fine. The defect is not under-qualification. It is that the
section half of a pointer is invisible to the checker in the one form the package uses most.

## Observed

Four plants in `documentation/80-agent-operating-procedure.md`, each run and reverted:

```
cmd:      plant `doc:00-constitution` §99  (prose section marker, section does not exist)
observed: docs-lint: OK — 19 documents, 105 anchors, 804 references, ...

cmd:      plant `doc:00-constitution#section-99`  (same claim, anchor form)
observed: FAIL check 6  documentation/80-agent-operating-procedure.md:
          'doc:00-constitution#section-99' — documentation/00-constitution.md
          does not provide '#section-99'
          docs-lint: 1 failure(s).

cmd:      plant `doc:00#independent-review`  (under-qualified id, anchor kept)
observed: docs-lint: OK  — and correctly resolved: check 6 globs documentation/00*.md
          and compares against the target's own `id:`, so the short id is not the bug

cmd:      plant `99-does-not-exist.md` §6  (bare filename, document does not exist)
observed: docs-lint: OK — the string never matches REF_RE, so nothing looked
```

The same claim about the same document is caught in one spelling and silent in the other.

## Scope, counted

Cross-document section pointers in `documentation/*.md`:

| form | count | what is checked |
|---|---|---|
| `` `doc:NN-slug` §N `` | 11 | the document; not the section |
| `` `NN-name.md` §N `` | 96 | nothing |
| `doc:NN-slug#anchor` | — | both, by check 6 |

107 pointers whose section half no mechanism has ever verified. Within-document `§N`
(`doc:00-constitution` §7.4 citing "§9.1", this file's own `§9.6`) is a separate question
this bean must answer rather than assume — see below.

## Why it is worth a bean rather than a note

**The failure is silent and in the direction of least resistance.** `adr:0003` caps a
document at 500 lines; `doc:00-constitution` is at exactly 500. Under that pressure the
cheap edit is to shorten a citation, and shortening `#orchestrating` to `§0` converts a
checked claim into an unchecked one with no signal. The pressure is permanent and it is
concentrated on the document where a broken pointer costs the most.

**It undercuts the rule it is supposed to serve.** `doc:05-authoring-for-agents` §4 requires
every non-owning mention of a fact to be a reference rather than a copy, and PR #36 fixed
six instances of that defect in one section. Deferring to an anchor is only safe if a broken
deferral is loud. Right now the cheapest way to comply with one-fact-one-place is also the
cheapest way to leave a pointer nobody will notice has rotted — and a section number is
exactly what rots, because `doc:README#changing-this-package` forbids reallocating section
numbers precisely because records cite them.

## What this bean must decide, not assume

Check 6's behaviour looks **deliberate**, and nothing in the tree says whether it is. The
anchor group in its regex is written optional, and its header comment explains a different
deliberate omission (a `bean:NNNN` placeholder must not resolve). So the first question is
not "how do we fix it" but:

1. Is a prose `§N` a legitimate reference form that should be resolved, or should it be
   banned in favour of an anchor? `doc:05-authoring-for-agents` §2 defines the reference
   grammar and is the place that answers this.
2. Is a bare `` `NN-name.md` `` a reference at all? 96 of them read as one. If they are, the
   grammar has a second spelling; if they are not, 96 mentions need rewriting.
3. Does within-document `§N` count? Both `doc:00-constitution` and this document use it
   pervasively and it is the form that reads best inline. A rule that forbids it is a large
   rewrite; a rule that exempts it leaves the cheapest cross-document shortcut one character
   away from the exempt form.
4. Whatever is decided, is it mechanically checkable, and observed failing before it is
   claimed (`doc:00-constitution#observed-failing`)? A rule about references that only
   review enforces is the defect this bean is about, one level up.

## Not in scope

Implementing the check. This bean states the gap; the mechanism belongs to whoever works it,
after question 1 is answered. PR #36 deliberately did not widen to include it.
