---
id: doc:30-code-style
title: Code style
status: active
superseded_by: null
read_when:
  - path: "**/*.kt"
  - path: "**/*.kts"
  - path: "**/*.ts"
  - path: "**/*.tsx"
  - path: config/detekt/**
  - path: build-logic/**
  - path: .editorconfig
  - task: kotlin|typescript|ktlint|detekt|archunit|formatting|code style|naming|complexity|build (check|gate) fail
provides:
  - doc:30-code-style#formatting
  - doc:30-code-style#kotlin-language-rules
  - doc:30-code-style#detekt-configuration
  - doc:30-code-style#custom-detekt-rules
  - doc:30-code-style#archunit-rules
  - doc:30-code-style#archunit-synthetic-classes
  - doc:30-code-style#testing-style
  - doc:30-code-style#changing-a-style-rule
depends_on: [doc:00-constitution, doc:10-architecture, doc:15-repository-layout, doc:20-ddd-practices, doc:80-agent-operating-procedure]
---

# 30 — Code Style

## The principle

> **Style is enforced by tools, never by review comments.**

A style opinion that lives in a human's head is a tax on every future change. A style
opinion encoded in a linter is paid once. Therefore:

1. If you want a style rule, you add it to a tool. Adding it to this document *without*
   a tool is only acceptable as a documented `Enforcement gap:` with a work item.
2. If you find yourself writing a style comment in review, **stop**. Either the tool is
   missing the rule (open a work item and say so in the thread) or the rule does not
   actually matter (drop it).
3. A pull request is never blocked on taste. It is blocked on a red build.

The three-tool division of labour:

| Tool | Answers | Failure mode it prevents |
|---|---|---|
| **ktlint** | "Is it formatted?" | Diff noise, formatting debates |
| **Detekt** | "Is it written well, and does it avoid our known hazards?" | Complexity, dangerous APIs, project-specific mistakes |
| **ArchUnit** | "Is it in the right place, depending on the right things?" | Architectural drift |

All three run in every module's `check`, which `qualityCheck` aggregates.
`./gradlew qualityCheck` is the gate; CI runs exactly that task with no extra arguments, so
a green local run means a green CI run. The gate is stated once, in `00-constitution.md`
§7.2.4; this document and `80-agent-operating-procedure.md` step 6 cite it rather than
restating its commands.

---

## 1. Formatting — ktlint <a id="formatting"></a>

ktlint owns Kotlin formatting, through `org.jlleitschuh.gradle.ktlint` applied in the
`modus.kotlin-base` convention plugin and in the root project. Its `ktlint_official` code
style is the ruleset. There is no negotiation about formatting; run
`./gradlew ktlintFormat` and move on.

| Setting | Value |
|---|---|
| Code style | `ktlint_official` |
| Indent | 4 spaces, no tabs |
| Max line length | 120 |
| Trailing commas | Required on call site and declaration site |
| Import ordering | Lexicographic, no wildcard imports (ever) |
| Final newline | Required |
| Trailing whitespace | Forbidden |
| Blank lines at block start/end | Forbidden |
| String templates | No redundant braces (`$name`, not `${name}`) |

Tasks: `./gradlew ktlintCheck` (part of `check`), `./gradlew ktlintFormat` (fixes).
Both cover `*.kt` and `*.kts`, the root project's build scripts included.

**Never** commit with a ktlint violation and "fix it in review". `ktlintFormat` takes under
two seconds.

**One tool per language, configured where its ecosystem expects.** ktlint owns `*.kt` and
`*.kts`; `backoffice/`'s own Prettier owns `*.ts`, `*.tsx` and `*.css`, and `qualityCheck`
now runs it. Spotless was considered and rejected in `bean:0029`: it would add a second
Kotlin formatter beside ktlint and a second Prettier configuration beside
`backoffice/.prettierrc`, which is one fact in two places.

**Enforcement gap:** `*.md`, `*.yaml` and `*.json` are formatted by nothing. That is
accepted rather than pending — `docs-lint` already checks what matters about Markdown here
(front-matter, anchors, references, line budget), and no rule in this package depends on
YAML or JSON layout. Raise a bean if one ever does.

---

## 2. Kotlin language rules <a id="kotlin-language-rules"></a>

These are enforced by ktlint or Detekt as noted; they are listed here because agents ask
about them.

| Rule | Enforced by |
|---|---|
| No wildcard imports | ktlint |
| No unused imports | ktlint |
| Explicit visibility modifiers on public API of library modules (`core/*`) | Kotlin compiler `-Xexplicit-api=strict` |
| `val` over `var` — a `var` in a class body requires a comment justifying it | Detekt `VarCouldBeVal` + custom `JustifiedVar` |
| No `!!` anywhere | Detekt `UnsafeCallOnNullableType` at `error` severity |
| No `lateinit` in `core/` | Detekt custom `ForbiddenDomainApi` |
| No `TODO()` / `NotImplementedError` on a merged branch | Detekt `NotImplementedDeclaration` at `error` |
| No `println` / `print` outside `build-logic` | Detekt `ForbiddenMethodCall` |
| No `@Suppress` without a trailing comment giving a reason | Detekt custom `JustifiedSuppression` |
| Warnings are errors | `allWarningsAsErrors = true` in the Kotlin convention plugin |
| No `Any`-typed public API | Detekt custom `ForbiddenDomainApi` (core only) |
| Data classes for data, `object` for singletons with no state | review |

**Compiler flags** (set in `build-logic`, `modus.kotlin-conventions`):
`-Xjsr305=strict`, `-Xexplicit-api=strict` (in `core/*`), `allWarningsAsErrors=true`,
`-opt-in=kotlin.RequiresOptIn`.

---

## 3. Detekt — standard configuration <a id="detekt-configuration"></a>

Detekt runs with `buildUponDefaultConfig = true` and a project config at
`config/detekt/detekt.yml`. Deviations from Detekt's defaults:

| Rule | Setting | Why |
|---|---|---|
| `MaxLineLength` | disabled | ktlint owns line length; one owner per rule |
| `CyclomaticComplexMethod` | threshold 10, `error` | Complexity above this is untestable |
| `LongMethod` | 40 lines, `error` | |
| `LongParameterList` | 6 (constructors 8), `error` | More means a missing value object |
| `TooManyFunctions` | 15 per class, `error` | More means a missing type |
| `LargeClass` | 250 lines, `error` | |
| `NestedBlockDepth` | 4, `error` | |
| `ReturnCount` | 3, `error`, excluding guard clauses | |
| `MagicNumber` | `error`; allowed: -1, 0, 1, 2; excluded in tests | Constants are named |
| `SwallowedException` | `error` | |
| `TooGenericExceptionCaught` | `error` | Catch what you can handle |
| `ThrowingExceptionsWithoutMessageOrCause` | `error` | |
| `ForbiddenComment` | `TODO:`, `FIXME:`, `STOPSHIP` → `error` | Unfinished work is a work item, not a comment |
| `UnusedPrivateMember` | `error` | |
| `ComplexCondition` | 3, `error` | Name the sub-predicates |
| `SpreadOperator` | `error` | Allocation surprise |
| `LabeledExpression` | `error` | |
| `UseCheckOrError`, `UseRequire` | `error` | Consistent invariant expression (`20` §7.2) |
| Naming rules | all `error` | |
| Formatting ruleset | **disabled entirely** | ktlint owns formatting (§1) |

**Test sources:** relaxed for `LongMethod`, `MagicNumber`, `TooManyFunctions`, and
`MaxLineLength`. Everything else applies — test code is code.

**The 1.23.8 toolchain traps.** Detekt 1.23.8 is the current stable release and it embeds
an IntelliJ core whose `JavaVersion` parser rejects the JDK 25 version string, so the
Gradle plugin's in-daemon invoker dies on this repository's toolchain; Detekt is therefore
run as a `JavaExec` of `detekt-cli` on a JDK 21 launcher, which cannot change a finding
because Detekt never emits bytecode. It also embeds Kotlin 2.0.21 while Modus compiles
with 2.4.10, so no `--classpath` is passed and analysis is PSI-only.
**Enforcement gap:** every rule annotated `@RequiresTypeResolution` is skipped in silence,
including the rules this table would otherwise rely on. `config/detekt/detekt.yml`'s
header owns the list of what that costs, what covers it elsewhere, and the closing
condition; `bean:0026` carries it.

**Baselines are forbidden.** A Detekt baseline file hides debt indefinitely. If a rule
cannot be satisfied today, either the rule is wrong (change it, with a rationale in this
document) or the code is wrong (fix it, or open a work item and disable the rule
repository-wide with a comment naming the work item). No `detekt-baseline.xml` may exist
in this repository. **Enforcement gap:** the Gradle check that would fail the build if any
file matching `*baseline*.xml` exists does not exist. `bean:0027` carries the audit.

---

## 4. Custom Detekt rules <a id="custom-detekt-rules"></a>

These express Modus-specific hazards that no off-the-shelf rule covers.

**Enforcement gap:** none of them exists. There is no rule-set provider in `build-logic`,
so this table and every `Enforced by:` cell in §2 naming one of these rules is a
specification, not a mechanism (`doc:00-constitution#observed-failing`). `bean:0026`
carries implementing them or striking them.

| Rule | What it catches | Why it exists |
|---|---|---|
| **`ForbiddenDomainApi`** | Any reference from `core/core-domain` to the forbidden list in `20-ddd-practices.md` §8 — Spring, Jackson, `java.io`/`java.nio.file`, `java.net`, SLF4J, `println`, `Instant.now()`, `UUID.randomUUID()`, `java.util.concurrent`, reflection, `lateinit`, `System.getenv`. | ArchUnit catches type dependencies but not *call sites* like `Instant.now()`. Detekt sees the AST, so it can. This is the rule that keeps the core pure. |
| **`NoFloatingPointMoney`** | `Float`/`Double` in any type whose name or property name matches money/cost/price/spend/usd. | Cost is the product. Floating-point money produces spend figures that do not add up, and nobody notices until an invoice does not reconcile. |
| **`UnevidencedMemoryWrite`** | A call to a `MemoryRepository.save`-shaped API whose argument is constructed without a non-empty evidence collection, where statically determinable. | The evidence rule (`00` §3) is the product's core promise. A best-effort static check plus the runtime schema validation is better than runtime alone. |
| **`ForbiddenTypeNameSuffix`** | Types under `core/` named `*Impl`, `*Manager`, `*Helper`, `*Util(s)`, `*Data`, `*Info`, `*Dto`, `*Entity`, `*Bean`, or `*Service` outside the domain-service allowlist. | These names describe position, not behaviour, and they attract unrelated code. Enforcing naming prevents the "junk drawer class". |
| **`DomainScopedRoute`** | A Spring mapping annotation in `adapter-rest` or `modules/*` whose path neither starts with `/domains/{domainId}` nor matches the **non-domain-scoped route allowlist** (`doc:10-architecture#domain-root-convention` §5.1 — the rule reads that list; it does not carry its own copy). | Permissions are domain-scoped (`00` §8). One un-scoped route is a cross-domain data leak. Detekt catches it at the annotation, before ArchUnit sees the compiled class. |
| **`NoBlockingInSuspend`** | `Thread.sleep`, `runBlocking`, blocking IO, or `.get()` on a future inside a `suspend` function. | The streaming adapters are the hot path for backoffice output; one blocking call stalls a shared dispatcher and every live stream stutters. |
| **`JustifiedSuppression`** | `@Suppress` with no trailing `//` comment explaining it. | A suppression with a reason is a decision; one without is an unexplained hole. |
| **`JustifiedVar`** | A `var` property in a class under `core/` with no explanatory comment. | Mutable aggregate state is legitimate but must be conscious. |
| **`RawTokenArithmetic`** | Arithmetic on a raw `Long`/`Int` named like a token count, instead of the `TokenCount` value object. | Context-budget and cost accounting must not silently mix units. |
| **`NoStringDomainId`** | A function parameter named `domainId`/`actorId`/`workItemId`/`runId` typed `String`. | Primitive obsession here means an id from one context can be passed where another is expected. |
| **`NoMutableSingletonState`** | A `var` property, or a property of a mutable collection type, on an `object` or `companion object` under `core/`. | `20-ddd-practices.md` §8 forbids hidden global state, and until this rule existed no tool owned that row — `ForbiddenDomainApi` enumerates API *references*, and a `var` in a companion is not a reference to anything. Detekt has the AST, so this one is genuinely decidable. |

**Severity:** all custom rules are `error`. **Adding a custom rule** requires: the rule
implementation, a test for the positive and negative case, a row in this table with a
"why", and a repository-wide clean run.

**What a Detekt rule can see that an ArchUnit rule cannot.** Detekt analyses the Kotlin
AST, so it sees comments (`PsiComment`), property mutability, annotation arguments, and
call sites. ArchUnit analyses compiled bytecode, so it sees types, members, annotations
and **annotation values** — but never a comment, because the compiler discards them. Any
rule phrased as "requires a `//` comment" must therefore be a Detekt rule
(`JustifiedSuppression`, `JustifiedVar` — both fine); a rule ArchUnit must own has to be
phrased against something that survives compilation, which in practice means an
annotation attribute. §5's `@Disabled` rule is the worked example.

---

## 5. ArchUnit <a id="archunit-rules"></a>

ArchUnit enforces structure — what depends on what, what lives where, what a type must
implement. The complete rule set is derived from the tables in
`doc:10-architecture#module-dependencies` §4.1 and `doc:15-repository-layout` §4.2-§4.3.

Rule groups:

| Group | Enforces |
|---|---|
| `LayerDependencyRules` | The Gradle module dependency table (`10` §4.1) |
| `DomainPurityRules` | The package-level `core-domain` rules (`10` §4.2) |
| `ContextIsolationRules` | `ContextInternalsAreSealed`, `PublishedLanguageAllowlist`, `PublishedLanguageIsLeaf`, and no cycles over the internals slices (`10` §3.1, §4.2) |
| `AdapterRules` | Adapters implement ports; domain types do not escape; no DTOs in core (`10` §4.3) |
| `RestRules` | Every route is domain-scoped; no field injection; controllers return DTOs |
| `NamingRules` | Package placement matches type kind (`20` §5.1) |
| `NoDatabaseRules` | No JDBC/JPA/ORM/SQL types anywhere (`00` §2) |
| `TestRules` | No mocking framework in `core/`; no wall-clock dependency in any test; `DisabledCarriesWorkItem` (below) |

ArchUnit tests live in the `architecture-tests` module, which imports every other module's
bytecode, so a new module is covered as soon as `settings.gradle.kts` names it. (There is no
`modus.archunit` convention plugin; `build-logic` carries `modus.kotlin-base`,
`modus.coverage`, `modus.spring-app` and `modus.spring-module` and nothing else.) Freezing
(ArchUnit's `FreezingArchRule`) is **forbidden** for the same reason Detekt baselines are.

### 5.0 Kotlin generates classes your rule has to see <a id="archunit-synthetic-classes"></a>

A rule scoped by **class name** must account for the classes Kotlin emits that the source
never names. Two bite immediately:

| source | emitted | consequence for a name-scoped rule |
|---|---|---|
| `private companion object` inside `Foo` | `Foo$Companion` | a nested class outside the set its own outer type is in |
| a top-level `private val` in `Foo.kt` | `FooKt` file facade | a class in the same package that is not `Foo` and never will be |

Decide membership on the **outermost** enclosing class (`name.substringBefore('$')`), and
put a value object's validating `Regex` in a companion rather than at file scope so no
facade is generated at all. Both cases were observed failing `SharedKernelIsLeaf` on its
first two runs (`adr:0004-domain-id-shared-kernel`), which is the only reason they are
written down here rather than rediscovered.

A rule scoped by **package** does not have this problem: both generated classes land in the
package of their source file. Prefer a package scope where the architecture gives you one —
that is the argument `20-ddd-practices.md` §5.1 makes for `..domain.aggregate`.

### 5.1 `DisabledCarriesWorkItem`

> Every `@Disabled` / `@Ignored` **annotation value** matches `^bean:\d{4}`, and the rest
> of the value is a non-blank reason.

```kotlin
@Disabled("bean:0042: flaky under parallel execution — shared temp dir")
```

The reference is an **annotation attribute**, not a comment. `@Disabled`'s `value` is
retained in the class file, so ArchUnit can read it and assert on it; a `//` comment
beside the annotation is discarded by the compiler and no ArchUnit rule could ever see it.
A rule stated against a comment here would be unenforceable while three documents told
agents the build was catching it — which is worse than an admitted gap, because nobody
checks it in review either.

Kotest's `.config(enabled = false)` and `xdescribe`/`xit` have no annotation to carry the
reference, so they are **forbidden outright** in this repository; disable a test with
`@Disabled`. That keeps one mechanism, and it is the mechanism the rule can see.

---

## 6. TypeScript and the backoffice

| Concern | Tool | Setting |
|---|---|---|
| Formatting | Prettier (`backoffice/.prettierrc.json` + `.editorconfig`) | 2-space indent, 100 columns, single quotes, trailing commas. The indent comes from `.editorconfig`'s `[*.{ts,tsx,css}]` block, which exists because Prettier reads `.editorconfig` and would otherwise inherit `[*]`'s `indent_size = 4` — a ktlint setting. |
| Linting | ESLint | `@typescript-eslint` recommended-type-checked + `react-hooks` + `jsx-a11y` + `import`. Two flat configs, one per tree: `backoffice/eslint.config.js` and `e2e/eslint.config.js`. |
| Types | `tsc --noEmit` | `strict: true`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noImplicitOverride` |
| `any` | ESLint `no-explicit-any` | `error`. Use `unknown` and narrow. |
| Non-null assertion `!` | ESLint `no-non-null-assertion` | `error`, set as its own rule in both configs. `!` is the assumption `doc:20-ddd-practices#domain-prohibitions` bans as `!!` in Kotlin. |
| Import cycles | ESLint `import/no-cycle` | `error`, in both configs. Needs `settings['import/parsers']` as well as a resolver, or it parses no TypeScript dependency and reports nothing. |
| Accessibility | `jsx-a11y` recommended | `error` on `**/*.tsx`; plus axe assertions in Playwright. `no-noninteractive-tabindex` additionally allows `role="log"`, for the reason `tabpanel` is already allowed: a scrollable region has to be keyboard-scrollable. |
| API types | Generated from the OpenAPI document | Hand-written API types are forbidden — they drift |


**Enforced by:** `qualityCheck`, through `backofficeTypecheck`, `backofficeLint` and
`backofficeFormatCheck` (`build.gradle.kts`), each observed rejecting a planted violation
(`bean:0029`). The complete gate — and the only normative statement of it — is
`00-constitution.md` §7.2.4.

**axe and `jsx-a11y` do not enforce each other, in either direction.** With `tabIndex`
removed from a genuinely overflowing `role="log"` region — measured `scrollHeight 1119`
against `clientHeight 416` — axe's `scrollable-region-focusable` appeared in
`results.passes`, not in `violations` and not in `inapplicable` (`bean:0046`). A green axe
run is not evidence that keyboard access survived a refactor, and a `jsx-a11y` suppression
is not evidence that it did not. Both are needed, and neither substitutes for driving the
keyboard path in `e2e/` (`doc:35-testing#load-bearing-evidence`).

**Enforced by:** `backofficeLint` over **both** trees. `e2e/` has its own flat config —
ESLint 9 resolves one from the working directory — and the `lint` script in
`backoffice/package.json` chains it, exactly as `typecheck` and `format:check` already
chain theirs, so `npm run lint` by hand covers what the gate covers. Observed
(`bean:0046`): two unused bindings and a non-null assertion in `e2e/tests/smoke.spec.ts`
now fail `./gradlew backofficeLint` on `@typescript-eslint/no-unused-vars` and
`@typescript-eslint/no-non-null-assertion`, where before they left it green.

**Enforced by:** `backofficeLint`, for the three rows that named no mechanism before
`bean:0046`. Each was observed rejecting a planted violation through the Gradle task:
`export const bang = (s: string | null) => s!.length;` in `backoffice/src/App.tsx` →
`Forbidden non-null assertion`; `<img src="/logo.png" />` → `jsx-a11y/alt-text`; a
`cx.ts` ⇄ `Tabs.tsx` re-export and a two-file cycle under `e2e/tests/` →
`Dependency cycle detected`. `no-non-null-assertion` is set as its own rule rather than by
swapping `recommendedTypeChecked` for `strictTypeChecked`, which would have turned on
twenty rules nobody assessed. Switching the rules on surfaced seven real violations in
`backoffice/src/` — six `jsx-a11y`, one `!`. Six were fixed in the source. The seventh is
the `tabIndex` on the scrollable `role="log"` transcript, which is kept and which the
`no-noninteractive-tabindex` row above accounts for; no rule was demoted below `error`.

---

## 7. Testing style <a id="testing-style"></a>

| Rule | Detail |
|---|---|
| Framework | Kotest (`StringSpec`/`FunSpec`) or JUnit 5 — choose one per module, never both |
| Assertions | Kotest matchers or AssertJ. Never bare `assert`. |
| Test names | Full sentences describing behaviour: `"refuses to close a work item with an unmet success criterion"` |
| Structure | Arrange / Act / Assert, separated by blank lines. No shared mutable fixture state between tests. |
| Mocks | Forbidden in `core/` — hand-written fakes only. Permitted in adapters for genuinely external systems. |
| Time | Always injected. A test that calls `Instant.now()` fails ArchUnit's `TestRules`. |
| Filesystem | Every filesystem test gets a fresh temp directory and deletes it on teardown. |
| Flakes | A flaky test is a failing test. `@Disabled`/`@Ignored` requires its **annotation value** to name the work item that will fix it — `@Disabled("bean:NNNN: reason")` — which ArchUnit reads and asserts (§5.1). Not a comment: comments do not exist in bytecode. |
| Determinism | No random data without a fixed, logged seed. |

---

## 8. Documentation and comments

- Comments explain **why**, never **what**. A comment restating the code is deleted.
- Public API in `core/` carries KDoc stating the invariant it upholds, not its signature.
- No commented-out code. It is in git history.
- No `TODO`/`FIXME` — Detekt's `ForbiddenComment` fails the build. Unfinished work is a
  work item in `beans/`, referenced by id if a pointer is genuinely needed.
- Markdown in `documentation/` wraps at roughly 90 columns for reviewable diffs, but this
  is not tool-enforced — prose reflow by machine causes worse diffs than it prevents.

---

## 9. Adding or changing a style rule <a id="changing-a-style-rule"></a>

1. Open a work item.
2. Implement the rule in ktlint config, `detekt.yml`, a custom Detekt rule, or ArchUnit.
3. Fix the entire repository in the same pull request. No baseline, no freeze, no
   exclusion list. If the repository-wide fix is too large for one pull request, the rule
   is not ready.
4. Add a row to the relevant table in this document, including the "why".
5. If the rule replaces something a reviewer used to say, note that in the PR body — that
   is the whole point of the exercise.
