# Modus — agent operating notes

Operational notes for agents and humans working in this repository. It is
deliberately short. **`documentation/` is the authority** on what Modus is, its
domain model and its architectural decisions; when this file and
`documentation/` disagree, `documentation/` wins and this file is the bug.

## Build, test, lint

Everything is Gradle. Requires JDK 25 (the wrapper provisions the rest).

```bash
./gradlew qualityCheck   # THE gate. Every module's check + build scripts + build-logic.
                         # CI runs exactly this command and nothing else.
./gradlew build          # compile + test + every module-level gate (not build-logic)
./gradlew ktlintCheck    # style only
./gradlew ktlintFormat   # fix style automatically — do this instead of hand-formatting
./gradlew detekt         # static analysis only
./gradlew :architecture-tests:test   # the architecture rules
./gradlew :modus-server:bootRun      # run the server
```

`build-logic` is an included build, so its own ktlint/Detekt/`allWarningsAsErrors`
gates are not reached by `build`. `qualityCheck` asks for them by name. If you
change a convention plugin, run `qualityCheck`.

Versions live in `gradle/libs.versions.toml` and nowhere else. Never write a
version literal into a module's `build.gradle.kts`.

## Module graph

```
core/core-domain                        pure Kotlin, ZERO framework dependencies
core/core-application                   use cases; depends on core-domain only
adapters/adapter-persistence-flatfile   durable flat-file store
adapters/adapter-rest                   /domains/{domainId} REST + SSE/WS
adapters/adapter-agent-claude           Claude Code process supervision + streaming
adapters/adapter-vcs-git                git-backed repository operations
modules/module-beans                    work tracking (installable per domain)
modules/module-cost                     LLM spend tracking (installable per domain)
app/modus-server                        Spring Boot application — wiring ONLY
architecture-tests                      the rules below, as failing tests
build-logic                             convention plugins
```

Root package `uk.m4xy.modus`. Bounded contexts inside `core-domain`:
`identity`, `domainmgmt`, `work`, `memory`, `execution`, `cost` — each has a
README describing its remit.

## Dependency rules

Dependencies point inwards. These are enforced by ArchUnit in
`architecture-tests`, so a violation is a build failure, not a review comment:

- `core-domain` depends on **nothing** framework-related — no Spring, no
  Jackson, no jakarta/javax, no logging framework — and on no outer layer.
- `core-application` may depend on `core-domain` **only**. Never on an adapter,
  a module or the app.
- Adapters must not depend on each other, on modules, or on the app.
- Modules must not depend on each other (a domain installs any subset), nor on
  adapters or the app.
- Nothing may depend on `app/modus-server`. It is wiring only; logic there is a
  bug.
- No package cycles anywhere.

If a rule genuinely needs to change, that is an architectural decision: record
it in `documentation/` and change the rule deliberately. Never weaken a rule to
get a branch green.

## Code style is mechanical

ktlint (`ktlint_official`, configured in `.editorconfig`) and Detekt
(`config/detekt/detekt.yml`) decide style and complexity. **Style is never a
review comment.** If the tools pass, the style is correct; if they fail, run
`./gradlew ktlintFormat` or fix the finding. To change a rule, change the
config — with a comment saying why — not the call site.

Kotlin explicit API mode is on: public declarations need explicit visibility and
return types.

Detekt runs PSI-only (no type resolution — see the header of
`config/detekt/detekt.yml` for why, and the `Enforcement gap:` it records). Every
rule that needs type resolution is listed there with `active: false`. Do not turn
one on: it will not fire, and a rule that cannot fire is worse than an admitted
gap. The bans worth keeping — no `println`, no static `java.time` clocks — are
enforced by ArchUnit in `architecture-tests`, which reads bytecode.

Every module applies a convention plugin from `build-logic/`
(`modus.kotlin-base`, `modus.spring-module`, `modus.spring-app`). No module
configures Kotlin, ktlint or Detekt itself.

## Workflow

**No direct commits to `main`.** Always:

1. Pick or create a work item in `beans/` (`NNNN-slug.md`, `hmans/beans`
   front-matter: `title`, `status`, `type`, `priority`, `created_at`).
2. Branch from `main`, named after the work: `feat/…`, `fix/…`, `docs/…`,
   `chore/…`.
3. Commit with conventional-commit messages.
4. Open a PR that references the work item and states the evidence — the
   commands run and their observed output — that the acceptance criteria are met.
5. Review, then merge. CI (`.github/workflows/ci.yml`) must be green.

Never claim a green build you did not observe. Paste the output.

## Agent context budget

Keep an agent's working context under **300k tokens**. Do not paste dependency
trees, full build scans, generated reports or whole files into context. Read the
part you need, summarise, and link to the file path instead. If a task cannot
fit, split it into separate work items rather than growing the context.

## Not yours unless the work item says so

`documentation/`, `backoffice/` and `e2e/` are owned by other work streams.
Placeholder sources across the modules are marked provisional and are replaced
by the work item that implements that area — do not build on them.
