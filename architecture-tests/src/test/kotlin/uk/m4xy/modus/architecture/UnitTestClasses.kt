package uk.m4xy.modus.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.core.importer.Location

/**
 * How a compiled unit test is recognised at analysis time.
 *
 * Every module publishes its `src/test` output as `<module>-unit-tests.jar` under
 * the `modus-unit-test-classes` usage (`modus.kotlin-base`), and
 * `:architecture-tests` resolves those jars onto its test runtime classpath. The
 * classifier separates a unit-test class from a production class in bytecode.
 *
 * `ImportOption.DoNotIncludeTests` is deliberately NOT how this works: it matches
 * a `build/classes/.../test/...` directory layout, which a jar location never
 * has, so on the purity rules it would exclude nothing and select nothing.
 * `TestPurityRulesTest.everyUnitTestPackageIsAnalysed` fails if this marker stops
 * matching.
 */
internal const val UNIT_TEST_JAR: String = "-unit-tests.jar"

/**
 * Keeps unit-test bytecode out of [ArchitectureRulesTest], which describes
 * production layering: a test depending on its own module is not a violation.
 */
class ExcludeUnitTestClasses : ImportOption {
    override fun includes(location: Location): Boolean = !location.contains(UNIT_TEST_JAR)
}

/** The classes the test-purity rules are about. */
internal val unitTestClasses: DescribedPredicate<JavaClass> =
    object : DescribedPredicate<JavaClass>("unit-test classes") {
        override fun test(javaClass: JavaClass): Boolean =
            javaClass.source
                .map { it.uri.toString().contains(UNIT_TEST_JAR) }
                .orElse(false)
    }
