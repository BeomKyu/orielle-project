plugins {
    kotlin("jvm")
}

group = "com.orielle"
version = "unspecified"

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}