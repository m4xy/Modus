// Work tracking: markdown work items following the hmans/beans schema.
// Installable per-domain module. Modules may depend on core-application and
// core-domain. A module must NEVER depend on another module.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
}
