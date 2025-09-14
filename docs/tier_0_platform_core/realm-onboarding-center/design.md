# Realm Onboarding Center Design Document

- **Version**: 1.1 (Final)
- **Author**: BeomKyu, Orielle 프로젝트 컨설턴트
- **Status**: FINALIZED

## 1. 서비스 개요
Realm Onboarding Center는 외부 개발자들이 Orielle 생태계에 자신의 서비스(Realm)를 입점시키기 위해 방문하는 공식적인 **'건축 허가 사무소'이자 '개발자 포털'**입니다. Keycloak의 복잡한 기술적 설정들을 사용자 친화적인 UI 뒤로 감추고, 개발자들에게 쉽고 편리한 온보딩 경험을 제공하여 Orielle 생태계의 성장을 촉진하는 것을 목표로 합니다.

## 2. 핵심 기능 및 책임
- [x] **셀프 서비스 Realm 등록**: 개발자가 포털 UI를 통해 자신의 서비스 이름, 설명, 리디렉션 URI 등 필요한 정보를 직접 입력하고 등록 신청을 할 수 있습니다.
- [x] **Keycloak 클라이언트 자동 생성**: 등록 신청이 완료되면, 백엔드는 Keycloak의 Admin API를 호출하여 해당 Realm을 위한 Keycloak 'Client'를 자동으로 생성하고 `Client ID`와 `Client Secret`을 발급합니다.
- [x] **보안 인증정보 관리**: 발급된 `Client Secret`을 안전하게 조회(최초 1회만 노출)하고, 유출 시 재발급할 수 있는 기능을 제공합니다.
- [x] **점진적 권한(Scope) 관리**: 개발자는 처음 등록 시 로그인에 필요한 최소 권한(`openid`, `profile`)만 할당받습니다. 이후 친구 목록 조회(`friends.read`) 등 추가 기능이 필요하면, 이 포털을 통해 필요한 권한을 '요청'하고 Orielle 플랫폼 관리자의 '승인'을 받는 절차를 거칩니다.
- [x] **사용량 대시보드**: 자신의 Realm(Client)이 Orielle의 API Gateway를 통해 API를 얼마나 호출하고 있는지 기본적인 통계(호출 수, 에러율 등)를 보여주는 대시보드를 제공합니다.

## 3. Data Model (ERD)
`RealmOnboardingCenter`는 2개의 핵심 테이블로 구성됩니다.

- **Table: `realms`**
  - `id` (UUID): Primary Key
  - `owner_user_id` (String): 이 Realm을 소유한 개발자의 Keycloak User ID
  - `name` (String): Realm 이름 (Unique)
  - `description` (String, nullable)
  - `website_url` (String, nullable): 서비스 웹사이트 주소
  - `redirect_uris` (JSONB/Array<String>): 로그인 후 리디렉션될 URI 목록
  - `keycloak_client_id` (String): Keycloak에 생성된 Client의 고유 ID (Unique)
  - `status` (Enum: `REVIEW`, `ACTIVE`, `REJECTED`, `SUSPENDED`): Realm의 현재 상태
  - `created_at` (LocalDateTime)
  - `updated_at` (LocalDateTime)

- **Table: `realm_scope_grants`** (Realm에 부여된 권한 목록)
  - `realm_id` (UUID): FK to `realms.id`
  - `scope_name` (String): 부여된 권한의 이름 (예: `friends.read`, `chat.write`)
  - `status` (Enum: `REQUESTED`, `GRANTED`, `REJECTED`): 권한 요청 상태
  - `requested_at` (LocalDateTime)
  - `granted_at` (LocalDateTime, nullable)

## 4. API Endpoints

### 1. 개발자 포털 API (Realm 관리)
#### `POST /api/realms`
- **설명**: 새로운 Realm 등록을 신청합니다.
- **인증/권한**: 로그인한 모든 개발자
- **Request Body**: `{"name": "...", "description": "...", "redirectUris": ["..."]}`
- **Response**: `ApiResponse<RealmDto>` (초기 `REVIEW` 상태)

#### `GET /api/realms/my`
- **설명**: 내가 소유한 모든 Realm 목록을 조회합니다.
- **인증/권한**: 로그인한 개발자
- **Response**: `ApiResponse<List<RealmDto>>`

#### `GET /api/realms/{realmId}`
- **설명**: 특정 Realm의 상세 정보를 조회합니다.
- **인증/권한**: 해당 Realm의 소유자
- **Response**: `ApiResponse<RealmDto>`

#### `PUT /api/realms/{realmId}`
- **설명**: Realm의 기본 정보(설명, 리디렉션 URI 등)를 수정합니다.
- **인증/권한**: 해당 Realm의 소유자
- **Request Body**: `{"description": "...", "redirectUris": ["..."]}`
- **Response**: `ApiResponse<RealmDto>`

### 2. 보안 인증정보 API
#### `GET /api/realms/{realmId}/client-secret`
- **설명**: Realm의 `Client Secret`을 조회합니다. 보안을 위해 **최초 생성 시에만** 값을 보여주고, 이후에는 조회할 수 없도록 처리해야 합니다.
- **인증/권한**: 해당 Realm의 소유자
- **Response**: `ApiResponse<{ "clientSecret": "..." }>`

#### `POST /api/realms/{realmId}/client-secret/reissue`
- **설명**: `Client Secret`을 재발급합니다. 이전 Secret은 즉시 만료됩니다.
- **인증/권한**: 해당 Realm의 소유자
- **Response**: `ApiResponse<{ "clientSecret": "..." }>` (새로 발급된 Secret)

### 3. 권한(Scope) 관리 API
#### `GET /api/realms/{realmId}/scopes`
- **설명**: 해당 Realm이 현재 요청했거나 부여받은 모든 권한(Scope) 목록과 상태를 조회합니다.
- **인증/권한**: 해당 Realm의 소유자
- **Response**: `ApiResponse<List<RealmScopeGrantDto>>`

#### `POST /api/realms/{realmId}/scopes`
- **설명**: 새로운 권한을 요청합니다.
- **인증/권한**: 해당 Realm의 소유자
- **Request Body**: `{"scopeName": "friends.read"}`
- **Response**: `ApiResponse<RealmScopeGrantDto>` (`REQUESTED` 상태)

## 5. Core Logic Flows
- **새로운 Realm 등록 시나리오**:
  1. 개발자가 포털 UI에서 `POST /api/realms` API를 호출합니다.
  2. Onboarding Center는 `realms` 테이블에 해당 정보를 `REVIEW` 상태로 저장합니다.
  3. 백그라운드에서 Keycloak Admin API를 호출하여 새로운 `Client`를 생성합니다. 이때 기본 Scope(`openid`, `profile`)만 할당합니다.
  4. 생성된 `keycloak_client_id`와 `Client Secret`을 `realms` 테이블에 업데이트하고, Secret은 암호화하여 저장합니다.
  5. Orielle 플랫폼 관리자에게 승인 요청 알림을 보냅니다. (MVP+ 기능)
  6. 관리자가 승인하면, `realms` 테이블의 `status`를 `ACTIVE`로 변경합니다.

## 6. Dependencies
- **Keycloak**: Admin API를 사용하여 Client를 생성하고 관리합니다.
- **PostgreSQL**: `realms`, `realm_scope_grants` 테이블 정보를 저장합니다.
- **API Gateway**: (향후 연동) Realm별 API 사용량 통계를 수집하기 위해 Gateway의 로그 데이터를 참조합니다.