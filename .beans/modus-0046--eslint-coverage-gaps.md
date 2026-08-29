---
# modus-0046
title: Close the ESLint gaps bean:0029's review exposed
status: completed
type: fix
priority: high
order: AJ
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
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

## Decisions taken

**`e2e/` gets its own flat config; `backofficeLint` widens through the npm script, not
through a second Gradle task.** ESLint 9 resolves a flat config from the working
directory, so one config in `backoffice/` cannot lint a sibling tree; and the two trees do
not share a rule set anyway — there is no React and no JSX in `e2e/`, and a Playwright test
body runs in Node while a `page.evaluate` callback runs in the browser, so both global sets
are in scope there and neither is in scope in the other. So: a second config,
`e2e/eslint.config.js`.

The task is the part worth arguing. A separate `e2eLint` Gradle task would have left
`npm run lint` — the command anyone in `backoffice/` actually types — short of what the
gate runs, which is the exact shape of the hole this bean closes: a check that exists and
does not cover. `backoffice/package.json`'s `lint` now chains `npm --prefix ../e2e run
lint`, which is what `typecheck` and `format:check` already do (`build.gradle.kts` already
described both as covering "backoffice/ and e2e/"). One task per concern, both trees, and
the hand-run command equals the gated one.

**`no-non-null-assertion` is set as its own rule, not by swapping the preset.** The row
documents a rule, not a preset. Moving `recommendedTypeChecked` to `strictTypeChecked`
would enable roughly twenty further rules that this bean neither assessed nor evidenced,
and `doc:80-agent-operating-procedure` §5.1 is explicit about drive-by scope. The rule is
`error` in both configs.

**All three rows are installed; none is struck.** The `knip` row `bean:0029` struck was
aspiration with no bearing on a written rule. These three are different: `!` is the
assumption `doc:20-ddd-practices#domain-prohibitions` bans as `!!` in Kotlin, cycles are the
`doc:00-constitution#layering` acyclic rule one layer up, and accessibility is a deliverable
under `doc:00-constitution` §10 with axe assertions already in `e2e/`. Each already had a
rule behind it; only the mechanism was missing.

**One jsx-a11y rule is configured rather than obeyed, and it is `no-noninteractive-tabindex`.**
The agent transcript is `role="log"`, `height: 26rem`, `overflow-y: auto`, measured at
`scrollHeight 1119` against `clientHeight 416` — a scrollable region, which WCAG 2.1.1
requires to be keyboard-scrollable, hence `tabIndex={0}`. The rule ships allowing exactly
one role for exactly this reason (`roles: ['tabpanel']`); `log` is added beside it and
nothing else is. Deleting the `tabIndex` would also have been green, which is why this is
recorded as a decision rather than a fix: axe accepts the region either way (below), so no
other gate in the repository would have noticed the keyboard access going missing.

**The six other violations are fixed in the source, not configured away.** Three of them
were the same defect: a keyboard handler on a container (`role="menu"`, `role="tablist"`, a
wrapper `<span>`) that is not itself focusable. Focus is always on a child in all three
cases, so the container only ever received those events by bubbling; the handler moved onto
the child, which is where the ARIA patterns put it. The fourth is `Dialog`'s
backdrop-to-close, which becomes a `useEffect` listener on the `<dialog>` element: as an
`onClick` prop it puts a pointer interaction on a non-interactive role, and jsx-a11y cannot
see that the keyboard equivalent is the platform's Escape arriving as `onCancel`.

The existing suite passed before and after all four, which is the problem: it never drove
the keyboard paths that moved, so it could not have told me if I had broken one. Two tests
were added and each was observed failing against the change deleted (`doc:35-testing` §6):

```
plant: the moved onKeyDown removed from the menu item and from cloneElement
observed:
  2 failed
    tests/domain-switcher.spec.ts:57 › arrow keys move focus between the menu items
    tests/smoke.spec.ts:97 › a tooltip opens on focus and dismisses on Escape
      - waiting for getByRole('tooltip') … unexpected value "visible"
```

`Tabs` is the third of the three and is rendered by no route, so it has no Playwright test
and cannot have one yet. It is exercised only by the type checker and the linter. Said
here rather than left for a reviewer to discover.

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | `e2e/` is linted, with the decision recorded | plant 1 — `backofficeLint` now rejects what it passed |
| 2 | `jsx-a11y` installed and `error` | plant 3 |
| 3 | `import/no-cycle` installed and `error` | plants 4 and 5 — and see "the finding" below, because the first version of this rule was green on both |
| 4 | `no-non-null-assertion` is `error` | plant 2 — the bean's own line, now rejected |
| 5 | both `Enforcement gap:` lines become `Enforced by:` | `doc:30-code-style` §6; `docs-lint: OK — 18 documents, 96 anchors, 690 references.` |

Every plant below was run as `./gradlew backofficeLint` and reverted. `1` is the line the
bean opened with; `2` is the line `bean:0029`'s review planted.

```
plant 1 — e2e/tests/smoke.spec.ts (was: BUILD SUCCESSFUL)
  const unusedBinding = 1;
  const alsoUnused = (s: string | null) => s!.length;
observed:
  e2e/tests/smoke.spec.ts
    10:7   error  'unusedBinding' is assigned a value but never used  @typescript-eslint/no-unused-vars
    13:9   error  'alsoUnused' is assigned a value but never used     @typescript-eslint/no-unused-vars
    13:44  error  Forbidden non-null assertion                        @typescript-eslint/no-non-null-assertion
  > Task :backofficeLint FAILED

plant 2 — backoffice/src/App.tsx (was: BUILD SUCCESSFUL)
  export const bang = (s: string | null) => s!.length;
observed:
    104:43  error    Forbidden non-null assertion  @typescript-eslint/no-non-null-assertion
  > Task :backofficeLint FAILED

plant 3 — backoffice/src/App.tsx
  <div tabIndex={0}><img src="/logo.png" /></div>
observed:
    106:10  error  `tabIndex` should only be declared on interactive elements   jsx-a11y/no-noninteractive-tabindex
    107:7   error  img elements must have an alt prop, either with meaningful   jsx-a11y/alt-text
                   text, or an empty string for decorative images
  > Task :backofficeLint FAILED

  (plant 3 is also the guard on the `roles: ['tabpanel', 'log']` option: a bare div still
  fails, so the allowance is the two roles and not the rule.)

plant 4 — backoffice/src/ui/cx.ts, re-exporting the module that imports it
  export { Tabs } from './Tabs';
observed:
  backoffice/src/ui/Tabs.tsx
    3:1  error  Dependency cycle detected  import/no-cycle
  backoffice/src/ui/cx.ts
    1:1  error  Dependency cycle detected  import/no-cycle
  > Task :backofficeLint FAILED

plant 5 — e2e/tests/planted-{a,b}.ts importing each other
observed:
  e2e/tests/planted-a.ts
    1:1  error  Dependency cycle detected  import/no-cycle
  e2e/tests/planted-b.ts
    1:1  error  Dependency cycle detected  import/no-cycle
  > Task :backofficeLint FAILED
```

Gate, after the last change:

```
./gradlew qualityCheck   BUILD SUCCESSFUL
./gradlew e2eTest        33 passed — BUILD SUCCESSFUL
```

## The finding: `import/no-cycle` was unfalsifiable on the first attempt

The first version of the rule was `import/no-cycle: error` with the plugin registered and
`settings['import/resolver'] = { typescript: … }`. Plants 4 and 5 were both **green**.

The resolver was not the problem — `import/no-unresolved` correctly rejected
`export { Nope } from './DoesNotExist';` and correctly accepted `'./Tabs'`, so the path
resolved. `eslint-plugin-import` also has to *parse* the resolved file to see what it
imports, and in flat config it takes the parser for a dependency from
`settings['import/parsers']`. With no such setting it parses no TypeScript file, follows
nothing, and reports nothing — silently, with a green build.

Fixed by spreading `importPlugin.flatConfigs.typescript.settings` (which is
`import/parsers`, `import/extensions` and `import/external-module-folders`) into both
configs ahead of the resolver override. Plants 4 and 5 then failed as above.

This is `doc:00-constitution#observed-failing` earning its place: the rule was installed,
`--print-config` showed it at severity 2, and it enforced nothing. Reading the config would
have confirmed it; only planting a cycle disproved it.

## Also corrected while here

The `Linting` row said `@typescript-eslint` **strict**. It has always been
`recommendedTypeChecked` (`backoffice/eslint.config.js`), which is what made
`no-non-null-assertion` a false claim in the first place. That is a fourth wrong cell in the
same table, of the same kind, and it now reads `recommended-type-checked` and names both
config files.
