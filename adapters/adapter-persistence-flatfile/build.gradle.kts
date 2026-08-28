// Durable flat-file store. Flat files are the preferred storage for Modus; a database is not assumed.
// Adapters may depend on core-application and core-domain. They must never
// depend on another adapter, on an installable module, or on the app.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
}
