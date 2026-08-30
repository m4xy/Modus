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
cmd:      sed -n '130,136p;150,153p' .beans/modus-0002--backoffice-foundation.md
observed: ### 1. Pricing was 3x over on the console's default model
          `PRICING` claimed $15/$75 per MTok for `claude-opus-5`. The list price is
          $5/$25, so the headline cost figure on the cost-conscious screen read 3x high.
          | `claude-opus-5` | 15 / 75 | **5 / 25** | wrong; the default model |
          [...]
          *Load-bearing:* `agent-console.spec.ts` now runs the identical session on three
          models and asserts the cost ratios [...] Restoring 15/75 makes that ratio 14.95
          and the test fails.
```

So the test's justification is **stronger** than a self-referential assertion usually deserves:
it is a guard written in response to a specific, documented defect, not a nice-to-have.

**How two readers concluded it never happened, which is the part worth keeping.** Checked
against `git log`, the claim looks fabricated:

```
cmd:      git log --all -S "opus-5" --oneline -- backoffice/src/agent/transport.ts
observed: 10af4f7 feat(backoffice): scaffold the backoffice with a tokenised design system
cmd:      git show 10af4f7:backoffice/src/agent/transport.ts | grep -n "opus-5"
observed: 94:  'claude-opus-5': { inputPerMTok: 5, outputPerMTok: 25 },
cmd:      git log --all -S "outputPerMTok: 75" --oneline -- backoffice/
observed: no output
```

Exactly one commit has ever touched the line carrying the Opus 5 rate, and it introduced it at
$5/$25 — so in committed history that rate has never changed at all.

Three things about that transcript are deliberate, and each replaces something that was wrong
in an earlier revision of this bean (a separate bean carries the general forms):

- **Quantifier, not count.** It said "three commits have ever touched the file". That was six
  within a day, because this branch's own commits touch it. A claim quantified over a growing
  set is stale on arrival; a claim about the set is not.
- **Scoped with `-- backoffice/`.** The unscoped search asserted a string's absence while
  *recording that string* in this bean — so once committed, the command returned the very
  commit that wrote the transcript. The observation was invalidated by the act of recording it.
- **One `-S`, not two.** The original used `-S "75_000_000" -S "outputPerMTok: 75"`, read as
  "either string". `git log -S` is **last-wins**: the second replaces the first, so only
  `outputPerMTok: 75` was ever searched and `75_000_000` was discarded with no error and no
  warning.

The reading of these searches is wrong even though each is individually correct — and note the
inference was **overdetermined**. Fixing the `-S` bug and rerunning would still return nothing,
because the real mechanism is the one below; the visible defect's repair would have raised
confidence in the wrong answer. The defect was **fixed in review
cycle 1 of PR #3, before the merge**, so only the corrected value was ever committed. `bean:0002`
is that pull request's bean and `10af4f7` is its commit — the same commit both readers used as
proof of absence.

**A defect caught in review is invisible to committed history by construction.** Under
squash-merge that is the normal case, not an edge one: the entire class of defects that review
catches leaves no trace in `git log`, which is *why* `adr:0005-evidence-lives-in-the-work-item`
puts the record in `.beans/`. Two independent readers searched the code history, found nothing,
and concluded the event did not occur — without searching the store this project designates for
exactly this. Absence of evidence in one store is not absence of the event, and the store that
was skipped is the one cited in every other paragraph.

That belongs in *this* bean because it is the same failure at one remove: **`git log -S` was
trusted as an authority on a question it cannot answer.** Not a stale constant this time but a
stale mental model of where the truth is kept — and, as with the rate table, nothing announced
the mismatch.

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

## The sharpest form: the test resists its own fix

`e2e/tests/agent-console.spec.ts` asserts `toBeCloseTo(5, 1)` and `toBeCloseTo(2, 1)` —
**hardcoded literals**, not values derived from `BASE_RATES_UPM`. Follow what that means on
2026-08-31, when Sonnet 5's introductory rate lapses:

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
