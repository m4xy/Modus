// LLM spend tracking and per-domain budgets.
// Installable per-domain module. Modules may depend on core-application and
// core-domain. A module must NEVER depend on another module.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
}
