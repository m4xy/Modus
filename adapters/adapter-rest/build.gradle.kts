// The REST surface rooted at /domains/{domainId}, plus SSE/WebSocket streaming.
// Adapters may depend on core-application and core-domain. They must never
// depend on another adapter, on an installable module, or on the app.
plugins {
    id("modus.spring-module")
}

dependencies {
    implementation(project(":core-application"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.springdoc.openapi.webmvc.ui)
}
