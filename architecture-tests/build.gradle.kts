// The architectural gate. This module has no production code: it puts every
// other module on the test classpath and asserts the dependency rules that the
// module graph is supposed to guarantee.
//
// If a rule here starts failing, the fix is to change the code, not the rule.
plugins {
    id("modus.kotlin-base")
}

dependencies {
    testImplementation(project(":core-domain"))
    testImplementation(project(":core-application"))
    testImplementation(project(":adapter-persistence-flatfile"))
    testImplementation(project(":adapter-rest"))
    testImplementation(project(":adapter-agent-claude"))
    testImplementation(project(":adapter-vcs-git"))
    testImplementation(project(":module-beans"))
    testImplementation(project(":module-cost"))
    testImplementation(project(":modus-server"))

    testImplementation(platform(libs.springBoot.bom))
    testImplementation(libs.archunit.junit5)
}
