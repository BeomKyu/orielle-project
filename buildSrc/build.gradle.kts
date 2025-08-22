plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    // 단순화: 카탈로그 파싱 제거, 고정된 JDK 21 사용
    jvmToolchain(21)
}

dependencies {
    // 프리컴파일드 스크립트 플러그인에서 Kotlin Gradle Plugin 사용
    implementation(libs.kotlinGradlePlugin)
}