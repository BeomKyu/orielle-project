# 🚀 Idessy-Service 개발 가이드 (v2.2 - 최종 설계 반영)

## 🎯 1. 핵심 목표

`idessy-service`는 Orielle의 가장 독창적인 개념인 **'데이터 주권'**을 기술적으로 구현하는 핵심 서비스입니다. 이 서비스는 **Spring WebFlux**를 기반으로 하므로, 데이터베이스 접근 역시 완전한 논블로킹(Non-Blocking) 방식인 **R2DBC**를 사용하여 구현합니다.

이 문서는 프로젝트의 최신 코드와 '최종 통합 설계서'를 모두 반영한 **단일 진실 공급원(Single Source of Truth)** 역할을 합니다.

## ✅ 2. 개발 로드맵

### 1단계: R2DBC 의존성 확인

`build.gradle.kts` 파일에 Spring Data R2DBC와 PostgreSQL용 R2DBC 드라이버 의존성이 올바르게 포함되어 있는지 확인합니다. `libs.versions.toml`의 `springBootR2dbcEcosystemInKotlin` 번들을 사용하는 것이 좋습니다.

```kotlin
// backend/orielle-idessy-service/build.gradle.kts
dependencies {
    // springBootStarterWebflux, springBootStarterDataR2dbc, r2dbcPostgresql 등이 포함됨
    implementation(libs.bundles.springBootR2dbcEcosystemInKotlin)
    // ...
}
```

### 2단계: 데이터 모델 (Entity) 최종 확정

'최종 통합 설계서'와 최신 코드를 비교하여 빠진 필드를 모두 추가한 최종 엔티티 모델입니다.

#### Entity 1: `Idessy` (핵심 페르소나)
* `description`, `anonymousId`, `isActive` 필드를 추가하여 설계를 완성했습니다.

```kotlin
// src/main/kotlin/com/orielle/entity/Idessy.kt
package com.orielle.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("idessys")
data class Idessy(
    @Id
    val id: UUID = UUID.randomUUID(),

    // Keycloak의 User ID와 연결됩니다.
    val userId: UUID,

    val type: IdessyType, // PERSONAL or GROUP

    var displayName: String,
    
    var description: String? = null, // Idessy 설명

    val anonymousId: String, // 외부 서비스에 노출될 익명 ID

    var isActive: Boolean = true, // Idessy 활성화/비활성화 상태

    var profileImageUrl: String? = null,

    var anonymousEmail: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class IdessyType {
    PERSONAL,
    GROUP
}
```

#### Entity 2: `GroupMembership` (그룹 멤버)
* 기존 코드가 이미 완벽하여 그대로 유지합니다. `role` 필드에 `GroupRole` Enum을 사용한 것이 좋습니다.

```kotlin
// src/main/kotlin/com/orielle/entity/GroupMembership.kt
package com.orielle.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("group_memberships")
data class GroupMembership(
    @Id
    val id: UUID = UUID.randomUUID(),

    val groupIdessyId: UUID,
    val memberIdessyId: UUID,
    
    var role: GroupRole, // ADMIN or MEMBER

    val joinedAt: LocalDateTime = LocalDateTime.now()
)

enum class GroupRole {
    ADMIN,
    MEMBER
}
```

#### Entity 3: `CorporateRepresentative` (법적 대표자)
* 설계서에 명시된 `role` 필드를 추가하여 법적 대표자의 권한을 명시하도록 수정했습니다.

```kotlin
// src/main/kotlin/com/orielle/entity/CorporateRepresentative.kt
package com.orielle.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("corporate_representatives")
data class CorporateRepresentative(
    @Id
    val id: UUID = UUID.randomUUID(),

    // 그룹 Idessy에 연결된 Corporate User의 ID
    val corporateUserId: UUID,

    // 법적 대표자인 Personal User의 ID
    val personalUserId: UUID,
    
    var role: String, // 예: "OWNER", "ADMIN" 등

    val appointedAt: LocalDateTime = LocalDateTime.now()
)
```

### 3단계: 리액티브 리포지토리(Repository) 구현

R2DBC의 `ReactiveCrudRepository`를 상속받아 각 엔티티에 맞는 리포지토리를 구현합니다.

```kotlin
// IdessyRepository.kt
interface IdessyRepository : ReactiveCrudRepository<Idessy, UUID> {
    fun findByUserId(userId: UUID): Flux<Idessy>
}

// GroupMembershipRepository.kt
interface GroupMembershipRepository : ReactiveCrudRepository<GroupMembership, UUID> {
    fun findByGroupIdessyId(groupIdessyId: UUID): Flux<GroupMembership>
}

// CorporateRepresentativeRepository.kt
interface CorporateRepresentativeRepository : ReactiveCrudRepository<CorporateRepresentative, UUID> {
    fun findByCorporateUserId(corporateUserId: UUID): Flux<CorporateRepresentative>
}
```

### 4단계: `GROUP` 타입 Idessy 생성 로직 (리액티브 파이프라인)

`POST /api/idessy` 요청 시, `type`이 `'GROUP'`인 경우의 로직을 `Mono`와 `Flux`를 사용한 리액티브 파이프라인으로 구성합니다.

1.  **Keycloak에 'CORPORATE' User 생성 요청 (WebClient 사용)**.
2.  응답으로 받은 User ID를 사용하여 `Idessy` 객체 생성 후 **`idessyRepository.save()`** 호출.
3.  저장된 Idessy 정보를 바탕으로 **`corporateRepresentativeRepository.save()`** 와 **`groupMembershipRepository.save()`** 를 연달아 호출.
4.  최종적으로 생성된 Idessy 정보를 `Mono<Idessy>` 형태로 반환.

### 5단계: REST API 엔드포인트 개발 (WebFlux)

컨트롤러의 모든 API는 `Mono`와 `Flux`를 반환 타입으로 사용하여 완전한 리액티브 스택을 유지합니다.

```kotlin
@RestController
@RequestMapping("/api/idessy")
class IdessyController(private val idessyRepository: IdessyRepository) {
    // ... API 구현
}
```
