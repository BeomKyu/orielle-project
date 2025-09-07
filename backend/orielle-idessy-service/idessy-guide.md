# 🚀 Idessy-Service 개발 시작 가이드 (v2.1 - R2DBC 리액티브 적용)

## 🎯 1. 핵심 목표

`idessy-service`는 Orielle의 가장 독창적인 개념인 **'데이터 주권'**을 기술적으로 구현하는 핵심 서비스입니다. 이 서비스는 **Spring WebFlux**를 기반으로 하므로, 데이터베이스 접근 역시 완전한 논블로킹(Non-Blocking) 방식인 **R2DBC**를 사용하여 구현합니다.

## ✅ 2. 개발 로드맵

### 1단계: R2DBC 의존성 추가

먼저 `build.gradle.kts` 파일에 Spring Data R2DBC와 PostgreSQL용 R2DBC 드라이버 의존성을 추가합니다.

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")
    // ... 기존 WebFlux 의존성
}
```

### 2단계: 데이터 모델 (Entity) 구현

'최종 통합 설계서'에 명시된 3개의 핵심 테이블을 Spring Data R2DBC에 맞게 재정의합니다.

#### Entity 1: `Idessy` (핵심 페르소나)

```kotlin
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("idessys") // JPA의 @Entity 대신 @Table 사용
data class Idessy(
    @Id
    val id: UUID = UUID.randomUUID(),

    // 'PERSONAL' 또는 'GROUP'
    val type: String,

    // Keycloak의 User ID를 참조 ('PERSONAL' 또는 'CORPORATE' User)
    val keycloakUserId: String,

    val name: String,               // 페르소나 이름
    val description: String?,       // 설명
    val anonymousId: String,        // 외부 서비스에 노출될 익명 ID
    val isActive: Boolean = true,   // 활성화 여부
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime
)
```

#### Entity 2: `GroupMembership` (그룹 멤버)

```kotlin
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("group_memberships")
data class GroupMembership(
    @Id
    val id: UUID = UUID.randomUUID(),

    // 어느 그룹에 속해 있는지 (Group Idessy의 ID)
    val groupIdessyId: UUID,

    // 어떤 멤버가 속해 있는지 (Personal Idessy의 ID)
    val memberIdessyId: UUID,

    // 그룹 내에서의 역할 (예: 'admin', 'member')
    val role: String,

    val joinedAt: LocalDateTime
)
```

#### Entity 3: `CorporateRepresentative` (법적 대표자)

```kotlin
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("corporate_representatives")
data class CorporateRepresentative(
    @Id
    val id: UUID = UUID.randomUUID(),

    // 법인격 User (Keycloak의 Corporate User ID)
    val corporateUserId: String,

    // 법적 대표자 User (Keycloak의 Personal User ID)
    val representativeUserId: String,

    // 권한 (예: 'owner', 'admin')
    val role: String,

    val assignedAt: LocalDateTime
)
```

### 3단계: 리액티브 리포지토리(Repository) 구현

JPA의 `JpaRepository` 대신, R2DBC의 `ReactiveCrudRepository`를 상속받아 리포지토리를 구현합니다. 모든 메서드는 `Mono` 또는 `Flux`를 반환합니다.

```kotlin
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import java.util.UUID

interface IdessyRepository : ReactiveCrudRepository<Idessy, UUID> {
    fun findByKeycloakUserId(keycloakUserId: String): Flux<Idessy>
}
```

### 4단계: `GROUP` 타입 Idessy 생성 로직 구현 (리액티브 파이프라인)

`POST /api/idessy` 요청 시, `type`이 `'GROUP'`인 경우의 로직을 `Mono`와 `Flux`를 사용한 리액티브 파이프라인으로 구성합니다.

1.  **Keycloak에 'CORPORATE' User 생성 요청 (WebClient 사용)**.
2.  응답으로 받은 User ID를 사용하여 `Idessy` 객체 생성 후 **`idessyRepository.save()`** 호출.
3.  저장된 Idessy 정보를 바탕으로 **`corporateRepresentativeRepository.save()`** 와 **`groupMembershipRepository.save()`** 를 연달아 호출.
4.  최종적으로 생성된 Idessy 정보를 `Mono<Idessy>` 형태로 반환.

### 5단계: REST API 엔드포인트 개발 (WebFlux)

컨트롤러의 모든 API는 `Mono`와 `Flux`를 반환 타입으로 사용해야 합니다.

```kotlin
@RestController
@RequestMapping("/api/idessy")
class IdessyController(private val idessyRepository: IdessyRepository) {

    @PostMapping
    fun createIdessy(@RequestBody idessy: Idessy): Mono<Idessy> {
        // ... 리액티브 생성 로직
    }

    @GetMapping("/my")
    fun getMyIdessies(@RequestParam keycloakUserId: String): Flux<Idessy> {
        return idessyRepository.findByKeycloakUserId(keycloakUserId)
    }

    // ... 기타 API
}
```