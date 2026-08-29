package uk.m4xy.modus.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaAnnotation
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvent
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * The mechanical expression of the Modus test taxonomy (`doc:35-testing`).
 *
 * The classpath already makes the split hard to get wrong — `modus.kotlin-base`
 * strips Spring off both unit-test classpaths and `assertUnitTestClasspathIsSpringFree`
 * holds them to an allowlist, so a misfiled test does not compile. These rules
 * are the second line: bytecode, so they hold if a module ever puts Spring back,
 * and they cover what a classpath cannot express — sleeping, disk, subprocesses,
 * and disabling a test with no owner.
 *
 * Scope: unlike [ArchitectureRulesTest], this class imports unit-test bytecode
 * ON PURPOSE. Rules qualified with [unitTestClasses] see only that; the two
 * unqualified rules apply to every imported class, production included.
 */
@AnalyzeClasses(packages = [ArchitectureRulesTest.ROOT])
class TestPurityRulesTest {
    /** A unit test that needs a Spring context was filed in the wrong drawer. */
    @ArchTest
    val unitTestsDoNotDependOnSpring: ArchRule =
        noClasses()
            .that(unitTestClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SPRING)
            .because("a unit test that needs a framework is an integration test — move it to src/integrationTest")

    /** Separate from [unitTestsDoNotDependOnSpring] so the failure names the fix. */
    @ArchTest
    val unitTestsAreNotSpringBootTests: ArchRule =
        noClasses()
            .that(unitTestClasses)
            .should()
            .beAnnotatedWith(SPRING_BOOT_TEST)
            .because("@SpringBootTest starts a context, which is the definition of an integration test")

    /** Not scoped to tests — nothing in Modus may call it. */
    @ArchTest
    val nothingSleepsTheThread: ArchRule =
        noClasses()
            .should()
            .callMethodWhere(
                object : DescribedPredicate<JavaMethodCall>("a java.lang.Thread.sleep method") {
                    override fun test(call: JavaMethodCall): Boolean =
                        call.targetOwner.isEquivalentTo(Thread::class.java) && call.target.name == "sleep"
                },
            ).because("sleeping is a race with a timer attached: await the condition or inject the clock")

    /**
     * A test that opens a file or a socket is neither fast nor isolated.
     *
     * `java.io..` is in the list as well as `java.nio.file..`, and it is the one
     * that matters: `File.createTempFile`, `File.readText` and `File.writeText`
     * are the idiomatic Kotlin route to the disk, so `java.io` is the LIKELY way
     * a filesystem-touching unit test gets written, not an exotic one. With only
     * `java.nio.file..` listed, a planted test that created, wrote and read a real
     * temp file passed. The ban is on the package, not on a hand-picked set of
     * classes: a list of `File`, `FileInputStream`, `RandomAccessFile`, … is the
     * same denylist shape that let this through the first time. `java.io.IOException`
     * and `java.io.Serializable` are caught too, which is intended — a unit test
     * that needs either is describing I/O it should not be doing.
     */
    @ArchTest
    val unitTestsDoNotTouchTheFilesystemOrTheNetwork: ArchRule =
        noClasses()
            .that(unitTestClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FILESYSTEM_IO, FILESYSTEM_NIO, NETWORK)
            .because("real I/O belongs in src/integrationTest, where it is allowed to be slow")

    /**
     * Separate from [unitTestsDoNotTouchTheFilesystemOrTheNetwork] so the failure
     * names the fix, and a call rather than a dependency because `ProcessBuilder`
     * and `Runtime` live in `java.lang` — no package ban can reach them.
     *
     * `doc:35-testing` §1 has always said a test that starts a process belongs in
     * `src/integrationTest`; until this rule existed that sentence was carried by
     * an `Enforced by:` that enforced nothing, and `ProcessBuilder("/bin/echo").start()`
     * in a unit test passed.
     */
    @ArchTest
    val unitTestsDoNotStartProcesses: ArchRule =
        noClasses()
            .that(unitTestClasses)
            .should()
            .callMethodWhere(
                object : DescribedPredicate<JavaMethodCall>("a process-starting method") {
                    override fun test(call: JavaMethodCall): Boolean =
                        PROCESS_STARTERS.any { (owner, methods) ->
                            call.targetOwner.isEquivalentTo(owner) && call.target.name in methods
                        }
                },
            ).because("a subprocess is a binary, a PATH and an exit code this machine happens to produce")

    /**
     * The enforcement doc:30 §5.1 specifies. Read out of the ANNOTATION VALUE,
     * which survives compilation; a `//` comment does not exist in bytecode.
     */
    @ArchTest
    val disabledCarriesAWorkItem: ArchRule =
        classes()
            .should(DisabledNamesAWorkItem())
            .because("a disabled test with no owner is a deleted test that still costs a build slot")

    /**
     * A guard on the guards. Every rule above is `noClasses(...)`, which passes
     * vacuously when nothing matched: a missing jar or a changed classifier would
     * quietly turn this file into a no-op. The expectation is derived, not listed
     * — `:architecture-tests:writeUnitTestPackages` reads the `package`
     * declaration out of every `src/test/kotlin` file in every analysed module.
     */
    @ArchTest
    fun everyUnitTestPackageIsAnalysed(classes: JavaClasses) {
        val manifest =
            checkNotNull(javaClass.getResourceAsStream(UNIT_TEST_PACKAGES)) {
                "$UNIT_TEST_PACKAGES is missing: :architecture-tests:writeUnitTestPackages did not run"
            }
        val expected =
            manifest
                .bufferedReader()
                .use { it.readLines() }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        check(expected.isNotEmpty()) { "$UNIT_TEST_PACKAGES is empty: no module has a unit test" }

        val analysed = classes.filter { unitTestClasses.test(it) }.map { it.packageName }.toSet()
        val missing = expected.filterNot { it in analysed }
        check(missing.isEmpty()) {
            "no unit-test bytecode was imported for: $missing (imported ${analysed.size} unit-test packages). " +
                "The test-purity rules would pass vacuously."
        }
    }

    /**
     * Reads `@Disabled`'s `value` off the class and off every method on it. The
     * value must open with a `bean:NNNN` reference and continue with a reason.
     */
    private class DisabledNamesAWorkItem : ArchCondition<JavaClass>("name a bean:NNNN work item in every @Disabled") {
        override fun check(
            item: JavaClass,
            events: ConditionEvents,
        ) {
            val sites =
                item.annotations.map { item.name to it } +
                    item.codeUnits.flatMap { unit -> unit.annotations.map { unit.fullName to it } }

            sites
                .filter { (_, annotation) -> annotation.rawType.name in DISABLING_ANNOTATIONS }
                .forEach { (owner, annotation) -> events.add(evaluate(item, owner, annotation)) }
        }

        private fun evaluate(
            item: JavaClass,
            owner: String,
            annotation: JavaAnnotation<*>,
        ): ConditionEvent {
            val value = annotation.get("value").orElse("").toString()
            val reference = WORK_ITEM.find(value)
            val reason =
                reference?.let {
                    value
                        .removeRange(it.range)
                        .trim()
                        .trimStart(':', '-')
                        .trim()
                }
            return if (reference != null && !reason.isNullOrBlank()) {
                SimpleConditionEvent.satisfied(item, "$owner is disabled by $reference")
            } else {
                SimpleConditionEvent.violated(
                    item,
                    "$owner: @Disabled(\"$value\") must open with a bean:NNNN reference and a reason, " +
                        "for example @Disabled(\"bean:0042: flaky under parallel execution\")",
                )
            }
        }
    }

    private companion object {
        const val SPRING = "org.springframework.."
        const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"
        const val FILESYSTEM_IO = "java.io.."
        const val FILESYSTEM_NIO = "java.nio.file.."
        const val NETWORK = "java.net.."

        /**
         * Every way the JDK starts a subprocess. `startPipeline` is static on
         * `ProcessBuilder` and starts several at once.
         */
        val PROCESS_STARTERS =
            listOf(
                ProcessBuilder::class.java to setOf("start", "startPipeline"),
                Runtime::class.java to setOf("exec"),
            )

        /** Generated by `:architecture-tests:writeUnitTestPackages`. */
        const val UNIT_TEST_PACKAGES = "/unit-test-packages.txt"

        /** `kotlin.test.Ignore` is a typealias for JUnit's `@Disabled`, so it lands here too. */
        val DISABLING_ANNOTATIONS = setOf("org.junit.jupiter.api.Disabled", "org.junit.Ignore")

        val WORK_ITEM = Regex("""^bean:\d{4}""")
    }
}
