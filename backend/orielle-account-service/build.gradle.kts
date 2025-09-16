group = "com.orielle"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinSpring)
}

repositories { mavenCentral() }

dependencies {
    implementation(libs.bundles.springBootWebfluxEcosystemInKotiln)
    implementation(libs.bundles.springBootR2dbcEcosystemInKotlin)
    testImplementation(libs.springBootStarterTest)
    testImplementation(kotlin("test"))
}
