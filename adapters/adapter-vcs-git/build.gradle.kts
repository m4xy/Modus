// Git-backed repository operations: clone, branch, commit, push.
// Adapters may depend on core-application and core-domain. They must never
// depend on another adapter, on an installable module, or on the app.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
}
