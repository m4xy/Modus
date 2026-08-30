---
# modus-0090
title: A constant in code that must match an external authority, with nothing comparing them
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# A constant in code that must match an external authority, with nothing comparing them

Why: a value is copied out of a document into source, the document is normative, the copy is
what runs, and **no mechanism ever compares the two again**. The copy is right on the day it
is written and silently wrong afterwards. Nothing goes red, because nothing is looking.

This is stated as a class rather than as its first instance deliberately. The specific
symptom found in `bean:0069` is a price table; the defect is not about prices.

## Why the existing gate looked like coverage

`e2e/tests/agent-console.spec.ts` prices the mock's tokens from `BASE_RATES_UPM` and then
asserts a ratio derived from `BASE_RATES_UPM`. It compares the code **to itself**.

That distinction — **internal consistency** versus **agreement with an authority** — is the
whole of this bean, and it is why the test reads as coverage to anyone skimming it. What the
test really covers is one entry moving *relative to the others*: an Opus 5 entry of $15/$75
would read 15x rather than 5x and fail. That is a property of the arithmetic, and it is
stated here as one. It cannot ever catch a table where every entry is consistently stale,
because staleness moves nothing relative to anything the test can see. A self-referential
assertion is real coverage of one property and zero coverage of the other, and the failure is
reading the first as the second.

**The $15/$75 error is real, and this test is its regression guard.** An earlier revision of
this section said the test "once caught" it; a *reviewer* caught it, and the test was written
afterwards. The record is in `bean:0002`, under `## Review cycle 1`:

```
cmd:      sed -n '130,144p;149,153p' .beans/modus-0002--backoffice-foundation.md
observed: ### 1. Pricing was 3x over on the console's default model

          `PRICING` claimed $15/$75 per MTok for `claude-opus-5`. The list price is
          $5/$25, so the headline cost figure on the cost-conscious screen read 3x high.
          Every row was re-checked against the published Anthropic model pricing (the
          `claude-api` reference, 2026-08-28), not from memory:

          | Model | Was | Now | Note |
          | --- | --- | --- | --- |
          | `claude-opus-5` | 15 / 75 | **5 / 25** | wrong; the default model |
          | `claude-sonnet-4-5` | 3 / 15 | *removed* | two generations superseded |
          | `claude-sonnet-5` | — | **3 / 15** | introductory $2/$10 lapses 2026-08-31 |
          | `claude-haiku-4-5` | 1 / 5 | 1 / 5 | already correct |

          The list price is encoded rather than the introductory price, so the counter

          *Load-bearing:* `agent-console.spec.ts` now runs the identical session on three
          models and asserts the cost ratios, which are exactly the price ratios — Opus 5
          is 5x Haiku 4.5, Sonnet 5 is 3x. Restoring 15/75 makes that ratio 14.95 and the
          test fails.
```

The ranges are the ones `bean:0103` uses, and they are wider than an earlier revision's
`'130,136p;150,153p'`, which quoted the `| claude-opus-5 | 15 / 75 |` row while emitting
neither it nor the table it sits in. The block above is that command's output, pasted
unedited rather than reconciled — including the sentence `sed` truncates mid-sentence at 144,
which is what the command actually prints. Line 144 is emitted whole; it is the sentence that
is cut off, because 145 is outside the range.

So the test's justification is **stronger** than a self-referential assertion usually deserves:
it is a guard written in response to a specific, documented defect, not a nice-to-have.

**Why `git log` says otherwise, and why that is not this bean's finding.** Checked against
committed history the incident looks fabricated, and readers concluded it had never happened.
It had; the fix landed in review, before the merge, so committed history never held it.
`bean:0103` owns that account whole — the pre-merge and squash-merge invisibility, `git log
-S`'s last-wins argument handling, the hazard of an `observed:` block that lands inside its own
searched corpus together with the mitigations available, and why
`adr:0005-evidence-lives-in-the-work-item` makes `.beans/` the store that held the answer.
`bean:0100` carries the same episode from the other side, as a claim arriving with confidence
already attached, and its own control. An earlier revision of this section restated all of that
at length because it predated both beans; it is removed rather than reconciled
(`doc:05-authoring-for-agents#one-fact-one-place`).

What this bean keeps is only what its thesis needs: **the $15/$75 defect was real, and this
test is the guard raised in response to it** — which is why the self-referential assertion is
worth keeping rather than deleting, and why everything below is about what that assertion
cannot do rather than about whether it should exist at all.

## The three instances found so far

All three are the same absence seen from different ends. `bean:0069` fixed the third locally
and could not fix the first two.

| # | instance | state |
|---|---|---|
| 1 | `BASE_RATES_UPM` in `backoffice/src/agent/transport.ts` versus `doc:60-cost-model#price-book`. Sonnet 5's introductory rate lapses after 2026-08-31; on 2026-09-01 the table is 33% low. | **open.** Nothing detects it. The KDoc says so plainly rather than promising loudness no mechanism delivers. |
| 2 | The same table versus `tools/cost_lib.py`'s `BASE_RATES_UPM`. Two halves of one seam, two hand-maintained copies of the same rates, no comparison. They disagreed on Sonnet 5 by 50% until `bean:0069`. A third disagreement is about **whether a price is returned at all**, not about its value: `cost_lib.normalise_model` strips a dated suffix (`^(.*)-\d{8}$`) and therefore prices `claude-opus-5-20260101`, while `isPricedModel` is a bare own-property check and rejects the same id. Neither half ever returns a *wrong* rate; they disagree on an id shape a real producer emits, and the TypeScript half silently declines to price it. | **open.** They agree today by inspection, which is not a mechanism, and the suffix rule is not shared at all. |
| 3 | An unpriced model id defaulting silently. `?? BASE_RATES_UPM['claude-sonnet-5']` would price a `claude-opus-4-8` run 60% low with nothing on screen, defaulting onto the one entry whose rate has an expiry. | **closed locally** by `bean:0069`: `totalCostUsd` in `useAgentSession.ts` returns `null` when `isPricedModel` rejects the id, so the console shows no figure — mirroring `cost_lib.normalise_model`, which raises. An earlier revision cited `costMicros`; the outcome is implemented and observable, but `costMicros` is exported and called by nothing, so the citation pointed at dead code while the live path ran through `totalCostUsd`. |

Note the scale mismatch that makes instance 1 easy to under-rate: the TypeScript table carries
**3 models**, `cost_lib` prices **8**. It is not a price book and must not be read as one.

## The hard part, stated rather than hand-waved

**The authority is currently prose in a Markdown document.** `doc:60-cost-model#price-book` is
a human-readable table with parenthetical effective dates in the cell text — "$3.00 (intro
$2.00 **through 2026-08-31**)". A mechanism that compares code to *that* is a Markdown-table
parser plus a date parser working on prose that was never written to be machine-read, and it
breaks the first time someone rewords a cell. That is a genuinely unsolved problem here, not a
missing afternoon's work, and a linter built against prose would be a fourth thing that looks
like coverage.

**So the likely resolution is not a linter.** `doc:60#price-book` §2.1 already says the
authoritative price book belongs at `domains/<domainId>/cost/price-book.md` — a document in
the flat-file store, with `effectiveFrom`/`effectiveTo` as data. Once it is data, the right
answer is that the rate table is **derived from it rather than compared to it**, and the
divergence becomes unrepresentable instead of merely detectable. Deleting the constant beats
watching it. This bean may well close by that route.

Which leaves an interim question worth answering explicitly rather than deferring: whether an
effective date that has *lapsed* can be caught more cheaply than a full comparison, since that
is the failure with a known date attached and does not need the authority parsed at all.

## The sharpest form: the test resists its own fix

`e2e/tests/agent-console.spec.ts` asserts `toBeCloseTo(5, 1)` and `toBeCloseTo(2, 1)` —
**hardcoded literals**, not values derived from `BASE_RATES_UPM`. Follow what that means on
**2026-09-01**, the first day the standard rate applies — the introductory rate runs *through*
2026-08-31, so the last day it holds and the first day it does not are different days, and an
earlier revision of this section named the wrong one while the instance table above named the
right one:

| what happens | what the gate does |
|---|---|
| nobody touches the table; it is now 33% low | **green.** Staleness moves no ratio. |
| someone notices and correctly sets Sonnet 5 to $3/$15 | **red.** The ratio becomes 3 and the assertion still says 2. |

So the suite is green while the code is wrong and red when someone makes it right. **The test
does not merely fail to detect the defect; it penalises the fix.** A developer who corrects
the rate gets a failing build and the fastest way out is to revert the correction — the gate
actively argues for the stale value.

This is the sharpest statement of the thesis available, sharper than the self-reference
framing above, because self-reference only explains why the test *cannot help*. This explains
why it *hurts*: a self-referential assertion over a constant does not sit at zero coverage, it
sits at negative coverage the moment the constant is meant to change. Anywhere a test asserts
a literal that was computed by hand from a constant, the same inversion is waiting.

The fix is not to update the literal. It is to derive the expectation from the same source the
code uses — at which point the assertion covers the arithmetic honestly and stops making any
claim about the rate, which is the authority's job and this bean's subject.

## A finding recorded against this bean, not acted on here

**This bean's own evidence block cites line numbers into another file.** The `sed -n
'130,144p;149,153p'` above addresses `bean:0002` by line, and `bean:0069` records the opposite
convention for exactly this reason: *a line number is a claim about a file's current shape, and
any edit above it falsifies the claim without touching what it describes.*

That is not a hypothetical here — it is the mechanism that produced the defect the block was
just fixed for. The ranges were narrowed at some point to `'130,136p;150,153p'`, the quoted
output was kept, and the quoted `| claude-opus-5 | 15 / 75 |` row then sat outside every range
the command printed. Nothing noticed, because nothing compares a `cmd:` to its `observed:`
outside `bean:0069`'s criteria table — which is `bean:0106`'s subject, and this block is one of
its three worked instances.

Not fixed here because the alternative is not obvious. `bean:0069`'s rule assumes a *content*
match is available — a `grep` for a phrase that does not wrap — and what this block needs is a
contiguous region including a table, which no single `grep` reproduces. The candidates are a
`sed` between two matched patterns, or splitting the citation into several greps and losing the
region. Choosing between them is a convention question for whichever bean owns `bean:0091`'s
transcript rules, not a repair to make inside a review round.

## Success criteria

- The class is named somewhere normative, with the internal-consistency versus
  agreement-with-an-authority distinction stated, so the next self-referential test is
  recognised as one before it is trusted. A worked instance exists in `bean:0069`.
- Instances 1 and 2 are closed by the same mechanism, or the bean records why they cannot be
  and what would.
- Whatever is built is **observed failing** against a planted stale rate before it is claimed
  (`doc:00-constitution#observed-failing`), and observed **not** firing against a current one.
  A comparator that fires on every run is not coverage, and one that never fires has not been
  shown to work.

  The second half of that is **not normative anywhere**. §9.1's MUST bullets require only the
  planted violation; the requirement to observe a mechanism *not* firing on a clean input lives
  in this bullet and in `bean:0069`'s detector evidence, and `bean:0105` records the sweep that
  found no third statement of it, along with the adjacent vacuity requirement §9.1 does carry
  and why that one does not cover this. Two unmerged beans agreeing is a convention, not a
  rule. `bean:0105` owns getting it into §9.1; until that lands, this criterion is stricter
  than the constitution it cites.
- If the resolution is derivation from `domains/<domainId>/cost/price-book.md` rather than
  comparison, that is a valid closure and the constant is deleted rather than gated.

Related: `bean:0069` (which found all three instances and closed the third), `bean:0016`
(spend attribution, which will consume whatever the price book becomes), `bean:0054` (the cost
baseline and its `--check`, itself a comparison against recorded inputs rather than against an
authority).
