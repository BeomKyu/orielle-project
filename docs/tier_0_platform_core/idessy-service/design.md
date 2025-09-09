# IdessyService Design Document

- **Version**: 1.2
- **Author**: BeomKyu, Orielle 프로젝트 컨설턴트
- **Status**: FINALIZED

## 1. 서비스 개요
Orielle 플랫폼의 핵심 개념인 'Idessy'(다중 페르소나)를 관리하는 마이크로서비스입니다. 플랫폼의 **'주민센터'이자 '비밀 금고'** 역할을 수행하며, Keycloak이 관리하는 중앙 'User'에 종속된 여러 'Idessy' 프로필의 생성, 조회, 상태 변경을 책임집니다.

특히 이 서비스는 `'PERSONAL'`과 `'GROUP'` 타입의 Idessy를 모두 관리하며, 외부 서비스(Realm)에 제공될 '익명 ID'를 생성하고, 그룹 Idessy와 법인격 User 간의 관계를 설정하여 플랫폼의 독자적인 신원 모델을 완성합니다.

## 2. 핵심 기능
- [x] **개인/그룹 Idessy 생성 및 관리**: `PERSONAL` 타입과 `GROUP` 타입의 Idessy를 모두 지원합니다.
- [x] **그룹 멤버 관리**: `GROUP` Idessy에 다른 `PERSONAL` Idessy를 멤버로 추가하거나 제외할 수 있습니다.
- [x] **법적 대표자 지정**: `GROUP` Idessy 생성 시 자동으로 생성되는 `'CORPORATE' User`에 대해 1명 이상의 `'PERSONAL' User`를 법적 책임자로 지정합니다.
- [x] **Keycloak User 연동**: 모든 Idessy는 특정 Keycloak User ID에 연결됩니다.
- [x] **익명 ID 관리**: 사용자가 특정 Realm에서 특정 Idessy를 사용할 때, 고유한 익명 ID를 동적으로 생성하고 관리하는 '비밀 장부' 역할을 수행합니다.
- [x] **익명 이메일 관리**: Idessy 생성 시 `@orielle.com` 익명 이메일을 선택적으로 생성할 수 있습니다.

## 3. Data Model (ERD)
`IdessyService`는 총 4개의 핵심 테이블로 구성됩니다.

- **Table: `idessys`** (Idessy 원장)
  - `id` (UUID): Primary Key
  - `keycloak_user_id` (String): 이 Idessy를 소유한 User의 Keycloak ID
  - `type` (Enum: `PERSONAL`, `GROUP`): Idessy 타입
  - `name` (String): Idessy 이름
  - `description` (String, nullable)
  - `anonymous_email` (String, nullable): Idessy 전용 익명 이메일 주소, Unique
  - `is_active` (Boolean): 활성화 여부
  - `created_at` (LocalDateTime)
  - `updated_at` (LocalDateTime)

- **Table: `anonymous_links`** (익명 ID 연결 테이블 - "비밀 장부")
  - `anonymous_id` (UUID): Primary Key. 이 ID가 외부 Realm에 `sub` 클레임으로 전달됨.
  - `keycloak_user_id` (String): 실제 User의 Keycloak ID (Index)
  - `idessy_id` (UUID): 사용된 Idessy의 ID (FK to `idessys.id`, Index)
  - `realm_client_id` (String): 외부 서비스(Realm)의 Keycloak Client ID (Index)
  - `created_at` (LocalDateTime)
  - `last_used_at` (LocalDateTime)
  - *Unique Constraint: (`keycloak_user_id`, `idessy_id`, `realm_client_id`)*

- **Table: `group_memberships`** (그룹 멤버 관계)
  - `group_idessy_id` (UUID): `'GROUP'` 타입 Idessy의 ID (FK to `idessys.id`)
  - `member_idessy_id` (UUID): 그룹에 속한 `'PERSONAL'` 타입 Idessy의 ID (FK to `idessys.id`)
  - `role` (Enum: `ADMIN`, `MEMBER`): 그룹 내 역할
  - `joined_at` (LocalDateTime)

- **Table: `corporate_representatives`** (법인격 User의 법적 대표자)
  - `corporate_user_id` (String): `'CORPORATE'` 타입 User의 Keycloak ID
  - `representative_user_id` (String): 법적 대표자인 `'PERSONAL'` 타입 User의 Keycloak ID
  - `assigned_at` (LocalDateTime)

## 4. API Endpoints
- **Idessy 관리**:
    - `GET /api/idessy/all`: (내부 토큰 사용) 내 모든 Idessy 조회
    - `POST /api/idessy`: 새 Idessy 생성 (Request Body에 type, name 등 포함)
    - `PUT /api/idessy/{id}`: Idessy 정보 수정
- **그룹 관리**:
    - `GET /api/idessy/groups/{groupId}/members`: 특정 그룹의 멤버 목록 조회
    - `POST /api/idessy/groups/{groupId}/members`: 그룹에 멤버 초대/추가
    - `DELETE /api/idessy/groups/{groupId}/members/{memberId}`: 그룹에서 멤버 제외
- **내부 시스템용 (Keycloak Protocol Mapper 호출용)**:
    - `GET /internal/anonymous-id?userId={uid}&idessyId={id}&clientId={cid}`: 익명 ID 조회 또는 생성

## 5. Core Logic Flows
- **`GROUP` Idessy 생성 시나리오**:
    1. 사용자가 `POST /api/idessy` (type: `'GROUP'`) API를 호출합니다.
    2. IdessyService는 Keycloak Admin API를 호출하여 이 그룹만을 위한 **`'CORPORATE'` 타입의 User를 백그라운드에서 생성**합니다.
    3. `idessys` 테이블에 새로운 `GROUP` Idessy 정보를 저장합니다. 이때 `keycloak_user_id`에는 방금 생성된 Corporate User의 ID가 기록됩니다.
    4. API를 호출한 사용자를 **첫 번째 법적 대표자**로 지정하여 `corporate_representatives` 테이블에 관계를 기록합니다.
    5. API를 호출한 사용자의 `PERSONAL` Idessy를 해당 그룹의 첫 `ADMIN` 멤버로 `group_memberships` 테이블에 기록합니다.

- **익명 ID 발급 시나리오**:
    1. 사용자가 외부 Realm에서 'Orielle로 로그인'을 시도합니다.
    2. 모든 인증/인가 과정이 끝나고 Access Token이 발급되기 직전, Keycloak에 설치된 **커스텀 프로토콜 매퍼**가 동작합니다.
    3. 프로토콜 매퍼는 현재 로그인한 사용자의 실제 `user_id`, 선택된 `idessy_id`, 그리고 대상 `realm_client_id`를 파라미터로 삼아 IdessyService의 `GET /internal/anonymous-id` API를 호출합니다.
    4. IdessyService는 `anonymous_links` 테이블에서 세 개의 파라미터와 일치하는 레코드를 조회합니다.
    5. 레코드가 존재하면 해당 `anonymous_id`를 반환하고, 존재하지 않으면 새로운 UUID로 `anonymous_id`를 생성하여 테이블에 저장한 뒤 반환합니다.
    6. 프로토콜 매퍼는 반환받은 `anonymous_id`로 토큰의 `sub` 클레임을 **교체**하여 최종 토큰을 발급합니다.

## 6. Dependencies & Common Modules
- **Keycloak**: User 인증 및 `keycloak_user_id` 제공, Admin API를 통한 Corporate User 생성
- **PostgreSQL**: 서비스의 모든 테이블 데이터 저장
- **orielle-common (utils)**: 공통 Enum(IdessyType, GroupRole), 표준 API 응답 DTO, 공통 예외 처리 클래스 등을 이 모듈에서 가져와 사용합니다.
