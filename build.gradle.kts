// orielle-project/build.gradle.kts

// 모든 하위 프로젝트(모듈)에 적용될 플러그인과 설정을 정의합니다.
subprojects {
    // 공통 플러그인 적용
    apply(plugin = "buildsrc.convention.kotlin-jvm")

    // 공통 저장소 설정
    repositories {
        mavenCentral()
    }
}
