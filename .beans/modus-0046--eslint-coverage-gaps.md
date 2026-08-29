---
# modus-0046
title: Close the ESLint gaps bean:0029's review exposed
status: todo
type: fix
priority: high
order: AJ
created_at: 2026-08-29T00:00:00Z
---

# Close the ESLint gaps `bean:0029`'s review exposed

Wiring the backoffice into the gate made `doc:30-code-style` §6 read as backed by a real
mechanism. Review then found three of its rows are not, and one whole directory is
unlinted. Under `doc:00-constitution#observed-failing` that is worse than the admitted gap
it replaced, because a table that is mostly true is one nobody re-checks.

| claim | reality | observed |
|---|---|---|
| ESLint covers the front end | `backofficeLint` runs `eslint .` with `workingDir = backoffice`, and `e2e/` has no ESLint configuration, so **`e2e/` is linted by nothing** | two unused bindings and a non-null assertion in `e2e/tests/smoke.spec.ts` → `./gradlew backofficeLint` **BUILD SUCCESSFUL**; a type error in the same file does fail `backofficeTypecheck`, which is what made the hole invisible |
| `jsx-a11y` recommended, `error` | `eslint-plugin-jsx-a11y` is not a dependency | not installed |
| `import/no-cycle`, `error` | `eslint-plugin-import` is not a dependency | not installed |
| `no-non-null-assertion`, `error` | `eslint.config.js` uses `recommendedTypeChecked`, not `strict` | `export const bang = (s: string \| null) => s!.length;` in `backoffice/src/App.tsx` → `backofficeLint` **BUILD SUCCESSFUL** |

## Success criteria

Each observed rejecting a planted violation before the row is written as `Enforced by:`
(`doc:00-constitution#observed-failing`).

- `e2e/` is linted. Either it gets its own flat config and a task, or `backofficeLint`'s
  scope widens to both trees — decide which, and record why. Accessibility assertions live
  in `e2e/`, so it is not a directory where lint quality is optional.
- `jsx-a11y` is installed and `error`, or the row is struck. `doc:00-constitution` §10 says
  the UI is a deliverable and every user-facing flow has a Playwright test; the axe
  assertions in `e2e/accessibility.spec.ts` already exist, so the lint half is the cheaper
  half of a rule the repository has already committed to.
- `import/no-cycle` is installed and `error`, or struck.
- `no-non-null-assertion` is `error` — `!` in TypeScript is the same unproven assumption
  `doc:20-ddd-practices#domain-prohibitions` bans as `!!` in Kotlin, where Detekt enforces
  it at `error`. The asymmetry is not defensible; make the front end match or say why.
- The two `Enforcement gap:` lines `bean:0029` left in `doc:30-code-style` §6 are removed,
  each replaced by an `Enforced by:` naming the mechanism observed failing.
