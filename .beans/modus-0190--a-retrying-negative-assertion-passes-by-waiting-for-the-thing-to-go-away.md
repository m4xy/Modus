---
# modus-0190
title: A retrying negative assertion passes by waiting for the thing to go away
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# A retrying negative assertion passes by waiting for the thing to go away

Why: Playwright's web-first assertions retry until they hold. `toHaveCount(0)`,
`toBeHidden()`, `not.toBeVisible()` and `toBeEmpty()` therefore do not assert *this never
appeared*; they assert *this is gone by the deadline*. Applied to anything that removes
itself — a toast, a transient banner, a spinner — the assertion sits through the thing it
was written to forbid, watches it dismiss on schedule, and reports a pass.

This is the `doc:00-constitution#observed-failing` shape exactly: a check that examined
nothing and a check that passed print the same thing. It cost a plant-and-revert round on
`bean:0141` and would have shipped a green suite over the defect it was written to catch.

## Observed

Against the pre-fix `Settings.tsx`, whose **Save budget** handler raised a success toast
and issued no request. `DISMISS_AFTER_MS` is `6000` in `backoffice/src/ui/Toast.tsx`; the
suite's `expect` timeout is `7_000` in `e2e/playwright.config.ts`.

```
assertion: await expect(page.getByText('Budget saved')).toHaveCount(0);
expected:  fails — the toast this asserts against is on screen
observed:  PASSED, after ~6s, against the code that raises the toast
           (the whole test then failed one line later, on toBeDisabled, which is
            how the vacuity was noticed at all — had the fix been only the toast,
            the round would have reported a clean plant)
```

The same page, probed rather than asserted, in the same build:

```
cmd:      probe spec — click, then read the count at four moments
observed: PROBE disabled = false
          PROBE count immediately = 1
          PROBE count after 50ms = 1
          PROBE count after 550ms = 1
          PROBE region = "Budget saved\n\nModus Core will refuse new runs above $750."
```

So the element was present within 50ms of the click and stayed for six seconds. The
retrying assertion had every opportunity to see it and passed regardless, because passing
is what it does once the count reaches zero — however it got there.

The non-retrying form fails as it should, same build, same click:

```
assertion: expect(await page.getByText('Budget saved').count()).toBe(0);
observed:  Error: expect(received).toBe(expected)
           Expected: 0
           Received: 1
```

`e2e/tests/settings.spec.ts` now carries the non-retrying form and the reason, but only
there. Nothing stops the next spec reaching for `toHaveCount(0)` over a toast.

Success criteria:

1. `doc:35-testing` states the rule: a negative assertion over a self-removing element is
   non-retrying, and a retrying one is only ever a statement about the deadline.
2. Something mechanical rejects a retrying negative matcher applied to the toast region or
   to toast text in `e2e/**` — an ESLint rule with a selector, or a lint step — and is
   observed rejecting a planted one.
3. The existing `e2e/**` specs are swept for the shape; each hit is either converted or
   recorded as unaffected with the reason.

Found while implementing `bean:0141`. Raised rather than fixed there: the rule belongs in
`documentation/`, which that change does not own.
