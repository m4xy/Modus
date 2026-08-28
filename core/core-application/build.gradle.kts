// Use cases. Orchestrates the domain and nothing else.
// May depend on core-domain ONLY — never on an adapter or an installable module.
plugins {
    id("modus.kotlin-base")
}

dependencies {
    api(project(":core-domain"))
}
