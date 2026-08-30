package uk.m4xy.modus.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import java.io.PrintStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The mechanical expression of the Modus architecture.
 *
 * These rules are the reason code review never has to argue about layering: a
 * violation is a build failure, not a comment. If a rule is wrong, changing it
 * is an architectural decision that belongs in `documentation/`, not a
 * convenience edit made to get a branch green.
 */
@AnalyzeClasses(
    packages = [ArchitectureRulesTest.ROOT],
    // DoNotIncludeTests only recognises test *directories*. Unit-test bytecode
    // now also reaches this classpath as jars, for TestPurityRulesTest, so it
    // has to be excluded by classifier too — these rules are about production
    // code and a test may legitimately depend on the module it tests.
    importOptions = [ImportOption.DoNotIncludeTests::class, ExcludeUnitTestClasses::class],
)
class ArchitectureRulesTest {
    /**
     * core-domain is pure Kotlin: no Spring, no Jackson, no jakarta/javax, no
     * serialisation or logging framework. Nothing that makes the domain model
     * hostage to a runtime.
     */
    @ArchTest
    val domainIsFrameworkFree: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "com.fasterxml.jackson..",
                "jakarta..",
                "javax..",
                "org.slf4j..",
                "org.apache..",
                "kotlinx.serialization..",
                "org.springdoc..",
            ).because("the domain model must outlive any framework it is currently wired into")

    @ArchTest
    val domainDependsOnNoOuterLayer: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(APPLICATION, ADAPTERS, MODULES, APP)
            .because("dependencies point inwards and the domain is the innermost layer")

    @ArchTest
    val applicationDependsOnDomainOnly: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTERS, MODULES, APP)
            .because("use cases talk to ports they own, never to an adapter or an installable module")

    @ArchTest
    val applicationIsFreeOfDeliveryConcerns: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..",
                "org.springframework.boot..",
                "jakarta.servlet..",
                "org.springdoc..",
            ).because("delivery and storage concerns belong in adapters")

    @ArchTest
    val adaptersDoNotDependOnEachOther: ArchRule =
        slices()
            .matching("$ROOT.adapter.(*)..")
            .should()
            .notDependOnEachOther()
            .because("adapters are swappable; coupling two of them makes both unswappable")

    @ArchTest
    val adaptersDoNotDependOnModulesOrApp: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(ADAPTERS)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(MODULES, APP)
            .because("adapters are wired by the app, they do not reach back into it")

    @ArchTest
    val modulesDoNotDependOnEachOther: ArchRule =
        slices()
            .matching("$ROOT.module.(*)..")
            .should()
            .notDependOnEachOther()
            .because("a domain may install any subset of modules, so no module may assume another is present")

    @ArchTest
    val modulesDoNotDependOnAdaptersOrApp: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(MODULES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTERS, APP)
            .because("modules extend the domain, they do not pick a delivery or storage mechanism")

    @ArchTest
    val nothingDependsOnTheApp: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage(APP)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(APP)
            .because("app/modus-server is wiring only, and nothing may depend on wiring")

    /** domain <- application <- adapters / modules <- app. */
    @ArchTest
    val layeringHolds: ArchRule =
        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("$ROOT..")
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Application")
            .definedBy(APPLICATION)
            .layer("Adapters")
            .definedBy(ADAPTERS)
            .layer("Modules")
            .definedBy(MODULES)
            .layer("App")
            .definedBy(APP)
            .whereLayer("App")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters")
            .mayOnlyBeAccessedByLayers("App")
            .whereLayer("Modules")
            .mayOnlyBeAccessedByLayers("App")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Adapters", "Modules", "App")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Adapters", "Modules", "App")

    /**
     * `doc:10-architecture` §4.2 `PublishedLanguageIsLeaf`, and the rule §3.1 rests on:
     * one context may import another's published language precisely because that package
     * drags nothing behind it.
     *
     * The origin's own context decides what is legal, which no `dependOnClassesThat`
     * predicate can see — it is handed the target only — so this is an `ArchCondition`.
     *
     * The one exemption is the shared-kernel [uk.m4xy.modus.core.domain.DomainEvent]
     * marker, named in §4.2. Without it every context would declare an identical event
     * interface of its own and there would be no type to dispatch a cross-context event
     * as.
     */
    @ArchTest
    val publishedLanguageIsLeaf: ArchRule =
        classes()
            .that()
            .resideInAnyPackage(PUBLISHED_LANGUAGE, DOMAIN_EVENTS)
            .should(dependOnlyOnLeafSafeTypes())
            .because("a published package that drags a dependency behind it is not safe for another context to import")

    /**
     * `doc:10-architecture` §4.2 `AggregatesAreSealedOrFinal`. An `open` aggregate root is
     * neither `final` nor `sealed`, so a subclass outside the boundary could override an
     * invariant check. `..domain.aggregate` is what gives the rule a decidable scope
     * (`doc:20-ddd-practices` §5.1).
     */
    @ArchTest
    val aggregatesAreSealedOrFinal: ArchRule =
        classes()
            .that()
            .resideInAPackage(AGGREGATES)
            .should(beFinalOrSealed())
            .because("an open aggregate root lets a subclass outside the boundary override an invariant")

    /**
     * The shared kernel is itself a leaf. `adr:0004-domain-id-shared-kernel` makes
     * [SHARED_KERNEL_DOMAIN_ID] importable from every context's published package, which
     * only stays safe while the kernel drags nothing behind it — a dependency added here
     * would be one every context inherits without seeing it.
     *
     * Scoped by name rather than by package, for the reason [SHARED_KERNEL] records.
     */
    @ArchTest
    val sharedKernelIsLeaf: ArchRule =
        classes()
            .that(areSharedKernel())
            .should(dependOnlyOnLeafSafeTypes("the Kotlin stdlib, java.time and the rest of the shared kernel"))
            .because("every context imports the shared kernel, so anything it drags behind it is imported unseen")

    /**
     * `doc:15-repository-layout#core-package-rules` §4.2 `PortsAreInterfaces`, at the scope
     * that document gives it: **every** `..domain.port..` package, context-scoped ones
     * included. One of the five §4.2 rules that paragraph records as not existing — until
     * this, "a port that is not an interface" merged green.
     *
     * A port with an implementation inside `core-domain` is not a port:
     * `doc:00-constitution` §1.2 declares the interface inside and puts every implementation
     * outside. The three port packages that exist today — `identity.port` twice and
     * `domainmgmt.port` — are what make this rule observable on real code rather than on an
     * empty package.
     *
     * `bean:0027` carries the audit of the remaining four.
     */
    @ArchTest
    val portsAreInterfaces: ArchRule =
        classes()
            .that()
            .resideInAPackage(ALL_PORTS)
            .should()
            .beInterfaces()
            .because("a port with an implementation inside core-domain is not a port (doc:00-constitution §1.2)")

    /**
     * The context-free ambient-capability ports are additionally **leaf**.
     *
     * This is deliberately narrower than [portsAreInterfaces] and cannot be merged into it.
     * A per-context port legitimately names its own context: `PermissionGrantRepository`
     * imports `PermissionGrant`, `ActorId` and `GrantId`, exactly as
     * `doc:20-ddd-practices#ports-and-adapters` §5.2 intends — a repository port speaks in
     * its own aggregate's types. Applying a leaf condition at `..domain.port..` scope would
     * reject all three ports that exist today.
     *
     * What makes `..core.domain.port` different is that it belongs to no context and every
     * context injects it, so a dependency added here is one every context inherits without
     * seeing it — the same argument `rule:archunit/sharedKernelIsLeaf` rests on, for the same
     * reason. A port returning `identity.published.ActorId` would put one context's published
     * language on the injection path of every other context, including those
     * `doc:10-architecture#bounded-contexts` §3.1 forbids from importing `identity` at all.
     *
     * Nothing on `main` could see this package before `bean:0065`: [SHARED_KERNEL] is an
     * exact name set and [PUBLISHED_LANGUAGE] a `*.published..` wildcard.
     */
    @ArchTest
    val ambientCapabilityPortsAreLeaf: ArchRule =
        classes()
            .that()
            .resideInAPackage(DOMAIN_PORTS)
            .should(dependOnlyOnLeafSafeTypes("the Kotlin stdlib and java.time"))
            .because("every context injects an ambient capability, so anything it drags behind it is imported unseen")

    @ArchTest
    val thereAreNoPackageCycles: ArchRule =
        slices()
            .matching("$ROOT.(**)")
            .should()
            .beFreeOfCycles()
            .because("a cycle is a boundary that was never actually drawn")

    /**
     * Nothing writes to `System.out`/`System.err`. Kotlin's `println` is
     * `@InlineOnly` and compiles straight to `PrintStream.println`, so this
     * catches it at the call site.
     *
     * This lives here rather than in `config/detekt/detekt.yml` because
     * Detekt's `ForbiddenMethodCall` needs type resolution, which Modus cannot
     * run — see the "Type resolution" section of that file. ArchUnit reads
     * bytecode and has the whole module graph on its classpath, so it can.
     */
    @ArchTest
    val nothingWritesToTheStandardStreams: ArchRule =
        noClasses()
            .should()
            .callMethodWhere(
                object : DescribedPredicate<JavaMethodCall>("a java.io.PrintStream print or println method") {
                    override fun test(call: JavaMethodCall): Boolean =
                        call.targetOwner.isAssignableTo(PrintStream::class.java) &&
                            call.target.name in PRINT_METHODS
                },
            ).because("use a structured logger or the execution output stream, not stdout")

    /**
     * Time is injected, never read from a static clock. The no-argument
     * overloads are banned; `Instant.now(clock)` and friends are exactly the
     * shape this rule is pushing code towards, so they stay legal.
     *
     * Also a replacement for a dead `ForbiddenMethodCall` entry — see above.
     */
    @ArchTest
    val timeIsInjectedNeverReadFromAStaticClock: ArchRule =
        noClasses()
            .should()
            .callMethod(Instant::class.java, "now")
            .orShould()
            .callMethod(LocalDate::class.java, "now")
            .orShould()
            .callMethod(LocalDateTime::class.java, "now")
            .because("inject a Clock so time is testable")

    /**
     * A guard on the guards. Every rule above is a `noClasses(...)` assertion,
     * which is vacuously satisfiable if nothing was imported — a misconfigured
     * classpath would quietly turn this whole file into a no-op. Fail loudly.
     *
     * The expectation is not a literal. `:architecture-tests:writeAnalysedPackages`
     * reads the `package` declaration out of every main-source Kotlin file in
     * every subproject and writes them to [ANALYSED_PACKAGES]; the same derived
     * project list puts those modules on this classpath. A module added to
     * `settings.gradle.kts` is therefore analysed, or this test fails — no
     * second list to keep in sync, and nothing to forget.
     */
    @ArchTest
    fun everyModuleIsOnTheAnalysedClasspath(classes: JavaClasses) {
        val manifest =
            checkNotNull(javaClass.getResourceAsStream(ANALYSED_PACKAGES)) {
                "$ANALYSED_PACKAGES is missing: :architecture-tests:writeAnalysedPackages did not run"
            }
        val expected =
            manifest
                .bufferedReader()
                .use { it.readLines() }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        check(expected.isNotEmpty()) { "$ANALYSED_PACKAGES is empty: no module sources were discovered" }

        val packages = classes.map { it.packageName }.toSet()
        val missing = expected.filterNot { it in packages }
        check(missing.isEmpty()) { "ArchUnit imported nothing for: $missing (imported ${packages.size} packages)" }
    }

    /**
     * [portsAreInterfaces] perceives every port package, asserted rather than assumed.
     *
     * `doc:15-repository-layout#core-package-rules` §4.2 renders this rule's scope as
     * `..domain.port..`, which matches **neither** the live `domain.<ctx>.port` nor the
     * context-free `domain.port` — the sibling `PublishedLanguageSourceIsLeaf` row in the
     * same table writes `<ctx>` explicitly, and this one does not. Implementing the rule
     * therefore meant *choosing* its real scope rather than inheriting one, so the choice is
     * asserted here: `$DOMAIN_ROOT..port..` must reach all three packages that exist.
     *
     * Without this, a glob that silently matched only the context-free package would leave
     * `identity.port` and `domainmgmt.port` unguarded and the rule green — the §4.2 row being
     * closed in name only. `bean:0095` carries correcting the document.
     */
    @ArchTest
    fun everyPortPackageIsSeenByPortsAreInterfaces(classes: JavaClasses) {
        val seen =
            classes
                .filter { it.packageName.startsWith("$DOMAIN_ROOT.") && it.packageName.endsWith(".port") }
                .map { it.packageName.removePrefix("$DOMAIN_ROOT.") }
                .toSortedSet()

        check(seen == PORT_PACKAGES) {
            "portsAreInterfaces is scoped at '$ALL_PORTS' and reaches $seen, but the port " +
                "packages in core-domain are $PORT_PACKAGES. A package the glob misses is a " +
                "package the rule leaves unguarded while reporting success " +
                "(doc:00-constitution#observed-failing)"
        }
    }

    /**
     * Non-vacuity and membership for [ambientCapabilityPortsAreLeaf]. **Nothing more.**
     *
     * An earlier version of this KDoc sold it as the answer to "green forever while
     * examining nothing", and `bean:0065` criterion 4 banked on that. It is not. This
     * asserts which **names** reside in the package; the escape that got past the gate was
     * in what those names **declare** — a `@JvmInline value class` return type, which leaves
     * the name set untouched. This guard stayed green through that escape and was right to.
     *
     * Perception asserted at the wrong granularity is worse than perception not asserted,
     * because it discharges the obligation in the author's mind and in the criterion table.
     * The declaration-level perception this package actually needs is
     * [AmbientCapabilityPortSourcePerceptionTest], over source, where the type survives.
     *
     * What this is still worth: it fails if the package empties or is renamed, and it makes
     * adding a port a deliberate edit. Be honest about the mechanism — both constants live a
     * few lines from the rule and the message ends "update this set deliberately", so it
     * **enforces a pause, not an invariant** (`doc:00-constitution#mechanical-enforcement`).
     * A pause is worth something in review and is not a gate.
     *
     * Scoped with the rule's own `..`, not `==`: [ambientCapabilityPortsAreLeaf] covers
     * subpackages, and a guard narrower than the rule it guards reports on a set the rule
     * does not have. A port in `..core.domain.port.internal` was verified invisible to the
     * `==` form, value-class return and all.
     */
    @ArchTest
    fun everyAmbientCapabilityPortIsSeenByItsOwnRule(classes: JavaClasses) {
        val seen =
            classes
                .filter {
                    it.packageName == DOMAIN_PORT_PACKAGE ||
                        it.packageName.startsWith("$DOMAIN_PORT_PACKAGE.")
                }.map { it.simpleName }
                .toSortedSet()

        check(seen.isNotEmpty()) {
            "ambientCapabilityPortsAreLeaf selected no class at all: nothing resides in " +
                "$DOMAIN_PORT_PACKAGE or below it, so the rule is vacuously satisfied and " +
                "enforces nothing (doc:00-constitution#observed-failing)"
        }
        check(seen == AMBIENT_CAPABILITY_PORTS) {
            "$DOMAIN_PORT_PACKAGE and its subpackages hold $seen, but the rule is declared to " +
                "cover $AMBIENT_CAPABILITY_PORTS. A port added without being named here is a " +
                "port nobody chose to put on every context's injection path. This checks names " +
                "only — what they declare is AmbientCapabilityPortSourcePerceptionTest's job. " +
                "Update this set deliberately."
        }
    }

    companion object {
        const val ROOT: String = "uk.m4xy.modus"

        private const val DOMAIN_ROOT = "$ROOT.core.domain"
        private const val DOMAIN = "$DOMAIN_ROOT.."
        private const val APPLICATION = "$ROOT.core.application.."
        private const val ADAPTERS = "$ROOT.adapter.."
        private const val MODULES = "$ROOT.module.."
        private const val APP = "$ROOT.app.."

        /**
         * The context-free ambient-capability port package (`bean:0065`). A sibling of the
         * shared kernel, not a member of it: [SHARED_KERNEL] is a name set, so
         * [sharedKernelIsLeaf] cannot see this package however it is spelled.
         */
        private const val DOMAIN_PORT_PACKAGE = "$DOMAIN_ROOT.port"
        private const val DOMAIN_PORTS = "$DOMAIN_PORT_PACKAGE.."

        /**
         * Every port package, context-scoped and context-free alike — the scope
         * `doc:15-repository-layout#core-package-rules` §4.2 gives `PortsAreInterfaces`.
         */
        private const val ALL_PORTS = "$DOMAIN_ROOT..port.."

        /**
         * The ports [ambientCapabilityPortsAreLeaf] is declared to cover, asserted
         * by [everyAmbientCapabilityPortIsSeenByItsOwnRule]. Written out so that adding one is
         * a visible edit rather than a silent widening of what every context injects.
         */
        private val AMBIENT_CAPABILITY_PORTS = sortedSetOf("ClockPort", "IdGeneratorPort", "RandomPort")

        /**
         * Every port package [portsAreInterfaces] must reach, relative to `$DOMAIN_ROOT`.
         * Written out so that the glob's reach is asserted rather than assumed, and so that a
         * new context's port package is a deliberate edit here.
         */
        private val PORT_PACKAGES = sortedSetOf("domainmgmt.port", "identity.port", "port")

        private const val PUBLISHED_LANGUAGE = "$DOMAIN_ROOT.*.published.."
        private const val DOMAIN_EVENTS = "$DOMAIN_ROOT.*.event.."
        private const val AGGREGATES = "$DOMAIN_ROOT.*.aggregate.."

        /** The shared-kernel event marker, exempt from [publishedLanguageIsLeaf] by `doc:10` §4.2. */
        private const val SHARED_KERNEL_EVENT = "$DOMAIN_ROOT.DomainEvent"

        /**
         * The tenant identifier, exempt from [publishedLanguageIsLeaf] by `doc:10` §4.2
         * (`adr:0004-domain-id-shared-kernel`). Every context's events name the domain they
         * concern, and a published package is a leaf, so a per-context `DomainId` would be
         * one tenant with as many unequal types as there are contexts.
         */
        private const val SHARED_KERNEL_DOMAIN_ID = "$DOMAIN_ROOT.DomainId"

        /**
         * The shared kernel, by name. Not `$DOMAIN_ROOT` as a package: `BoundedContexts`
         * lives there too and legitimately references every context marker, so a package
         * wildcard would either fail on it or have to carve it out. Naming the members is
         * also what stops the [publishedLanguageIsLeaf] exemption widening by accident —
         * adding one is an edit here, visible in review. If a third member ever arrives,
         * move all of them to a `..domain.kernel` package and scope both rules to it
         * structurally, the way `..domain.aggregate` scopes [aggregatesAreSealedOrFinal]
         * (`doc:20-ddd-practices` §5.1).
         */
        private val SHARED_KERNEL = setOf(SHARED_KERNEL_EVENT, SHARED_KERNEL_DOMAIN_ID)

        /**
         * Membership is decided on the **outermost enclosing class**, because Kotlin
         * generates classes the source does not name. A `private companion object` becomes
         * `DomainId${'$'}Companion`, and a top-level `private val` becomes a `DomainIdKt` file
         * facade. Matching the exact name alone put the companion outside its own kernel and
         * the facade outside it permanently — both observed, on the first two runs of
         * [sharedKernelIsLeaf]. The facade stays excluded by design, which is why
         * [SHARED_KERNEL_DOMAIN_ID] keeps its regex in a companion rather than at file scope.
         *
         * The walk is structural, via [JavaClass.getEnclosingClass]. An earlier version split
         * the binary name on `${'$'}`, which is textual and therefore forgeable: Kotlin permits
         * `${'$'}` in a backticked type name, so a **top-level** `` `DomainId${'$'}Evil` `` in this
         * package joined the shared kernel without anyone editing [SHARED_KERNEL]. Review
         * planted exactly that and the build stayed green — so the claim that the exemption
         * cannot widen without a visible edit here was false as written.
         */
        private fun isSharedKernel(javaClass: JavaClass): Boolean = outermost(javaClass).name in SHARED_KERNEL

        private tailrec fun outermost(javaClass: JavaClass): JavaClass {
            val enclosing = javaClass.enclosingClass.orElse(null) ?: return javaClass
            return outermost(enclosing)
        }

        /**
         * The Kotlin standard library plus the `java.*` packages it erases to. `java.util`
         * and `java.lang` are prefixes of themselves only in the sense that
         * `java.util.concurrent` and `java.lang.reflect` are forbidden in the domain by
         * their own rules, not by this one.
         *
         * `org.jetbrains.annotations` is stdlib, not a third-party dependency: `kotlinc`
         * emits `@NotNull`/`@Nullable` onto every generated member, and `kotlin-stdlib`
         * carries `org.jetbrains:annotations` as an `api` dependency. Excluding it would
         * make the rule unsatisfiable for every Kotlin type ever written.
         */
        private val LEAF_SAFE_PACKAGES = setOf("kotlin", "java.lang", "java.util", "org.jetbrains.annotations")

        /**
         * The context segment of `uk.m4xy.modus.core.domain.<ctx>.…`, or [NO_CONTEXT] for a
         * type that sits directly in `$DOMAIN_ROOT` — the shared kernel, and
         * `BoundedContexts`. Those belong to no context, so no `..published..` package is
         * their own, which is what [NO_CONTEXT] makes explicit: an earlier version let
         * `removePrefix` no-op and yielded `"uk"`, reaching the right answer by accident.
         */
        private fun contextOf(javaClass: JavaClass): String =
            javaClass.packageName
                .takeIf { it.startsWith("$DOMAIN_ROOT.") }
                ?.removePrefix("$DOMAIN_ROOT.")
                ?.substringBefore('.')
                ?: NO_CONTEXT

        // No context owns this type, so no context's published language is "its own".
        private const val NO_CONTEXT = ""

        private fun isLeafSafe(
            target: JavaClass,
            context: String,
        ): Boolean {
            if (target.isPrimitive || target.isArray) return true
            val targetPackage = target.packageName
            return LEAF_SAFE_PACKAGES.any { targetPackage == it || targetPackage.startsWith("$it.") } ||
                targetPackage == "java.time" ||
                targetPackage.startsWith("java.time.") ||
                targetPackage == "$DOMAIN_ROOT.$context.published" ||
                isSharedKernel(target)
        }

        private fun areSharedKernel(): DescribedPredicate<JavaClass> =
            object : DescribedPredicate<JavaClass>("the shared kernel (${SHARED_KERNEL.joinToString(", ")})") {
                override fun test(javaClass: JavaClass): Boolean = isSharedKernel(javaClass)
            }

        private fun dependOnlyOnLeafSafeTypes(
            permitted: String =
                "the Kotlin stdlib, java.time, their own context's published language " +
                    "and the shared kernel " + SHARED_KERNEL.joinToString(", "),
        ): ArchCondition<JavaClass> =
            object : ArchCondition<JavaClass>("depend on nothing beyond $permitted") {
                override fun check(
                    item: JavaClass,
                    events: ConditionEvents,
                ) {
                    val context = contextOf(item)
                    item.directDependenciesFromSelf
                        .filterNot { isLeafSafe(it.targetClass, context) }
                        .forEach { events.add(SimpleConditionEvent.violated(item, it.description)) }
                }
            }

        private fun beFinalOrSealed(): ArchCondition<JavaClass> =
            object : ArchCondition<JavaClass>("be final or sealed") {
                override fun check(
                    item: JavaClass,
                    events: ConditionEvents,
                ) {
                    val sealedOrFinal = item.modifiers.contains(JavaModifier.FINAL) || item.reflect().isSealed
                    if (!sealedOrFinal) {
                        events.add(
                            SimpleConditionEvent.violated(
                                item,
                                "${item.name} is neither final nor sealed, so it is an open aggregate",
                            ),
                        )
                    }
                }
            }

        /** Generated by `:architecture-tests:writeAnalysedPackages`. */
        private const val ANALYSED_PACKAGES = "/analysed-packages.txt"

        private val PRINT_METHODS = setOf("print", "println")
    }
}
