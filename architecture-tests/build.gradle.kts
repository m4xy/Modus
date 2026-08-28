import org.gradle.api.attributes.Usage

// The architectural gate. This module has no production code: it puts every
// other module on the test classpath and asserts the dependency rules that the
// module graph is supposed to guarantee.
//
// If a rule here starts failing, the fix is to change the code, not the rule.
//
// Nothing in this file names a module. Both the analysed classpath and the
// manifest the guard test checks against are derived from the project
// structure, so `settings.gradle.kts` is the only place a module has to be
// registered. A module that is added and forgotten cannot escape analysis,
// because there is nothing to forget.
plugins {
    id("modus.kotlin-base")
}

/** Every Modus module except this one. Derived, never listed. */
val analysedProjects = rootProject.subprojects.filter { it.path != project.path }

dependencies {
    analysedProjects.forEach { testImplementation(project(it.path)) }

    testImplementation(platform(libs.springBoot.bom))
    testImplementation(libs.archunit.junit6)
}

// --- The unit-test bytecode the test-purity rules analyse ------------------
// `TestPurityRulesTest` asserts against compiled UNIT-TEST classes, which no
// ordinary dependency puts here: a project dependency publishes main output and
// nothing else. Modules expose theirs under the `modus-unit-test-classes` usage
// (`modus.kotlin-base`), resolved through the same derived project list as
// everything else — a new module's unit tests are analysed automatically.
val analysedUnitTests = configurations.dependencyScope("analysedUnitTests")

val analysedUnitTestClasses =
    configurations.resolvable("analysedUnitTestClasses") {
        description = "Compiled unit-test classes of every analysed module."
        extendsFrom(analysedUnitTests.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "modus-unit-test-classes"))
        }
    }

dependencies {
    analysedProjects.forEach { add(analysedUnitTests.name, project(it.path)) }
}

sourceSets.named("test") {
    runtimeClasspath += files(analysedUnitTestClasses)
}

/**
 * Writes the packages that ArchUnit MUST import, read straight out of the
 * `package` declaration of every main-source Kotlin file in every analysed
 * module. `ArchitectureRulesTest.everyModuleIsOnTheAnalysedClasspath` asserts
 * against this file, so the guard's expectations and the classpath are derived
 * from the same source and cannot drift apart.
 */
abstract class WriteAnalysedPackages : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun write() {
        val packages =
            sources.files
                .filter { it.isFile && it.extension == "kt" }
                .mapNotNull { file ->
                    file
                        .readLines()
                        .firstOrNull { it.startsWith("package ") }
                        ?.removePrefix("package ")
                        ?.trim()
                        ?.removeSuffix(";")
                }.toSortedSet()
        manifest.get().asFile.writeText(packages.joinToString(separator = "\n", postfix = "\n"))
    }
}

val analysedPackagesDir = layout.buildDirectory.dir("generated/analysed-packages")

val writeAnalysedPackages =
    tasks.register<WriteAnalysedPackages>("writeAnalysedPackages") {
        group = "verification"
        description = "Records the packages every architecture rule must have been able to see."
        analysedProjects.forEach { analysed ->
            sources.from(analysed.fileTree("src/main/kotlin") { include("**/*.kt") })
        }
        manifest = analysedPackagesDir.map { it.file("analysed-packages.txt") }
    }

/**
 * The same trick for the unit-test side, asserted by
 * `TestPurityRulesTest.everyUnitTestPackageIsAnalysed`.
 */
val writeUnitTestPackages =
    tasks.register<WriteAnalysedPackages>("writeUnitTestPackages") {
        group = "verification"
        description = "Records the packages every test-purity rule must have been able to see."
        analysedProjects.forEach { analysed ->
            sources.from(analysed.fileTree("src/test/kotlin") { include("**/*.kt") })
        }
        manifest = analysedPackagesDir.map { it.file("unit-test-packages.txt") }
    }

sourceSets.named("test") {
    resources.srcDir(analysedPackagesDir)
}

tasks.named<ProcessResources>("processTestResources") {
    dependsOn(writeAnalysedPackages, writeUnitTestPackages)
}
