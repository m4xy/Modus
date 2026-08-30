---
# modus-0099
title: Fence parity and the citation matcher compose into a hole neither of them owns
status: todo
type: fix
priority: high
created_at: 2026-08-30T00:00:00Z
blocked_by: [modus-0063]
---

# Fence parity and the citation matcher compose into a hole neither of them owns

Two defects in check 14 have been recorded separately and each has been reasoned about
separately. `bean:0063` owns the fence classifier: a line-initial marker that is content
inverts the inside/outside sense of every line after it. `bean:0061` owns the citation
matcher: any line outside a fence bearing `criterion N` answers criterion N, and it cannot
tell a citation from a mention.

**Composed, they are worse than either.** In this repository a bean's evidence is a
transcript of `docs-lint` output, and that output is this check's own
`criterion N is not answered` message. Invert the parity and that transcript stops being
code and becomes prose. The matcher then reads it, and a range citation such as
`criteria 1-11` answers eleven criteria in one line.

> **A bean is one stray line-initial fence marker away from closing every criterion it
> numbers, on evidence it never recorded.**

Neither half's bean states this. `bean:0063`'s treats parity as a parse defect and measures
it against classification. `bean:0061`'s treats the matcher as too loose and measures it
against mentions in authored prose. The interaction — one edit, total suppression — belongs
to neither and so was owned by nobody.

## Observed

### The severe form: one marker, and the bean closes green

Planted on `.beans/modus-0033`, a `status: todo` bean, by flipping its status to `completed`
and appending five numbered criteria and an `## Evidence` section holding a `### ` entry and
one fenced transcript. Reverted with `git checkout -- .beans`; `git status --porcelain`
empty after each run. The control and the plant differ by **one line**.

```
control:  five numbered criteria; `## Evidence` holding `### The run` and a balanced fenced
          transcript reading
            FAIL check 14 .beans/modus-0033: criteria 1-5 are not answered in the evidence
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 3 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 4 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 5 is not answered in the evidence; …
          docs-lint: 5 failure(s).
exit:     1

planted:  the same file with ONE line-initial fence marker added inside the evidence
          section, above the transcript
observed: docs-lint: OK — 19 documents, 106 anchors, 923 references, 65 beans, 28 graph
          edges, 19 selectable, 65 bean ids, 1 introduced, 68 on origin/main, 1 closing
          transitions, 5 criteria checked, 0 unnumbered.
exit:     0
```

`5 criteria checked` and **exit 0**. Every criterion the bean numbers is closed by a line
stating that none of them is answered. The `OK` line's counts — this check's own vacuity
assertion — report the criteria as examined, because they were: they were examined and found
answered.

### The same shape, occurring by accident, on a real bean

Reproduced on `.beans/modus-0087`, an unmerged bean raised this sprint whose subject is check
14's evidence conditions and whose text therefore quotes check 14's output. One line-initial
marker inserted after its H1, nothing else changed:

```
control:  modus-0087 flipped to `completed`, nothing else
observed: FAIL check 14 …: closes with no evidence section; …
          FAIL check 14 …: criterion 1 is not answered in the evidence; …
          FAIL check 14 …: criterion 2 is not answered in the evidence; …
          FAIL check 14 …: criterion 3 is not answered in the evidence; …
          FAIL check 14 …: criterion 5 is not answered in the evidence; …
          FAIL check 14 …: criterion 6 is not answered in the evidence; …
          docs-lint: 6 failure(s).
exit:     1

planted:  the same, with one line-initial fence marker after the H1
observed: FAIL check 14 …: closes with no evidence section; …
          docs-lint: 1 failure(s).
exit:     1
```

Six findings to one. It stays non-zero only because that bean has no evidence section at
all; a bean that has one goes green, which is the plant above. **The exit code is not the
severity — the suppression is.**

### A third instance, and nobody planted it

The control run above also shows criteria 1, 2, 3, 5 and 6 flagged and **criterion 4
absent**. Nothing was planted to cause that. `modus-0087` contains, in top-level prose, the
phrase "criterion 4's decision, and it is constrained by check 11", and the matcher marks
criterion 4 answered on that alone. A bean documenting check 14's weakness shipped with one
of its six criteria pre-closed by check 14's weakness, and no marker was needed — the
matcher half is sufficient on its own for a single criterion. This is the second such
accident found this sprint and the first found without looking for it.

## Which fix closes it — measured, and not the one to guess

The natural guess is that the citation-site requirement closes this, since the laundering
happens through a citation. **It does not.** The pasted output lands at column zero in
top-level prose, which is exactly where a citation is permitted to stand. The classifier's
**refusal** is what closes it: an odd parity leaves a block open at end of file, and the file
is refused with the line named.

The green plant above, run against four analysers:

```
cmd:      the green plant, against each analyser in turn
observed: today, on main                          docs-lint: OK      exit 0   HOLE OPEN
          the bean:0063 branch, whole             refused, fence at line 76   exit 1
          its classifier fix alone                refused, fence at line 76   exit 1
          its citation-site requirement alone     docs-lint: OK      exit 0   HOLE OPEN
exit:     0, 1, 1, 0
```

Two consequences, and the second is the reason this bean exists rather than a comment:

1. `bean:0063`'s unterminated-fence refusal **already closes this**, and the work here is to
   prove it against the composed shape, pin it, and write the interaction down. That is why
   this bean is `blocked_by: [modus-0063]`.
2. **The refusal is load-bearing and must not be removed as redundant.** A later agent
   reading that a citation must stand in top-level prose could reasonably conclude the
   refusal is belt-and-braces and drop it. The fourth row above is the measurement that says
   otherwise. Without that row this bean would have recorded the wrong fix.

## Why this was owned by nobody

Each half was analysed against the failure mode of its own mechanism. The classifier was
measured by what it classifies; the matcher by what it matches. Neither analysis asked what
the other does with its output, and the composition is where the severity is: the classifier
alone mis-parses a file, the matcher alone over-reads a mention, and together they convert a
bean's own failure transcript into its evidence.

The general form is worth stating because it is not about these two mechanisms:

> **A gate built from two mechanisms needs a plant against the pair, not one against each.**
> `doc:00-constitution#observed-failing` requires a mechanism be observed rejecting a planted
> violation. Observed separately, both halves reject their own plants. The composed plant was
> rejected by neither, and no test in either bean would have found it.

Check 14 has at least one more composition of the same shape, found separately and recorded
in the unmerged `modus-0087`: recognising an evidence table is gated on a closed vocabulary
of four column headers, and when none matches, `NOEVCOL` fires and `!noevcol` suppresses the
whole per-criterion cascade. So the real sequence is **header → numbering → citation →
cell**, four mechanisms deep, and a defect at any position hides the state of everything
behind it. The generalisation stands as written and the pairwise form understates it: a plant
must reach the **last** mechanism in the chain, and a plant that stops at the first will pass
while everything behind it is unexamined and silent.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The composed plant above is observed refused, against the merged classifier, with the message naming the fence | planted violation, reverted |
| 2 | The measurement that the citation-site requirement alone does NOT close it is reproduced, and recorded where a later agent removing the refusal would read it | test-run |
| 3 | A fixture pinning the composed shape exists in `tools/docs-lint-test.sh`, asserted as a verdict and not only as a classification | test-run |
| 4 | The accidental instance — a criterion pre-answered by prose about it, with no marker involved — is either closed or recorded as belonging to `bean:0061` | citation |
| 5 | `doc:05-authoring-for-agents#checks` states that a bean's own lint transcript is not evidence for the criteria it names | diff |
| 6 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The classifier itself (`bean:0063`) and the matcher itself (`bean:0061`). This bean owns
  neither half; it owns the fact that they compose and that the composition was untested.
- Check 6's copy of the old fence toggle, raised separately and unmerged.
- Whether an evidence cell's contents are evidence at all, raised separately and unmerged as
  `.beans/modus-0087`.
- Transcript discipline in evidence (`bean:0091`) and pull-request bodies restating evidence
  (`bean:0098`), both of which touch what a transcript is for but neither of which reads the
  analyser.
