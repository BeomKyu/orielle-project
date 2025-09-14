# Utils (Common) Module Design Document

- **Version**: 1.2 (Final)
- **Author**: BeomKyu, Orielle 프로젝트 컨설턴트
- **Status**: FINALIZED

## 1. 모듈 개요
Utils Module(`orielle-project/utils`)은 Orielle 플랫폼의 모든 마이크로서비스가 공유하는 **'공통 건축 표준 설계도'** 역할을 수행합니다. 코드 중복을 제거하고, 개발 경험(DX)을 향상시키며, 플랫폼 전체의 기술적 일관성을 유지하는 것을 목표로 합니다.

이 모듈은 Orielle의 보안 모델에 따라 **내부 서비스용(`internal`)**과 **외부 개발자용(`sdk`)**으로 명확히 구분하여 개발 및 배포되어야 합니다.

## 2. 모듈 구조
- **`orielle-utils-internal` (내부 서비스 전용)**: Orielle의 모든 내부 서비스(IdessyService, FriendService 등)가 사용하는 '완전판' 라이브러리입니다. 외부로 노출되어서는 안 되는 민감한 로직과 내부 서비스 간의 상호작용에 필요한 모든 유틸리티를 포함합니다.
- **`orielle-utils-sdk` (외부 Realm 개발자용)**: 외부 개발자에게 배포될 '공개용 경량 버전' SDK입니다. 외부 Realm이 Orielle 플랫폼과 안전하게 상호작용하는 데 필요한 최소한의 기능만을 제공하며, 민감한 내부 로직은 철저히 배제됩니다.

## 3. 핵심 구성 요소

### 3.1. 도메인 공통 요소 (Domain Commons)
- **`Enums`**: 플랫폼 전반에서 사용되는 타입이나 역할을 정의하여 타입 안전성을 확보합니다.
  - 예: `IdessyType`, `GroupRole`
- **`Constants`**: 공통으로 사용되는 상수 값들을 관리합니다.
  - 예: `TOKEN_ISSUER = "orielle.com"`, `HEADER_ORIELLE_REQUEST_ID = "X-Orielle-Request-Id"`
- **`DTOs (Data Transfer Objects)`**: 표준 API 응답 형식(`ApiResponse`, `ApiError`)과 같이 여러 서비스에서 공통으로 사용되는 데이터 객체를 정의합니다.

### 3.2. 애플리케이션 유틸리티 (Application Utilities)
- **`Exception Handling`**: 공통 예외 클래스(`OrielleException`, `ResourceNotFoundException` 등)를 정의하고, 이를 처리하는 전역 예외 핸들러(Global Exception Handler)의 기본 구현을 제공합니다.
- **`Date & Time Utilities`**: 날짜/시간 관련 처리를 돕는 확장 함수나 헬퍼 클래스를 제공합니다. (예: `String.toLocalDateTime()`)
- **`Validation`**: 반복적으로 사용되는 검증 로직(예: 이메일 형식, 비밀번호 정책)을 헬퍼 함수나 커스텀 어노테이션으로 제공합니다.

### 3.3. 보안 관련 (Security)
- **`utils-sdk` (외부용)**:
  - **기능**: 외부 Realm이 받은 **'익명 토큰'**의 서명을 검증하는 기능만 제공합니다.
  - **의존성**: `java-jwt` 등 가벼운 JWT 라이브러리. **Spring Security 미포함.**
- **`utils-internal` (내부용)**:
  - **기능**: Spring Security와 통합되어, Controller에서 `@AuthenticationPrincipal` 등으로 토큰 정보를 쉽게 주입받을 수 있도록 설정합니다. 또한 `sub` (idessy_id), `own` (user_id) 등 Orielle의 커스텀 클레임을 파싱하는 유틸리티를 제공합니다.
  - **의존성**: **Spring Security 필수 포함.**

### 3.4. 인프라 유틸리티 (Infrastructure Utilities)
- **`Logging`**: 모든 마이크로서비스가 일관된 JSON 포맷으로 로그를 남기도록 `Logback` 설정을 제공하거나, `MDC(Mapped Diagnostic Context)`에 요청 ID 등을 자동으로 추가하는 필터 구현을 제공합니다.
- **`Serialization`**: `kotlinx.serialization` 또는 `Jackson`의 `ObjectMapper`에 대한 공통 설정을 제공하여 모든 서비스가 동일한 직렬화/역직렬화 규칙(예: 날짜 포맷)을 따르도록 합니다.

## 4. 의존성 관리
- 이 Utils 모듈은 다른 Orielle 서비스들에 의해 의존되지만, 스스로는 외부 라이브러리(Kotlin, Spring 등) 외에 다른 Orielle 서비스에 대한 의존성을 가져서는 안 됩니다. (순환 참조 방지)
- `utils-sdk`는 외부 개발자의 편의를 위해 최소한의 의존성만을 가져야 합니다.