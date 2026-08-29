---
# modus-0060
title: The cursor refusal is all-or-nothing on count, not proportional
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0054]
---

# The cursor refusal is all-or-nothing on count, not proportional

`bean:0054`'s `advance_cursor` in `tools/cost-record.py` stops the recorder silently re-billing
a whole run into an append-only money log when the delta cursor goes missing. It refuses only
when the timestamp fallback would select **every** message. One message surviving at or before
the previous record's `endedAt` is enough to turn a near-total re-bill into a `timestamp-fallback`
partial that bills almost the whole run again.

## Observed

A reviewer drove the boundary directly, through the real hook entry point:

```
setup:    seed a record covering 5 messages, then rewrite the transcript with new message
          ids such that ONE message is at-or-before the previous record's endedAt and FOUR
          are after it
cmd:      <Stop payload> | python3 tools/cost-record.py
observed: refusal did NOT fire
          billingBasis = timestamp-fallback
          messages     = 4        (of 5 — the run is billed again almost in full)
```

The same review confirmed the other three paths are correct and that the failure here is
bounded rather than silent: the record carries `billingBasis: timestamp-fallback` and a
`billingNote` admitting the record may overlap or undercount, and the basis is machine
filterable, so a consumer can exclude it. It is a soft edge on a hard guard, not a hole in it.

## Success criteria

- The refusal is **proportional**, not all-or-nothing: the recorder refuses when the timestamp
  fallback would select most of the run, not merely all of it. The bean picks and justifies the
  threshold — a fraction of messages, a fraction of tokens, or a fraction of the previously
  recorded spend — rather than defaulting to a round number.
- Billed tokens are probably the better basis than message count: five messages can differ by
  two orders of magnitude in cost, and it is the money the guard exists to protect.
- The reviewer's reproduction above is a test: 1 of 5 surviving must refuse under the new rule
  if 4 of 5 would be re-billed.
- A genuine small delta after a compaction still records, because refusing every partial makes
  the recorder useless on exactly the long sessions it matters most for.
- `billingBasis` keeps its existing values so a consumer written against the current log does
  not break; a new value is added rather than an existing one repurposed.

## Not in scope

The deeper fix is for the recorder not to depend on a cursor it can lose — a per-run sequence
the harness supplies, or billing straight from the hook payload. Neither exists: no hook event
carries usage at all (`doc:60-cost-model` §3.2.1), which is why the transcript is read in the
first place. Recorded so the next reader does not re-derive it.

## Constraint that does not change here

**Nothing runs the recorder's self-test in CI.** There is no Python gate, so
`python3 tools/cost-record.py --self-test` and any test added for this threshold are manual
until that is closed. Same standing constraint as `bean:0059`.
