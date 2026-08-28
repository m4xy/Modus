// Supervises Claude Code processes and streams their output back to callers.
// Adapters may depend on core-application and core-domain. They must never
// depend on another adapter, on an installable module, or on the app.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
}
