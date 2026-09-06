---
# modus-0190
title: A negative assertion passes when the thing is not there yet, or no longer there
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# A negative assertion passes when the thing is not there yet, or no longer there

Why: Playwright's web-first assertions retry until they hold. `toHaveCount(0)`, `toBeHidden()`,
`not.toBeVisible()` and `toBeEmpty()` therefore never assert *this was never here*; they assert
*this is absent at some point before the deadline*. There are two ways to satisfy that without
the code being correct, and **both have been observed in this repository, six times between
them**:

| mode | how it passes | lintable by matcher? |
|---|---|---|
| **not-yet-there** | the assertion runs before the thing it forbids has rendered | **no** |
| **no-longer-there** | the assertion waits out an element that removes itself | yes |

The first version of this bean carried only the second mode, because that is the one that cost
a plant-and-revert round. That was the narrower half: its proposed remedy — a lint rule scoped
"to the toast region or to toast text" — catches **one** of the six instances found and none of
the other five. The mode that accounts for five of six is missing from a bean raised to prevent
it, which is the same failure the bean is about.

This is `doc:00-constitution#observed-failing` exactly: a check that examined nothing and a
check that passed print the same thing.

## Observed

### Mode A — not-yet-there (five instances)

Five absence assertions in the first draft of `e2e/tests/failure-states.spec.ts`, run straight
after `goto`. They resolved in **150–210 ms** against an empty-state sentence that does not
render until **1847 ms** — the query has to reject and its one retry has to fail first — so all
five passed against the unfixed routes they were written to catch.

```
assertion: await expect(page.getByText('No work items yet')).toHaveCount(0);
expected:  fails — the unfixed screen renders exactly this under a 500
observed:  PASSED, in ~150ms, while the screen was still a skeleton
```

The sixth instance is the one that matters most, because it is in a file nobody was editing and
it has been green in CI for the life of the branch — `e2e/tests/domain-switcher.spec.ts:25`,
in a test named *navigation reflects the permissions of the domain you are in*:

```
cmd:      plant `if (false) return null;` over backoffice/src/app/AppShell.tsx:152,
          deleting permission-based nav hiding outright, then run the file
expected: 'navigation reflects the permissions of the domain you are in' fails
observed: 5 passed (15.2s)
          the assertion resolved in 12ms, before the shell rendered any nav
```

A test named for permissions could not fail when permissions stopped being enforced. Its only
positive assertion sat two lines *below* the absence check. Fixed in this branch by putting the
positive assertion first; the class is not fixed.

### Mode B — no-longer-there (one instance)

`Settings.tsx`'s **Save budget** raised a success toast and issued no request (`bean:0141`).
`DISMISS_AFTER_MS` is `6000` in `backoffice/src/ui/Toast.tsx`; the suite's `expect` timeout is
`7_000` in `e2e/playwright.config.ts`.

```
assertion: await expect(page.getByText('Budget saved')).toHaveCount(0);
expected:  fails — the toast is on screen
observed:  PASSED, at 6398ms, against the code that raises the toast
```

Probed rather than asserted, same build:

```
cmd:      a throwaway spec that clicks and reads the count at four moments
observed: PROBE disabled = false
          PROBE count immediately = 1
          PROBE count after 50ms = 1
          PROBE count after 550ms = 1
          PROBE region = "Budget saved\n\nModus Core will refuse new runs above $750."
```

The non-retrying form fails as it should:

```
assertion: expect(await page.getByText('Budget saved').count()).toBe(0);
observed:  Error: expect(received).toBe(expected)
           Expected: 0
           Received: 1
```

### A third shape, adjacent: safe only by fixture timing

`e2e/tests/agent-console.spec.ts:293` asserts no `Streaming` badge after a refused run. It is
anchored by positive assertions, so it is not an instance — but it survives partly because the
default replay stream runs **17.3 s** against a 7 s `expect` timeout. Nothing records that
margin, and shortening the fixture would silently convert it into one. Commented in place.

## Success criteria

| # | Criterion | Evidence kind |
|---|---|---|
| 1 | `doc:35-testing` states both modes and the remedy for each: a negative assertion over a self-removing element is non-retrying; a negative assertion may not be the first assertion after a navigation | citation |
| 2 | An AST rule rejects an absence assertion that is not preceded, in the same test body, by a positive assertion on a terminal state — the structural rule, which is the only one that reaches mode A | test-run |
| 3 | A matcher+selector rule rejects a retrying negative matcher over a self-removing element — the toast region and toast text at minimum | test-run |
| 4 | Both rules are observed rejecting a planted violation, and the plant is confirmed applied before the run is read | test-run |
| 5 | `e2e/**` is swept for both shapes; every hit is converted, or recorded as unaffected with the reason and the measured margin | test-run |

Criterion 2 is the one that would have caught all six. Criterion 3 alone catches one.

Note for whoever takes this: `toBeHidden()` and `not.toBeVisible()` are the same trap under
different names, and `toBeEmpty()` on a live region is mode B wearing mode A's clothes — the
region element exists from first paint, so it is empty both before the toast and after it.

Found while implementing `bean:0140` and `bean:0141`, and widened after independent review of
that PR re-planted both false negatives and found the sixth instance. Raised rather than fixed
there: the rule belongs in `documentation/` and the lint rule in the shared config, neither of
which that change owns.
