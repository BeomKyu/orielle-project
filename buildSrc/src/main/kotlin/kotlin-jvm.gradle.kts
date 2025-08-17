// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 코틀린 JVM 플러그인을 적용합니다.
    kotlin("jvm")
}

// 모든 코틀린 JVM 모듈이 공통으로 사용할 저장소를 설정합니다.
repositories {
    mavenCentral()
}

kotlin {
    // Java 21 버전을 사용하도록 툴체인을 설정합니다.
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test>().configureEach {
    // 모든 테스트 작업에 JUnit Platform을 사용하도록 설정합니다.
    useJUnitPlatform()

    // 실패한 테스트뿐만 아니라 모든 테스트 결과를 로그에 출력합니다.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
