// Convention plugins. Every Modus module applies one of these and configures
// nothing else: the toolchain, compiler flags, ktlint, Detekt and test wiring
// live here so they cannot drift between modules.
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.kotlin.allopen)
    implementation(libs.plugin.springBoot)
    implementation(libs.plugin.ktlint)
}
