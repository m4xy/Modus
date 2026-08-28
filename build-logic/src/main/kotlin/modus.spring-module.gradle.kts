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
// the unit-test classpath through the module's own production dependencies. It
// is cut in `modus.kotlin-base`, not here: `:architecture-tests` is not a Spring
// module and still inherits the whole Spring runtime graph through its project
// dependencies, so the cut has to be unconditional to be worth anything.
//
// That cut is what makes misclassification a compile error at the import
// statement rather than a review comment on the pull request, and
// `assertUnitTestClasspathIsSpringFree` (also `modus.kotlin-base`) states
// positively what a unit-test classpath may contain, so a Spring-adjacent group
// nobody thought to exclude fails the build instead of arriving in silence.
