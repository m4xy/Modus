---
# modus-0112
title: A grep over chosen phrasings is evidence about wording, and is read as evidence about a rule
status: todo
type: fix
priority: high
created_at: 2026-09-03T00:00:00Z
---

# A grep over chosen phrasings is evidence about wording, and is read as evidence about a rule

`bean:0105` established that a requirement was stated nowhere normative by running one command
over the corpus and reading its output. The command was well formed, the output was real, and
the block reproduced **byte-for-byte** on the tree it was written against and again after a
two-merge rebase moved the base from `905a5f9` to `6fbf0e0`.

It was wrong on both trees, and it was wrong about the one file that decided the question.

## The instance

The sweep, as `bean:0105` carried it:

```
cmd:      grep -rl 'not firing\|does not fire\|fires on every\|never fires' .beans documentation | sort
```

Four phrasings. `documentation/50-memory-and-evidence.md` §2.2 states the rule the sweep was
looking for — all three observations, with the reason — and matches none of them:

```
cmd:      grep -c 'not firing\|does not fire\|fires on every\|never fires' documentation/50-memory-and-evidence.md
observed: 0
```

It is absent because §2.2 says the same thing in different words:

```
cmd:      grep -o 'Firing on every input is also firing' documentation/50-memory-and-evidence.md
observed: Firing on every input is also firing
```

```
cmd:      grep -o '| A mechanism observed firing, never observed silent |' documentation/50-memory-and-evidence.md
observed: | A mechanism observed firing, never observed silent |
```

`fires on every` and `Firing on every` are the same claim and different strings. That is the
whole mechanism of the failure.

## The shape, which is not specific to that sweep

**A `grep` is evidence about where a string occurs. A claim about where a *rule* is stated is a
claim about meaning.** The step between them is a paraphrase the author performed once, in their
head, when they chose the patterns — and nothing in the record carries it, so nothing can check
it. The prose credits the conclusion to *reading*, which is the right instrument; the corpus the
reading ran over was chosen by the grep, which is not.

It fails **silently and in one direction**:

| outcome | what the reader sees | caught by |
|---|---|---|
| a false positive — a file matches and does not state the rule | a file in the list that reading discards | the reading, which is why authors trust the method |
| a false negative — a file states the rule and does not match | nothing at all | nothing |

The output of a sweep that missed everything and a sweep that missed nothing are the same kind
of object: a plausible list of real files. There is no `0` to notice, no error, and no line
saying which corpus was actually examined. `bean:0105`'s list was **entirely correct** — every
file in it genuinely did support the conclusion — and the conclusion was false anyway.

## Being reproducible is not being correct, and this is the clean demonstration of that

Worth stating separately because the repository's evidence conventions are built almost entirely
around reproducibility, and this instance separates the two properties cleanly.

The sweep was re-run on the rebased tree because a corpus-wide fence was expected to decay, and
it had: six files became eight. The recapture was correct and necessary. **It was also beside
the point** — the file that falsified the bean was missing from both the six and the eight, and
re-running the command could never have surfaced it, because re-running a command checks that
the command still does what it did. It cannot check that the command answers the question the
prose asks of it.

So a fence can be reproducible, recaptured, correctly sorted, honestly transcribed, and wrong
about its own subject on every tree it has ever run on. `doc:50-memory-and-evidence` §2.2's
existing rows reject a figure with no command and a count with no tree; this passes both. The
command is there, the tree is there, and the defect is upstream of both.

## The connection to the negative half, which is not a coincidence

`doc:50-memory-and-evidence` §2.2 requires that a **mechanism** be observed not firing on input
that does not violate the rule, because a mechanism that fires on everything has still been
observed firing. The mirror of that applies to an evidence command and is stated nowhere: **a
search offered as evidence of absence has been observed matching, and has never been observed
matching a case it was supposed to find.** A sweep that misses everything returns a short list
and no error, exactly as a gate that fires on everything returns a rejection and no error.

That symmetry is why this is raised as its own bean rather than folded into `bean:0105`. It is a
rule about evidence commands, not about the rule `bean:0105` was chasing.

## What would have caught it, in ascending cost

- **A discrimination run.** One extra line: take a file known to state the thing, run the same
  pattern over it, and show it matches. `bean:0105`'s sweep had no such control, and `doc:50`
  §2.2 was available to be used as one on the day the bean was written.
- **Stating the search space as a claim.** "These four phrasings" is a decision with a failure
  mode; "the corpus" is not what was searched. Written down, the next reader can attack the
  patterns, which is the only part of the method that was ever weak.
- **Enumerate, then read, for a where-is-this-stated claim.** The corpus here is 19 documents.
  The sweep's own prose already said reading decides the question; a filter narrow enough to be
  wrong was doing work the reading was credited with.

None is a new mechanism. `bean:0106` extends the evidence extractor to every `cmd:`/`observed:`
block in a bean, and would run this sweep and reproduce it — which is the point: the extractor
checks that a command's output is what the bean says, and this command's output always was.

## What this bean does not claim

No estimate of how many sweeps in the corpus have the same defect. Finding out is part of the
work and not a claim made here. `bean:0105` is `completed` and is **not** to be repaired:
`adr:0005-evidence-lives-in-the-work-item#finalisation` makes a closed bean immutable, and its
record is more useful intact — it is the one worked example of the failure.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and a
cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | `doc:50-memory-and-evidence` §2.2 names the shape — a match-based search offered as evidence about where a rule is stated — with the reason it fails silently and in one direction, not only the prohibition | |
| 2 | The rule states what makes such a search admissible. A discrimination observation — the same pattern run against a case known to state the thing, shown to match — is the floor and is **not** sufficient on its own, because the author picks the control: the only control that would have failed `bean:0105`'s sweep is `doc:50` §2.2, which the author would have had to already suspect. So the rule must also say when a filter is inadmissible at all and the corpus must be enumerated and read instead — which removes the filter rather than testing it, and is the third remedy above | |
| 3 | The instance is cited to `bean:0105`'s closed record rather than restated, and that record is not edited to accommodate this (`adr:0005-evidence-lives-in-the-work-item#finalisation`) | |
