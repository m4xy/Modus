---
# modus-0191
title: The mock-API flag is not statically foldable, so MSW ships in every build
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# The mock-API flag is not statically foldable, so MSW ships in every build

Why: `backoffice/src/main.tsx` guards the mocked API behind
`import.meta.env['VITE_MOCK_API'] !== 'false'` and dynamically imports `./mocks/browser` when
it holds. The intent — stated in that file's own comment and repeated in `bean:0002` — is that
pointing the app at a real backend "drops MSW out of the bundle entirely". It does not.

**Bracket access defeats Vite's static replacement.** Vite folds `import.meta.env.FOO` to a
literal; `import.meta.env['FOO']` is left as a property read on an injected object, so the
condition survives into the output as a runtime comparison and Rollup cannot prove the dynamic
`import()` is unreachable. The MSW chunk is emitted either way.

This matters beyond bundle size: `backoffice/src/mocks/handlers.ts` carries a `?fail=<resource>`
fault switch (`bean:0140`), so on any build this repository actually produces, `?fail=all` is
live on the shipped bundle and every API read can be made to 500 from the URL bar.

## Observed

```
cmd:      cd backoffice && rm -rf dist && npx vite build
observed: dist/assets/browser-Ck4cxOVV.js   434345 bytes

cmd:      cd backoffice && rm -rf dist && VITE_MOCK_API=false npx vite build
expected: no browser-*.js chunk — this is the flag's whole purpose
observed: dist/assets/browser-C2ngF1C2.js   434367 bytes
```

The chunk is not merely emitted, it is intact. Against the `VITE_MOCK_API=false` output:

```
cmd:      grep -o 'URLSearchParams' dist/assets/browser-*.js | head -2
observed: URLSearchParams

cmd:      grep -o 'fail' dist/assets/browser-*.js | head -3
observed: fail
          fail
          fail

cmd:      grep -c 'status:500' dist/assets/browser-*.js
observed: 4
```

The cause, read out of the entry chunk of the same build:

```
cmd:      grep -o 'VITE_MOCK_API[^,;)]*' dist/assets/index-*.js | head -3
expected: nothing — a folded constant leaves no identifier behind
observed: VITE_MOCK_API:`false`}.VITE_MOCK_API!==`false`
```

That is a runtime property read against an injected object, not a constant. And nothing sets
the flag in the first place:

```
cmd:      grep -rn 'VITE_MOCK_API' --exclude-dir=node_modules --exclude-dir=dist .
observed: backoffice/src/main.tsx:13   (the read)
          backoffice/src/mocks/handlers.ts:24   (a comment about the read)
          four .beans/ files
          (no .env file, no vite `define`, no deploy step, no CI variable — nothing
           anywhere assigns it)
```

## Success criteria

| # | Criterion | Evidence kind |
|---|---|---|
| 1 | A production build with the mocks disabled emits no MSW chunk, and no artefact in `dist/` contains the `?fail=` switch — asserted by grep over the built output, not by reading the config | command |
| 2 | The disabling mechanism is exercised by something that runs: a build in CI, or a check that greps `dist/`. A flag no build sets is a flag nobody can observe working | test-run |
| 3 | `src/main.tsx`'s comment and `bean:0002`'s claim are corrected or shown true; `backoffice/src/mocks/handlers.ts`'s corrected comment is reduced to a pointer once it is | citation |
| 4 | Every remaining `import.meta.env['…']` bracket access in `backoffice/` is converted or recorded as deliberately dynamic | citation |

Either dot access (`import.meta.env.VITE_MOCK_API`) or an explicit `define` in
`vite.config.ts` will fold it; the choice is the implementer's, and criterion 1 is what
decides whether it worked.

Note for whoever takes this: `src/agent/mockTransport.ts`'s `?fault=` and `?replay=` switches
are the precedent this repository already had, and they are worse — they sit in the **main**
chunk behind no flag at all, so they ship unconditionally. Fixing the flag does not reach them.
`bean:0146` owns making the `VITE_MOCK_API=false` path actually work against a live server;
this bean owns only whether the flag folds.

Found by independent review of the `bean:0140`/`bean:0141` pull request, which had asserted the
opposite in a code comment without building it either way.
