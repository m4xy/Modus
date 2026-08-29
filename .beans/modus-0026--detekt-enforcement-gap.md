---
# modus-0026
title: Close the Detekt enforcement gap — implement the custom rules or strike them
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Close the Detekt enforcement gap — implement the custom rules or strike them

Why: `doc:30-code-style#custom-detekt-rules` tabulates eleven Modus-specific rules and
says they live in `build-logic` as a rule-set provider. No provider and no rule exists;
`doc:30-code-style#kotlin-language-rules` §2's `Enforced by:` column and
`doc:00-constitution#layering` §1.3's `ForbiddenDomainApi` citation rest on the same
absence. §4 and §1.3 now carry an `Enforcement gap:` naming this bean, §4 covering §2's
column with it; the wider sweep is `bean:0027`.

Separately, Detekt 1.23.8 analyses the PSI only, so the 65 configurable rules needing type
resolution cannot fire whatever the config says. `config/detekt/detekt.yml`'s header owns
that list, what covers it elsewhere, and its closing condition.

Success criteria:

- Each of the eleven rules is either implemented with a positive and a negative test and a
  repository-wide clean run (`doc:30-code-style#changing-a-style-rule`), or struck with its
  hazard reassigned to a mechanism that exists — several are decidable in bytecode and
  belong in `architecture-tests`, as `println` and the static clock already do.
- Every surviving rule observed rejecting a planted violation
  (`doc:00-constitution#observed-failing`).
- Type resolution re-audited on Detekt 2.x stable, built against Kotlin 2.4.x and tested
  on JDK 25, at which point the `JavaExec`-on-JDK-21 workaround and every `active: false`
  type-resolution block are revisited together.
