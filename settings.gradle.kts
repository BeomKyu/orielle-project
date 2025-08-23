rootProject.name = "orielle-project"

// 공통(frontend, backend) 모듈
include("utils")

// 백엔드 서브 모듈 빌드 스크립트
include("backend:orielle-idessy-service")
include("backend:orielle-friend-service")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
    // Version Catalog는 기본 위치 gradle/libs.versions.toml을 자동 사용
}