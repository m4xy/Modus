// The single deployable. Wiring ONLY: this module may depend on everything,
// and nothing may depend on it. Business logic here is a bug.
plugins {
    id("modus.spring-app")
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-application"))

    runtimeOnly(project(":adapter-persistence-flatfile"))
    implementation(project(":adapter-rest"))
    runtimeOnly(project(":adapter-agent-claude"))
    runtimeOnly(project(":adapter-vcs-git"))

    runtimeOnly(project(":module-beans"))
    runtimeOnly(project(":module-cost"))

    implementation(libs.spring.boot.starter.web)
}
