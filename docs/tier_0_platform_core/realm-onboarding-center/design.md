# Realm Onboarding Center Design Document

- **Version**: 1.0
- **Author**: BeomKyu
- **Status**: DRAFT

## 1. Service Overview
외부 개발자들이 Orielle 생태계에 자신의 서비스(Realm)를 입점시키기 위해 방문하는 공식적인 **'건축 허가 사무소'이자 '개발자 포털'**입니다. Keycloak의 복잡한 기술적 설정들을 감추고, 개발자들에게 쉽고 편리한 온보딩 경험을 제공하는 것을 목표로 합니다.

## 2. Core Features
- [ ] **Self-Service Realm Registration**: 개발자가 포털 UI를 통해 자신의 서비스(Realm) 정보를 직접 입력하고 등록할 수 있습니다.
- [ ] **Automated Keycloak Client Creation**: 개발자가 등록을 완료하면, 백엔드는 Keycloak의 Admin API를 호출하여 해당 Realm을 위한 'Client'를 자동으로 생성하고 `Client ID`와 `Client Secret`을 발급합니다.
- [ ] **Credential Management**: 발급된 `Client Secret`을 안전하게 조회하고, 필요시 재발급할 수 있는 기능을 제공합니다.
- [ ] **Progressive Scope Management**: 개발자는 처음 등록 시 최소한의 권한(Scope)만 할당받으며, 친구 목록 조회(`friends.read`) 등 추가 기능이 필요하면 이 포털을 통해 필요한 권한을 '요청'하고 승인받는 절차를 거칩니다.
- [ ] **Usage Dashboard**: 자신의 Realm이 Orielle API를 얼마나 사용하고 있는지 통계를 보여주는 대시보드를 제공합니다.

## 3. Data Model (ERD)
- **Table: `realms`**
  - `id` (UUID): PK
  - `owner_user_id` (String): 이 Realm을 소유한 개발자의 Keycloak User ID
  - `name` (String): Realm 이름
  - `description` (String)
  - `redirect_uris` (Array<String>)
  - `keycloak_client_id` (String): Keycloak에 생성된 Client의 ID
  - `status` (String): (e.g., PENDING, APPROVED, REJECTED)

## 4. Dependencies
- **Keycloak**: Admin API를 사용하여 Client를 생성하고 관리합니다.
- **PostgreSQL**: `realms` 테이블 정보를 저장합니다.
