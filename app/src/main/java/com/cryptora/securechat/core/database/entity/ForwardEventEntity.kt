package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardEvent

@Entity(
    tableName = "forward_events",
    indices = [
        Index("rootMessageId"),
        Index("secureMessageId"),
        Index("timestamp")
    ]
)
data class ForwardEventEntity(
    @PrimaryKey val eventId: String,
    val rootMessageId: String,
    val secureMessageId: String,
    val parentEventId: String?,
    val fromUserId: String,
    val fromUsername: String,
    val toUserId: String,
    val toUsername: String,
    val timestamp: Long,
    val policyVersion: Int = 1,
    val approvalStatus: String = "APPROVED",
    val previousEventHash: String,
    val eventHash: String,
    val signatureMetadata: String = ""
) {
    fun toDomain(): ForwardEvent = ForwardEvent(
        eventId = eventId,
        rootMessageId = rootMessageId,
        secureMessageId = secureMessageId,
        parentEventId = parentEventId,
        fromUserId = fromUserId,
        fromUsername = fromUsername,
        toUserId = toUserId,
        toUsername = toUsername,
        timestamp = timestamp,
        policyVersion = policyVersion,
        approvalStatus = try { ForwardApprovalStatus.valueOf(approvalStatus) } catch (e: Exception) { ForwardApprovalStatus.APPROVED },
        previousEventHash = previousEventHash,
        eventHash = eventHash,
        signatureMetadata = signatureMetadata
    )

    companion object {
        fun fromDomain(event: ForwardEvent): ForwardEventEntity = ForwardEventEntity(
            eventId = event.eventId,
            rootMessageId = event.rootMessageId,
            secureMessageId = event.secureMessageId,
            parentEventId = event.parentEventId,
            fromUserId = event.fromUserId,
            fromUsername = event.fromUsername,
            toUserId = event.toUserId,
            toUsername = event.toUsername,
            timestamp = event.timestamp,
            policyVersion = event.policyVersion,
            approvalStatus = event.approvalStatus.name,
            previousEventHash = event.previousEventHash,
            eventHash = event.eventHash,
            signatureMetadata = event.signatureMetadata
        )
    }
}
