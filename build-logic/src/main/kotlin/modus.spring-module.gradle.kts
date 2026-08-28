// Spring on top of modus.kotlin-base, for adapters and installable modules.
//
// This deliberately does NOT apply the Spring Boot Gradle plugin: only
// app/modus-server produces an executable jar. Everything else is a plain
// library that happens to have Spring on its compile classpath, versions
// supplied by the Boot BOM.
plugins {
    id("modus.kotlin-base")
    id("org.jetbrains.kotlin.plugin.spring")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    val bom = platform(libs.findLibrary("springBoot-bom").get())
    "implementation"(bom)
    "integrationTestImplementation"(bom)

    "implementation"(libs.findLibrary("spring-boot-starter").get())

    // Spring's test support is available to integration tests and to nothing
    // else. It is NOT on `testImplementation`: a unit test that imports
    // @SpringBootTest, MockMvc or @MockitoBean has no such symbol to import.
    "integrationTestImplementation"(libs.findLibrary("spring-boot-starter-test").get())
}

// `testImplementation` extends `implementation`, so Spring would still arrive on
// the unit-test classpath through the module's own production dependencies. Cut
// it there. This is the whole mechanism: misclassification is a compile error at
// the import statement, not a review comment on the pull request. The two groups
// are every Spring group Modus resolves today;
// `assertUnitTestClasspathIsSpringFree` (modus.kotlin-base) fails the build if a
// third one appears, so this list cannot silently rot.
val springGroups = listOf("org.springframework", "org.springframework.boot")

listOf("testCompileClasspath", "testRuntimeClasspath").forEach { classpath ->
    configurations.named(classpath) {
        springGroups.forEach { exclude(group = it) }
    }
}
