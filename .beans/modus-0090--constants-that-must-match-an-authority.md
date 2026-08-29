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
whole of this bean, and it is why the test reads as coverage to anyone skimming it. The test
is not useless: it once caught Opus 5 priced at $15/$75, because a wrong entry moved a ratio
*relative to the other entries*. It cannot ever catch a table where every entry is
consistently stale, because staleness moves nothing relative to anything the test can see. A
self-referential assertion is real coverage of one property and zero coverage of the other,
and the failure is reading the first as the second.

## The three instances found so far

All three are the same absence seen from different ends. `bean:0069` fixed the third locally
and could not fix the first two.

| # | instance | state |
|---|---|---|
| 1 | `BASE_RATES_UPM` in `backoffice/src/agent/transport.ts` versus `doc:60-cost-model#price-book`. Sonnet 5's introductory rate lapses after 2026-08-31; on 2026-09-01 the table is 33% low. | **open.** Nothing detects it. The KDoc says so plainly rather than promising loudness no mechanism delivers. |
| 2 | The same table versus `tools/cost_lib.py`'s `BASE_RATES_UPM`. Two halves of one seam, two hand-maintained copies of the same rates, no comparison. They disagreed on Sonnet 5 by 50% until `bean:0069`. | **open.** They agree today by inspection, which is not a mechanism. |
| 3 | An unpriced model id defaulting silently. `?? BASE_RATES_UPM['claude-sonnet-5']` would price a `claude-opus-4-8` run 60% low with nothing on screen, defaulting onto the one entry whose rate has an expiry. | **closed locally** by `bean:0069`: `costMicros` returns `null` and the console shows no figure, mirroring `cost_lib.normalise_model`, which raises. |

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
- If the resolution is derivation from `domains/<domainId>/cost/price-book.md` rather than
  comparison, that is a valid closure and the constant is deleted rather than gated.

Related: `bean:0069` (which found all three instances and closed the third), `bean:0016`
(spend attribution, which will consume whatever the price book becomes), `bean:0054` (the cost
baseline and its `--check`, itself a comparison against recorded inputs rather than against an
authority).
