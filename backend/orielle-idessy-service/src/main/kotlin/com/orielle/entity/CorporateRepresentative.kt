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

    val appointedAt: LocalDateTime = LocalDateTime.now()
)
