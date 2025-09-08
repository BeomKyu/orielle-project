# IdessyService Design Document

- **Version**: 1.0
- **Author**: BeomKyu
- **Status**: FINALIZED

## 1. Service Overview
Orielle 플랫폼의 핵심 개념인 'Idessy'(다중 페르소나)를 관리하는 마이크로서비스입니다. Keycloak이 관리하는 중앙 'User'에 종속된 여러 'Idessy' 프로필의 생성, 조회, 상태 변경을 책임지며, 외부 서비스(Realm)에 제공될 '익명 ID'를 관리하여 사용자의 프라이버시를 보장합니다.

## 2. Core Features
- [x] **Idessy 생성/조회/수정/삭제**: 기본적인 CRUD 기능
- [x] **Keycloak User 연동**: Idessy는 특정 Keycloak User ID에 반드시 연결되어야 합니다.
- [x] **익명 ID 관리**: 각 Idessy는 외부 서비스에 노출될 고유한 익명 ID를 가져야 합니다.
- [x] **상태 관리**: Idessy를 활성/비활성 상태로 변경할 수 있어야 합니다.
- [x] **소유자 기반 조회**: 특정 Keycloak User ID로 그 사람이 소유한 모든 Idessy 목록을 조회할 수 있어야 합니다.

## 3. Data Model (ERD)
- **Table: `idessy`**
  - `id` (UUID): Primary Key
  - `keycloak_user_id` (String): Foreign Key (Keycloak User ID)
  - `name` (String): Idessy 이름
  - `description` (String, nullable)
  - `anonymous_id` (String): 외부 서비스용 익명 ID, Unique
  - `is_active` (Boolean): 활성화 여부
  - `created_at` (LocalDateTime)
  - `updated_at` (LocalDateTime)

## 4. API Endpoints
- `GET /api/idessy/my?keycloakUserId={id}`: 내 모든 Idessy 조회
- `GET /api/idessy/my/active?keycloakUserId={id}`: 내 활성화된 Idessy 조회
- `GET /api/idessy/anonymous/{anonymousId}`: 익명 ID로 Idessy 조회 (외부 서비스용)
- `POST /api/idessy`: 새 Idessy 생성
- `PUT /api/idessy/{id}/activate`: Idessy 활성화
- `PUT /api/idessy/{id}/deactivate`: Idessy 비활성화

## 5. Dependencies
- **Keycloak**: User 인증 및 `keycloak_user_id` 제공
- **PostgreSQL**: `idessy` 테이블 데이터 저장
