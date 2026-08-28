---
id: "0002"
title: Backoffice foundation
status: in-review
kind: epic
domain: modus
created: 2026-08-28
---

> **Frontmatter is provisional.** The bean schema is being ratified separately
> (work item 0006). The keys above are the current draft; expect this file to be
> re-stamped once the schema freezes, and treat any mismatch as the schema's
> problem rather than this bean's.

## Scope

The human sanitisation layer over the Modus data model. Everything under
`backoffice/` and `e2e/`; no Gradle, Kotlin, CI or documentation changes.

- Vite + React + TypeScript application with a strict compiler configuration
  (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- A design system: tokens for colour, type, spacing, radius, elevation and
  motion, driving light and dark themes, plus twelve primitives.
- An app shell built around the domain switcher, since `/domains/{domainId}` is
  the product's root concept. Navigation is permission-aware.
- Screens: Work, Agent console (streaming), Cost, and functional stubs for
  Repositories, Memories, Skills and Settings.
- A typed, domain-scoped API seam backed by MSW. The real backend is not wired.
- Playwright end-to-end and accessibility coverage.

Out of scope, deliberately: the real SSE transport (tracked as 0003), work item
editing, and any call to a live API.

## Success criteria and evidence

Every command below was run from `backoffice/` on 2026-08-28.

### 1. The project builds

```
$ npm run build
vite v8.2.2 building client environment for production...
✓ 362 modules transformed.
dist/index.html                    1.38 kB │ gzip:   0.72 kB
dist/assets/index-BKxwL_yR.css    41.05 kB │ gzip:   8.32 kB
dist/assets/index-pmwkuhIV.js    331.17 kB │ gzip: 102.88 kB
dist/assets/browser-D_Qk7pJ3.js  430.53 kB │ gzip: 161.26 kB
✓ built in 200ms
```

MSW is dynamically imported behind `VITE_MOCK_API`, so it splits into its own
chunk and leaves the bundle entirely once a real backend exists.

### 2. Types check, in both packages

```
$ npm run typecheck
> tsc --noEmit -p tsconfig.json && npm --prefix ../e2e run typecheck
> @modus/e2e@0.0.0 typecheck
> tsc --noEmit
```

No output, exit 0. `exactOptionalPropertyTypes` is on, which is why optional
props are spread conditionally rather than passed as `undefined`.

### 3. Lint and formatting are clean

```
$ npm run lint
> eslint .
exit=0

$ npm run format:check
Checking formatting...
All matched files use Prettier code style!
```

ESLint runs `typescript-eslint` with type-aware rules and bans the `fetch`
global everywhere except `src/api/http.ts` and the mock handlers, so "no
component calls fetch directly" is enforced mechanically rather than by review.

### 4. End-to-end and accessibility tests pass

```
$ npm run test:e2e
> npm --prefix ../e2e run test
> playwright test
  33 passed (7.6s)
```

The suite covers: a smoke test per route, root and unknown-domain redirects,
opening a work item's markdown body, the theme toggle (including persistence
across reload and an explicit light choice beating a dark OS), the domain
switcher (mouse and keyboard, plus permission-driven navigation differences),
the streaming agent console (incremental text, tool calls, tool failures, live
cost counter, cancellation), and axe-core scans of all seven routes plus the
dark theme, the open domain menu, an open dialog, and a mid-stream console.
Zero violations against `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`.

### 5. Contrast is measured, not assumed

Token pairs were checked numerically against the WCAG formula before any
component used them. Two failures were found and fixed: the muted ink step
failed on the page ground (4.30:1) and the light informational hue failed inside
its own badge (3.73:1). Both were re-stepped and now pass at 4.96:1 and 5.47:1.
Chart series colours were run through the dataviz palette validator and pass the
lightness band, chroma floor, adjacent CVD separation, normal-vision floor and
surface-contrast checks in both themes.

### 6. The streaming seam is swappable

The console depends only on `StreamTransport`. `MockStreamTransport` replays a
canned session with realistic pacing; the SSE implementation lands in 0003 and
changes one line at one call site.

The *behavioural* half of that claim was weaker than the structural half, and
the review found it. `cancel()` used to depend on the mock synthesising a
`session-end` that a real `EventSource.close()` would never send, and an
`error` event left tool blocks spinning. Both are fixed below; the mock now
closes the way a real transport does, and the interface documents that
`onClose` may fire with no terminal event before it.

## Notes

- Styling is CSS Modules. The token layer stays authoritative because there is
  no utility vocabulary competing with it, and no runtime cost.
- Replay pacing is adjustable from the URL (`?replay=0.05`), which the e2e suite
  uses to run a 20-second session in one second.

## Review cycle 1

Six inline threads on PR #3, all six fixed. Every test added or changed below
was checked by breaking the source it covers and confirming the test fails —
the project's stand-in for a mutation-testing gate until one exists.

### 1. Pricing was 3x over on the console's default model

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
reads slightly high for three days rather than halving silently on 1 September.
The constant is now documented as server-side data that belongs in the `usage`
payload once 0003 lands. The model picker and the fixtures were moved to Sonnet
5 with it.

*Load-bearing:* `agent-console.spec.ts` now runs the identical session on three
models and asserts the cost ratios, which are exactly the price ratios — Opus 5
is 5x Haiku 4.5, Sonnet 5 is 3x. Restoring 15/75 makes that ratio 14.95 and the
test fails.

### 2. An `error` event left tool blocks spinning forever

Only `session-end` demoted `running` tool blocks to `failed`. A real stream
that dies mid tool call sends `error` and never gets to send `session-end`, so
the user was left with a permanent spinner beside an error notice. The sweep is
now a shared `resolveRunningTools` helper applied by `session-end`, `error` and
`cancel`, and `error` closes the subscription. The `onError` path, previously
dead and untested, now has coverage.

*Load-bearing:* two new tests drive the mock's injected faults
(`?fault=stream-error` and `?fault=transport-error`). Removing the sweep from
the `error` case fails both — the tool block stays `running`.

### 3. Stopping depended on an undocumented transport courtesy

`MockStreamTransport.cancel()` synthesised a `session-end`, and that synthetic
event was the only thing moving the UI out of `streaming`. `useAgentSession.cancel()`
now owns the transition itself — sets `cancelled`, sweeps running tools — and
the mock no longer emits the courtesy event, so it closes exactly the way
`EventSource.close()` does. `StreamHandlers.onClose` and
`StreamSubscription.cancel` document the contract.

*Load-bearing:* `a running session can be stopped` now passes against a mock
that emits no terminal event. Reverting the state transition in the hook fails
it — `Cancelled` never appears.

### 4. The `agents.run` gate had no coverage at all

No fixture granted `agents.read` without `agents.run`, so the disabled button
and its explanatory copy had never rendered. Replacing `can('agents.run')` with
`true` used to delete the gate with the full suite still green.

A fourth fixture domain, **Beacon Analytics**, now grants every read capability
and no `agents.run` — an observer in someone else's tenant. The test asserts
the button is genuinely disabled, the copy is present, and a forced click
starts nothing.

*Load-bearing:* re-running the reviewer's experiment — `const canRun = true` —
now fails `runs are refused where the actor cannot start them` (30 passed, 1
failed), where before it passed 33/33.

### 5. The `fetch` ban was bypassable five ways

`no-restricted-globals` only matches unqualified identifiers, so
`window.fetch`, `globalThis.fetch`, `EventSource`, `WebSocket` and
`navigator.sendBeacon` all linted clean — including the two constructors an SSE
client (0003) would reach for. `EventSource`, `WebSocket` and `XMLHttpRequest`
joined the globals list, and a `no-restricted-properties` rule now covers
`fetch`/`EventSource`/`WebSocket`/`XMLHttpRequest` on `window`, `globalThis`
and `self`, plus `navigator.sendBeacon`.

*Proof:* all five bypasses, planted in `src/routes/NotFound.tsx`, now error
(8 errors including the bare `fetch`, `self.fetch` and `XMLHttpRequest`); the
probes were reverted. The SSE client is deliberately **not** pre-allowlisted —
0003 has to add itself on purpose.

### 6. `% SERIES.length` cycled colours

The file claimed series colours are "never cycled" while all three call sites
used `SERIES[index % SERIES.length]`; a sixth model would silently repeat
`--series-1` and void the CVD-separation claim. The invariant is now true
rather than deleted: the palette keeps five validated slots, and a sixth or
later series is folded into one neutral `--series-other` bucket
("Other (N models)") which is achromatic on purpose, so it cannot be mistaken
for a series under any colour vision. `--series-other` is 4.30:1 on the light
surface and 6.11:1 on the dark one.

*Load-bearing:* Beacon Analytics reports six models, and a new test reads the
computed background of every slice and asserts no colour repeats. Restoring the
modulo fails it with six slices and a duplicate.

### Also addressed from the review summary

- **`$428.60` was a fixture echo.** The cost test now asserts derived numbers:
  the month-on-month delta and its percentage (`▲ $77.55 (22%)` from 428.60 and
  351.05), the share of budget consumed (57% of $750.00, and the meter's
  `aria-valuenow`), and one work item's share of the month (15%). *Load-bearing:*
  flipping the delta subtraction to addition fails it.
- **Seven `renders {X}` smoke tests duplicated seven axe route scans.** Merged:
  the axe route loop now asserts the heading *name* and the domain switcher in
  the same visit, and the duplicate smoke loop is gone. *Load-bearing:*
  renaming the Skills heading fails `Skills renders with no accessibility
  violations`.

### Gates, re-run after the fixes

```
$ npm run build
vite v8.2.2 building client environment for production...
✓ 362 modules transformed.
dist/index.html                    1.38 kB │ gzip:   0.72 kB
dist/assets/index-DXpPxzwf.css    41.12 kB │ gzip:   8.34 kB
dist/assets/index-c2YdZCB-.js    332.22 kB │ gzip: 103.18 kB
dist/assets/browser-DWZKGrUU.js  433.96 kB │ gzip: 162.15 kB
✓ built in 170ms

$ npm run typecheck
> tsc --noEmit -p tsconfig.json && npm --prefix ../e2e run typecheck
exit=0

$ npm run lint
> eslint .
exit=0

$ npm run format:check
Checking formatting...
All matched files use Prettier code style!

$ npm run test:e2e
> playwright test
  31 passed (7.0s)
```

31, not 33: seven duplicate smoke tests were removed and five tests added
(priced cost, two stream-failure paths, the refused run, the six-model
palette). Test count went down and coverage went up, which is the intended
direction.
