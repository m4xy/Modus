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

## Notes

- Styling is CSS Modules. The token layer stays authoritative because there is
  no utility vocabulary competing with it, and no runtime cost.
- Replay pacing is adjustable from the URL (`?replay=0.05`), which the e2e suite
  uses to run a 20-second session in one second.
