package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.AccessRequestStatus

@Entity(
    tableName = "access_requests",
    indices = [
        Index("messageId"),
        Index("status")
    ]
)
data class AccessRequestEntity(
    @PrimaryKey val requestId: String,
    val messageId: String,
    val conversationId: String,
    val requesterId: String,
    val requesterUsername: String,
    val ownerId: String,
    val contentTitle: String = "Secure Content",
    val requestedDuration: Long = 30L * 60L * 1000L,
    val status: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val grantedDuration: Long? = null,
    val expiresAt: Long? = null
) {
    fun toDomain(): AccessRequest {
        val s = try {
            AccessRequestStatus.valueOf(status)
        } catch (e: Exception) {
            AccessRequestStatus.PENDING
        }
        return AccessRequest(
            requestId = requestId,
            secureMessageId = messageId,
            conversationId = conversationId,
            requesterId = requesterId,
            requesterUsername = requesterUsername,
            ownerId = ownerId,
            contentTitle = contentTitle,
            requestedDuration = requestedDuration,
            status = s,
            requestedAt = requestedAt,
            respondedAt = respondedAt,
            grantedDuration = grantedDuration,
            expiresAt = expiresAt
        )
    }

    companion object {
        fun fromDomain(domain: AccessRequest): AccessRequestEntity = AccessRequestEntity(
            requestId = domain.requestId,
            messageId = domain.secureMessageId,
            conversationId = domain.conversationId,
            requesterId = domain.requesterId,
            requesterUsername = domain.requesterUsername,
            ownerId = domain.ownerId,
            contentTitle = domain.contentTitle,
            requestedDuration = domain.requestedDuration,
            status = domain.status.name,
            requestedAt = domain.requestedAt,
            respondedAt = domain.respondedAt,
            grantedDuration = domain.grantedDuration,
            expiresAt = domain.expiresAt
        )
    }
}
