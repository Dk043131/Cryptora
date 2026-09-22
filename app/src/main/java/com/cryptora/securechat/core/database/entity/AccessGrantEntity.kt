package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptora.securechat.domain.model.AccessGrant

@Entity(
    tableName = "access_grants",
    indices = [
        Index("secureMessageId"),
        Index("granteeId"),
        Index("expiresAt")
    ]
)
data class AccessGrantEntity(
    @PrimaryKey val grantId: String,
    val requestId: String,
    val secureMessageId: String,
    val granteeId: String,
    val grantedDuration: Long,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isRevoked: Boolean = false
) {
    fun toDomain(): AccessGrant = AccessGrant(
        grantId = grantId,
        requestId = requestId,
        secureMessageId = secureMessageId,
        granteeId = granteeId,
        grantedDuration = grantedDuration,
        grantedAt = grantedAt,
        expiresAt = expiresAt,
        isRevoked = isRevoked
    )

    companion object {
        fun fromDomain(grant: AccessGrant): AccessGrantEntity = AccessGrantEntity(
            grantId = grant.grantId,
            requestId = grant.requestId,
            secureMessageId = grant.secureMessageId,
            granteeId = grant.granteeId,
            grantedDuration = grant.grantedDuration,
            grantedAt = grant.grantedAt,
            expiresAt = grant.expiresAt,
            isRevoked = grant.isRevoked
        )
    }
}
