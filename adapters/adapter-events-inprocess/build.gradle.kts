// Synchronous in-process fan-out of drained domain events: the walking skeleton's
// implementation of core-application's DomainEventDispatchPort.
// Adapters may depend on core-application and core-domain. They must never
// depend on another adapter, on an installable module, or on the app.
//
// modus.kotlin-base, not modus.spring-module: this adapter needs no Spring. The
// technology it adapts to is the calling thread, and applying a framework it does
// not use would put Spring on a classpath for the sake of matching its siblings.
plugins {
    id("modus.kotlin-base")
}

dependencies {
    implementation(project(":core-application"))
}
