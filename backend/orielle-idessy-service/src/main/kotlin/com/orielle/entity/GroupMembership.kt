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
    
    var role: GroupRole,

    val joinedAt: LocalDateTime = LocalDateTime.now()
)

enum class GroupRole {
    ADMIN,
    MEMBER
}
