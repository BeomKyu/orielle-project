// orielle-project/build.gradle.kts

// 모든 하위 프로젝트(모듈)에 적용될 플러그인과 설정을 정의합니다.
subprojects {
    // 모든 모듈에 공통으로 적용할 플러그인들
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // 모든 모듈이 사용할 저장소 (dependencies를 다운로드할 곳)
    repositories {
        mavenCentral()
    }

    // 모든 모듈의 코틀린 컴파일러 옵션 등 공통 설정
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
}