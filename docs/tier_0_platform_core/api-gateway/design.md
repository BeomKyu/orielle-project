# API Gateway Design Document

- **Version**: 1.0
- **Author**: BeomKyu
- **Status**: DRAFT

## 1. Service Overview
Orielle 생태계로 들어오고 나가는 **모든 내부/외부 통신을 중계하는 유일한 관문**이자, 내부 서비스 간의 통신을 중재하는 '교통 관제 센터'입니다. 플랫폼 전체의 보안, 안정성, 확장성, 그리고 미래의 수익 모델까지 책임지는 핵심 인프라입니다.

## 2. Core Features
- [ ] **Single Entry Point**: 외부 세계에 `api.orielle.io`라는 단 하나의 주소만을 노출하고, 요청을 올바른 목적지 서비스로 라우팅합니다.
- [ ] **Authentication & Authorization**: 들어오는 모든 요청의 JWT('익명 토큰' 또는 '통합 토큰')를 가장 먼저 검사하여 유효성을 검증하고, 토큰에 부여된 권한(Scope)에 따라 API 접근을 제어합니다.
- [ ] **Rate Limiting**: DDoS 공격 및 특정 사용자의 과도한 요청으로부터 시스템을 보호하기 위한 요청 제한 기능을 제공합니다.
- [ ] **Centralized Logging & Monitoring**: 모든 API 트래픽을 중앙에서 로깅하고 모니터링하여 '사용량 기반 과금', '개발자를 위한 분석 대시보드' 등 비즈니스 모델의 기반 데이터를 수집합니다.
- [ ] **Inter-Service Communication**: 내부 서비스 간의 통신(Service-to-Service)을 중재하고 표준화합니다.

## 3. Technical Implementation
- **Technology**: Spring Cloud Gateway (Kotlin 기반) 또는 Kong/Ambassador 등 검토
- **Integration**:
    - **Keycloak**: JWT 토큰의 유효성을 검증하기 위해 JWKS URI를 사용합니다.
    - **Service Discovery**: Kubernetes의 서비스 디스커버리 메커니즘과 연동하여 동적으로 라우팅 대상을 찾습니다.

## 4. API Endpoints
API Gateway 자체는 특정 엔드포인트를 갖기보다는, 들어오는 요청 경로에 따라 동적으로 하위 서비스로 라우팅하는 역할을 합니다.
- **Example Routing Rule:**
  - `api.orielle.io/idessy/**` → `idessy-service`
  - `api.orielle.io/friends/**` → `friend-service`
