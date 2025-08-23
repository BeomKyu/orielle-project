// orielle-project/build.gradle.kts

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSpring) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.springDependencyManagement) apply false
}

// 모든 하위 프로젝트(모듈)에 공통으로 적용할 최소 설정만 남깁니다.
subprojects {
    // 모든 모듈에 Java 플러그인을 적용하여 Toolchain을 중앙에서 지정
    apply(plugin = "java")

    // Kotlin DSL에서는 다른 프로젝트를 구성할 때 java { } 대신 JavaPluginExtension으로 구성해야 합니다.
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(
                providers.gradleProperty("javaToolchain")
                    .map { JavaLanguageVersion.of(it.toInt()) }
            )
        }
    }

    repositories {
        mavenCentral()
    }

    // 공통 테스트 설정
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
