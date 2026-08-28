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
    "testImplementation"(bom)

    "implementation"(libs.findLibrary("spring-boot-starter").get())
    "testImplementation"(libs.findLibrary("spring-boot-starter-test").get())
}
