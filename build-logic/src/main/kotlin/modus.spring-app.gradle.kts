// The single deployable: layers the Spring Boot Gradle plugin (bootJar,
// bootRun, BOM-driven dependency management) on top of modus.spring-module.
//
// Only app/modus-server may apply this.
plugins {
    id("modus.spring-module")
    id("org.springframework.boot")
}
