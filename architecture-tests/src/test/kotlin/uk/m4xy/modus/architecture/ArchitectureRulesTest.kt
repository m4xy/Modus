package uk.m4xy.modus.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
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

    companion object {
        const val ROOT: String = "uk.m4xy.modus"

        private const val DOMAIN = "$ROOT.core.domain.."
        private const val APPLICATION = "$ROOT.core.application.."
        private const val ADAPTERS = "$ROOT.adapter.."
        private const val MODULES = "$ROOT.module.."
        private const val APP = "$ROOT.app.."

        /** Generated by `:architecture-tests:writeAnalysedPackages`. */
        private const val ANALYSED_PACKAGES = "/analysed-packages.txt"

        private val PRINT_METHODS = setOf("print", "println")
    }
}
