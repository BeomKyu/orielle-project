# API Gateway Design Document

- **Version**: 1.1
- **Author**: BeomKyu, Orielle 프로젝트 컨설턴트
- **Status**: FINALIZED

## 1. 서비스 개요
[cite_start]API Gateway는 Orielle 생태계로 들어오고 나가는 **모든 내부/외부 통신을 중계하는 유일한 관문**이자, 내부 서비스 간의 통신을 중재하는 '교통 관제 센터'입니다[cite: 38, 73]. [cite_start]플랫폼 전체의 보안, 안정성, 확장성, 그리고 미래의 수익 모델까지 책임지는 Tier 0의 핵심 인프라입니다[cite: 74].

## 2. 핵심 기능 및 책임
- [x] [cite_start]**단일 진입점 (Single Entry Point)**: 외부 세계에 `api.orielle.io`라는 단 하나의 주소만을 노출하고, 요청 경로에 따라 올바른 마이크로서비스로 정확하게 라우팅합니다[cite: 75].
- [x] [cite_start]**인증 및 인가 (Authentication & Authorization)**: 플랫폼의 첫 번째 보안 검색대로서, 들어오는 모든 요청의 JWT('익명 토큰' 또는 '통합 토큰')를 가장 먼저 검사하여 유효성을 검증합니다.
- [x] **횡단 관심사 처리 (Cross-Cutting Concerns)**: 모든 마이크로서비스에 공통적으로 적용되어야 할 로깅, 모니터링, 요청 제한 등의 기능을 중앙에서 일괄 처리합니다.
- [x] [cite_start]**서비스 간 통신 중재 (Inter-Service Communication)**: 내부 서비스들이 서로를 직접 호출하는 것이 아니라, 반드시 API Gateway를 통해 통신하도록 하여, 통신 흐름을 표준화하고 중앙에서 통제합니다[cite: 77].
- [x] [cite_start]**데이터 수집 허브 (Data Aggregation Hub)**: 모든 트래픽이 거쳐 가는 유일한 통로로서, '사용량 기반 과금', '개발자 분석 대시보드' 등 미래 비즈니스 모델의 기반이 될 '데이터 금광' 역할을 수행합니다.

## 3. 기술 스택 및 구현
- **주요 기술**: **Spring Cloud Gateway (Kotlin 기반)**
  - **선정 이유**: 기존 Orielle 서비스들이 Spring Boot / Kotlin 스택으로 개발되므로 기술 스택의 일관성을 유지할 수 있습니다. 또한, Netty 기반의 비동기 논블로킹 방식으로 동작하여 높은 성능을 보장하며, 필터(Filter)를 통해 복잡한 로직을 유연하게 구현할 수 있습니다.
- **통합 방식**:
  - **서비스 디스커버리 (Service Discovery)**: Kubernetes의 서비스 디스커버리 메커니즘과 연동합니다. 라우팅 규칙에 `lb://idessy-service`와 같이 서비스 이름을 사용하면, Gateway가 자동으로 해당 서비스의 Pod IP 목록을 찾아 요청을 전달합니다.
  - **Keycloak**: 토큰 검증을 위해 Keycloak의 공개키 목록 주소(JWKS URI)를 설정에 등록하고, 캐싱하여 사용합니다.

## 4. 라우팅 규칙 (Routing Rules)
라우팅 규칙은 `application.yml` 파일에 선언적으로 정의합니다.

- **`application.yml` 예시**:
  ```yaml
  spring:
    cloud:
      gateway:
        routes:
          # IdessyService 라우팅
          - id: idessy_service_route
            uri: lb://idessy-service # Kubernetes 서비스 이름
            predicates:
              - Path=/api/idessy/**
            filters:
              - StripPrefix=2 # /api/idessy 제거 후 전달

          # FriendService 라우팅
          - id: friend_service_route
            uri: lb://friend-service
            predicates:
              - Path=/api/friends/**
            filters:
              - StripPrefix=2 # /api/friends 제거 후 전달

          # 내부용 API 라우팅 (외부 노출 방지)
          - id: internal_idessy_api
            uri: lb://idessy-service
            predicates:
              - Path=/internal/**
              # 추가로, 특정 IP 대역에서만 접근을 허용하는 Predicate 필요
              - RemoteAddr=10.0.0.0/8
  ```

## 5. 글로벌 필터 (Global Filters) - 핵심 로직
API Gateway는 모든 라우팅 규칙에 공통적으로 적용되는 '글로벌 필터'를 통해 횡단 관심사를 처리합니다. 이 필터들은 정해진 순서대로 동작합니다.

### 1. `Request ID Injection Filter` (순서: 1)
- **역할**: 요청 헤더에 `X-Orielle-Request-Id`가 없으면 새로 생성하여 추가합니다. 이 ID는 모든 하위 서비스로 전파되어 분산 추적(Distributed Tracing) 및 로그 분석에 사용됩니다.

### 2. `CORS Handling Filter` (순서: 2)
- **역할**: 웹 브라우저의 Cross-Origin Resource Sharing 정책을 처리합니다.

### 3. `Rate Limiting Filter` (순서: 3)
- **역할**: DDoS 공격 방어 및 서비스 안정성 확보를 위해 요청량을 제어합니다.
- **구현 전략**: Redis를 활용하여 구현하며, 인증된 사용자의 경우 토큰의 `sub` (익명ID 또는 IdessyID)를 기준으로, 비인증 사용자의 경우 IP 주소를 기준으로 요청 횟수를 제한합니다.

### 4. `Authentication Filter` (순서: 10)
- **역할**: 플랫폼의 **'1차 보안 검색대'** 역할을 수행합니다.
- **로직**:
  1.  요청 헤더에서 `Authorization` 토큰을 추출합니다.
  2.  Keycloak의 JWKS 엔드포인트에서 가져온 공개키로 토큰의 서명을 검증합니다.
  3.  토큰의 만료 시간, 발급자(Issuer) 등을 검증합니다.
  4.  검증에 실패하면 `401 Unauthorized` 에러를 즉시 반환합니다.
  5.  검증에 성공하면, 토큰의 주요 클레임(예: `sub`, `scope`)을 파싱하여 요청 헤더(예: `X-Authenticated-User-Id`, `X-User-Roles`)에 추가한 뒤 다음 필터로 전달합니다. 이를 통해 하위 서비스들은 토큰을 다시 검증할 필요 없이 헤더 정보만 신뢰하고 사용할 수 있습니다.

### 5. `Logging Filter` (순서: 마지막)
- **역할**: 모든 요청과 그에 대한 최종 응답(HTTP 상태 코드, 처리 시간 등)을 일관된 JSON 형식으로 로깅합니다. 이 로그는 Orielle의 **'데이터 금광'**의 원천이 됩니다.

## 6. 에러 처리 전략
API Gateway는 하위 서비스에서 발생한 에러가 아닌, **Gateway 자체에서 발생한 에러**에 대해 표준화된 `ApiResponse` 형식으로 응답해야 합니다.

-   **401 Unauthorized**: 인증 필터에서 토큰 검증 실패 시
-   **403 Forbidden**: 토큰은 유효하나, 해당 API에 접근할 권한(Scope)이 없을 경우
-   **404 Not Found**: 라우팅 규칙에 맞는 하위 서비스를 찾지 못했을 경우
-   **429 Too Many Requests**: Rate Limiting에 의해 요청이 차단되었을 경우
-   **503 Service Unavailable**: 라우팅 대상 하위 서비스가 응답하지 않을 경우 (Circuit Breaker 연동)