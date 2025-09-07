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

    var displayName: String,

    val type: IdessyType, // PERSONAL or GROUP

    var profileImageUrl: String? = null,

    var anonymousEmail: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class IdessyType {
    PERSONAL,
    GROUP
}
