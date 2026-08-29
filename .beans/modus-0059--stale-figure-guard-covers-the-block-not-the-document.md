---
# modus-0059
title: The stale-figure guard covers the generated block, not the document
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0054]
---

# The stale-figure guard covers the generated block, not the document

`bean:0054` moved every volatile cost figure into a generated block and made
`tools/cost-replay.py --check` fail when it drifts. That closed the mechanism it was aimed at
and left the surrounding surface open: `--check` compares `runs.ndjson`, `baseline.md` and the
region between the bean's `cost-replay` markers, and nothing else. Bean prose **outside** the
markers, pull-request bodies, source comments and `documentation/` all pass silently.

## Observed

A reviewer appended the sentence `a derived cost of $999.99 across 12,345,678 tokens` to the
bean **outside** the markers and ran the guard:

```
cmd:      python3 tools/cost-replay.py --check
observed: (no output)
exit:     0
```

The same review found two live instances, in the same change that introduced the guard:

| where | said | artifact said |
|---|---|---|
| pull request #38's body | cache read `98.4%` | `98.19%` |
| `tools/cost_lib.py`, a comment | the floor-division bias is `2,399 micro-dollars` | a figure from a corpus two generations earlier |

Both were hand-written, both outside anything `--check` reads, and the second was in merged
code rather than a pull-request body — so it would have outlived the review that let it
through. They are fixed; the hole that let them through is not.

## Success criteria

- `--check` rejects a stale volatile figure anywhere in the files this work owns, not only
  between the markers. At minimum: the bean whole, `documentation/60-cost-model.md`, and
  `tools/cost_lib.py` and `tools/cost-replay.py`'s own comments.
- The reviewer's `$999.99 / 12,345,678` plant, added anywhere in those files, makes the guard
  exit non-zero. That plant is the regression test.
- A legitimate number is not rejected. **This is the hard part and the bean must decide the
  approach rather than assume a regex.** `2.1.236` is a CLI version, `shared/prompt-caching.md:141`
  is a line number, `0o644` is a mode, `1.25x` is a sourced price multiplier, `300k` is a
  constitutional ceiling, `2026-08-29` is a date, and `bean:0054` is an id. A naive
  "reject digit groups" check fires on all of them and gets switched off within a day.
  Candidate approaches, to be chosen and justified rather than assumed:
  - an allowlist of legitimate literals, with the guard rejecting anything else numeric;
  - a marker requirement — a figure derived from the corpus must sit inside a generated block
    or carry an explicit `<!-- volatile -->` annotation, and unannotated numerics in a fixed
    set of prose files fail;
  - comparison against the artifact — extract every number from the artifact and reject a
    prose number that *matches an old generation* rather than one that matches nothing.
    Only this one distinguishes stale from merely numeric, and it needs generation history
    the repository does not keep today.
- Whichever is chosen, the bean records why the other two were not.

## Constraint that does not change here

**Nothing runs `--check` in CI.** There is no Python gate; `./gradlew qualityCheck` covers
Kotlin and TypeScript, and wiring a Python check means editing `build.gradle.kts`, which
`bean:0054` did not own. Every guard discussed above is manual until that is closed. Widening
what `--check` inspects without also making something run it moves the failure from
"nobody checks" to "nobody runs the checker", which is not an improvement — so the gate is in
scope for this bean, or this bean states plainly that it is not and why.
