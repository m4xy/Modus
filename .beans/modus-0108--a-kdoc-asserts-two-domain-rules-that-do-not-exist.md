---
# modus-0108
title: A KDoc asserts two domain rules that do not exist, and now contradicts the document that says so
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# A KDoc asserts two domain rules that do not exist, and now contradicts the document that says so

`architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt:527-530`
carries this, in the KDoc opening at `:526` for the stdlib allowlist:

```
cmd:      sed -n '526,530p' architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/ArchitectureRulesTest.kt
observed:         /**
                   * The Kotlin standard library plus the `java.*` packages it erases to. `java.util`
                   * and `java.lang` are prefixes of themselves only in the sense that
                   * `java.util.concurrent` and `java.lang.reflect` are forbidden in the domain by
                   * their own rules, not by this one.
exit:     0
```

**No such rules exist.** `NoAmbientConcurrency` and `NoReflection` are specified in
`doc:15-repository-layout#core-package-rules` §4.2 and implemented by nothing. The nearest
thing is `rule:archunit/nothingSleepsTheThread`, which bans `Thread.sleep` repository-wide and
reaches no other part of either row.

## Why it is worth a bean rather than a one-line edit

`bean:0068`'s gates branch rewrote §4.2's enforcement-gap paragraph to state, rule by rule and
clause by clause, exactly what is and is not enforced — including that `NoAmbientConcurrency`
is reached only through `Thread.sleep` and that `NoReflection` does not exist at all. That
branch merged as `9c9940d`, so the repository holds a code comment and a normative document
making opposite claims about the same two rules **now**, not on some later merge, with the
comment sitting in the file a reader opens to check the document.

`doc:05-authoring-for-agents#one-fact-one-place` binds code comments explicitly — "they are
simply the copies nobody greps" — and this is the shape it warns about, with the aggravating
detail that the copy asserts enforcement rather than merely restating it. A reader who trusts
it will believe the domain is guarded where it is not.

It is not fixed in `bean:0068`'s branch because that branch's diff is under review and an
author editing outside the flagged surface between the flag and the review is how a reviewer
ends up reviewing something nobody flagged.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The KDoc states what is actually enforced, or names the gap, and cites `doc:15-repository-layout#core-package-rules` §4.2 rather than restating it | citation |
| 2 | The enumeration behind the claim is redone at fix time, not carried from here: every `ArchRule` val in both files, checked against §4.2's rows | command |
| 3 | Whether any other comment in `architecture-tests/` asserts enforcement that does not exist is answered, since this one was found by reading a neighbouring line rather than by looking | command |
