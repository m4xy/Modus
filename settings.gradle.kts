pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "modus"

// Project names are flat and unique; directories carry the layer grouping.
// This list is the one home for the module set: no document restates it
// (doc:05-authoring-for-agents#one-fact-one-place). What a module may depend on is
// doc:10-architecture#module-dependencies, enforced by the ArchUnit rules in
// architecture-tests — not by keeping a second copy of this list in sync.
fun module(
    name: String,
    path: String,
) {
    include(name)
    project(":$name").projectDir = file(path)
}

module("core-domain", "core/core-domain")
module("core-application", "core/core-application")

module("adapter-persistence-flatfile", "adapters/adapter-persistence-flatfile")
module("adapter-rest", "adapters/adapter-rest")
module("adapter-agent-claude", "adapters/adapter-agent-claude")
module("adapter-vcs-git", "adapters/adapter-vcs-git")

module("module-beans", "modules/module-beans")
module("module-cost", "modules/module-cost")

module("modus-server", "app/modus-server")

module("architecture-tests", "architecture-tests")
