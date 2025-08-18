# Orielle Project

이 프로젝트는 Spring Boot와 Kotlin WebFlux를 사용하여 구축된 리액티브 백엔드 애플리케이션입니다.

## ✨ 주요 특징

- **리액티브 스택**: Spring WebFlux와 코틀린 코루틴을 사용하여 비동기 및 논블로킹 방식으로 높은 성능과 확장성을 추구합니다.
- **모던 코틀린 활용**: 코틀린의 최신 기능을 활용하여 간결하고 안전한 코드를 지향합니다.
- **중앙화된 의존성 관리**: Gradle Version Catalog (`libs.versions.toml`)를 통해 프로젝트의 모든 의존성을 체계적으로 관리합니다.
- **다중 모듈 구조**: `orielle-idessy-service`와 같은 하위 모듈을 통해 기능을 분리하고 확장 가능한 구조를 가집니다.

## 🛠️ 기술 스택

- **Framework**: Spring Boot 3.5.4 (WebFlux)
- **Language**: Kotlin 2.2.10
- **Build Tool**: Gradle
- **Asynchronous**: Kotlin Coroutines
- **JSON**: Jackson, Kotlinx Serialization

## 🚀 시작하기

1.  **저장소 복제**

    ```bash
    git clone <repository-url>
    ```

2.  **프로젝트 빌드**

    ```bash
    ./gradlew build
    ```

3.  **애플리케이션 실행**

    ```bash
    ./gradlew :backend:orielle-idessy-service:bootRun
    ```
