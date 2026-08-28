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
| **ktlint** (via Spotless) | "Is it formatted?" | Diff noise, formatting debates |
| **Detekt** | "Is it written well, and does it avoid our known hazards?" | Complexity, dangerous APIs, project-specific mistakes |
| **ArchUnit** | "Is it in the right place, depending on the right things?" | Architectural drift |

All three run in the `check` task. `./gradlew check` is the gate; CI runs exactly the
same task with no extra arguments, so a green local run means a green CI run.

---

## 1. Formatting — ktlint via Spotless

Spotless owns formatting. ktlint's `ktlint_official` code style is the ruleset. There is
no negotiation about formatting; run `./gradlew spotlessApply` and move on.

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

Also under Spotless:

| Target | Tool |
|---|---|
| `*.kt`, `*.kts` | ktlint |
| `*.ts`, `*.tsx`, `*.css` in `backoffice/`, `e2e/` | Prettier |
| `*.md` in `documentation/`, `beans/` | Trailing-whitespace + final-newline + no-tabs only. Prose is not reflowed by a tool. |
| `*.yaml`, `*.json` | Two-space indent, sorted keys where order is not semantic |
| Every source file | License/`@file:` header check is **off** — headers are noise |

Tasks: `./gradlew spotlessCheck` (part of `check`), `./gradlew spotlessApply` (fixes).

**Never** commit with a Spotless violation and "fix it in review". `spotlessApply` takes
under two seconds.

---

## 2. Kotlin language rules

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

## 3. Detekt — standard configuration

Detekt runs with `buildUponDefaultConfig = true` and a project config at
`build-logic/src/main/resources/detekt.yml`. Deviations from Detekt's defaults:

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
| Formatting ruleset | **disabled entirely** | Spotless/ktlint owns formatting |

**Test sources:** relaxed for `LongMethod`, `MagicNumber`, `TooManyFunctions`, and
`MaxLineLength`. Everything else applies — test code is code.

**Baselines are forbidden.** A Detekt baseline file hides debt indefinitely. If a rule
cannot be satisfied today, either the rule is wrong (change it, with a rationale in this
document) or the code is wrong (fix it, or open a work item and disable the rule
repository-wide with a comment naming the work item). No `detekt-baseline.xml` may exist
in this repository. **Enforced by:** a Gradle check that fails if any file matching
`*baseline*.xml` exists.

---

## 4. Custom Detekt rules

These express Modus-specific hazards that no off-the-shelf rule covers. They live in
`build-logic` as a Detekt rule-set provider and run on every Kotlin module.

| Rule | What it catches | Why it exists |
|---|---|---|
| **`ForbiddenDomainApi`** | Any reference from `core/core-domain` to the forbidden list in `20-ddd-practices.md` §8 — Spring, Jackson, `java.io`/`java.nio.file`, `java.net`, SLF4J, `println`, `Instant.now()`, `UUID.randomUUID()`, `java.util.concurrent`, reflection, `lateinit`, `System.getenv`. | ArchUnit catches type dependencies but not *call sites* like `Instant.now()`. Detekt sees the AST, so it can. This is the rule that keeps the core pure. |
| **`NoFloatingPointMoney`** | `Float`/`Double` in any type whose name or property name matches money/cost/price/spend/usd. | Cost is the product. Floating-point money produces spend figures that do not add up, and nobody notices until an invoice does not reconcile. |
| **`UnevidencedMemoryWrite`** | A call to a `MemoryRepository.save`-shaped API whose argument is constructed without a non-empty evidence collection, where statically determinable. | The evidence rule (`00` §3) is the product's core promise. A best-effort static check plus the runtime schema validation is better than runtime alone. |
| **`ForbiddenTypeNameSuffix`** | Types under `core/` named `*Impl`, `*Manager`, `*Helper`, `*Util(s)`, `*Data`, `*Info`, `*Dto`, `*Entity`, `*Bean`, or `*Service` outside the domain-service allowlist. | These names describe position, not behaviour, and they attract unrelated code. Enforcing naming prevents the "junk drawer class". |
| **`DomainScopedRoute`** | A Spring mapping annotation in `adapter-rest` or `modules/*` whose path does not start with `/domains/{domainId}`, unless the class is on the bootstrap allowlist. | Permissions are domain-scoped (`00` §8). One un-scoped route is a cross-domain data leak. Detekt catches it at the annotation, before ArchUnit sees the compiled class. |
| **`NoBlockingInSuspend`** | `Thread.sleep`, `runBlocking`, blocking IO, or `.get()` on a future inside a `suspend` function. | The streaming adapters are the hot path for backoffice output; one blocking call stalls a shared dispatcher and every live stream stutters. |
| **`JustifiedSuppression`** | `@Suppress` with no trailing `//` comment explaining it. | A suppression with a reason is a decision; one without is an unexplained hole. |
| **`JustifiedVar`** | A `var` property in a class under `core/` with no explanatory comment. | Mutable aggregate state is legitimate but must be conscious. |
| **`RawTokenArithmetic`** | Arithmetic on a raw `Long`/`Int` named like a token count, instead of the `TokenCount` value object. | Context-budget and cost accounting must not silently mix units. |
| **`NoStringDomainId`** | A function parameter named `domainId`/`actorId`/`workItemId`/`runId` typed `String`. | Primitive obsession here means an id from one context can be passed where another is expected. |

**Severity:** all custom rules are `error`. **Adding a custom rule** requires: the rule
implementation, a test for the positive and negative case, a row in this table with a
"why", and a repository-wide clean run.

---

## 5. ArchUnit

ArchUnit enforces structure — what depends on what, what lives where, what a type must
implement. The complete rule set is derived from the tables in `10-architecture.md` §4.

Rule groups:

| Group | Enforces |
|---|---|
| `LayerDependencyRules` | The Gradle module dependency table (`10` §4.1) |
| `DomainPurityRules` | The package-level `core-domain` rules (`10` §4.2) |
| `ContextIsolationRules` | Bounded-context import allowlist (`10` §3.1) + no slice cycles |
| `AdapterRules` | Adapters implement ports; domain types do not escape; no DTOs in core (`10` §4.3) |
| `RestRules` | Every route is domain-scoped; no field injection; controllers return DTOs |
| `NamingRules` | Package placement matches type kind (`20` §5.1) |
| `NoDatabaseRules` | No JDBC/JPA/ORM/SQL types anywhere (`00` §2) |
| `TestRules` | No mocking framework in `core/`; no wall-clock dependency in any test; no `@Disabled` without a work-item reference |

ArchUnit tests live in `build-logic`'s `modus.archunit` convention plugin and are applied
to every Kotlin module, so a new module gets them automatically. Freezing (ArchUnit's
`FreezingArchRule`) is **forbidden** for the same reason Detekt baselines are.

---

## 6. TypeScript and the backoffice

| Concern | Tool | Setting |
|---|---|---|
| Formatting | Prettier (via Spotless) | 2-space indent, 100 columns, single quotes, trailing commas |
| Linting | ESLint | `@typescript-eslint` strict + `react-hooks` + `jsx-a11y` |
| Types | `tsc --noEmit` | `strict: true`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noImplicitOverride` |
| `any` | ESLint `no-explicit-any` | `error`. Use `unknown` and narrow. |
| Non-null assertion `!` | ESLint `no-non-null-assertion` | `error` |
| Import cycles | ESLint `import/no-cycle` | `error` |
| Accessibility | `jsx-a11y` recommended | `error`; plus axe assertions in Playwright |
| API types | Generated from the OpenAPI document | Hand-written API types are forbidden — they drift |
| Dead code | `knip` | `error` in CI |

`./gradlew check` runs the backoffice checks too, so there is one command to remember.

---

## 7. Testing style

| Rule | Detail |
|---|---|
| Framework | Kotest (`StringSpec`/`FunSpec`) or JUnit 5 — choose one per module, never both |
| Assertions | Kotest matchers or AssertJ. Never bare `assert`. |
| Test names | Full sentences describing behaviour: `"refuses to close a work item with an unmet success criterion"` |
| Structure | Arrange / Act / Assert, separated by blank lines. No shared mutable fixture state between tests. |
| Mocks | Forbidden in `core/` — hand-written fakes only. Permitted in adapters for genuinely external systems. |
| Time | Always injected. A test that calls `Instant.now()` fails ArchUnit's `TestRules`. |
| Filesystem | Every filesystem test gets a fresh temp directory and deletes it on teardown. |
| Flakes | A flaky test is a failing test. `@Disabled`/`@Ignored` requires a comment naming the work item that will fix it; ArchUnit enforces the comment. |
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

## 9. Adding or changing a style rule

1. Open a work item.
2. Implement the rule in ktlint config, `detekt.yml`, a custom Detekt rule, or ArchUnit.
3. Fix the entire repository in the same pull request. No baseline, no freeze, no
   exclusion list. If the repository-wide fix is too large for one pull request, the rule
   is not ready.
4. Add a row to the relevant table in this document, including the "why".
5. If the rule replaces something a reviewer used to say, note that in the PR body — that
   is the whole point of the exercise.
