package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index("timestamp"),
        Index("expiresAt"),
        Index("rootMessageId")
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val encryptedContentBase64: String,
    val keyAlias: String,
    val ivBase64: String,
    val messageType: String,
    val deliveryStatus: String,
    val isOutgoing: Boolean,
    val decryptedTextCache: String?,
    val timestamp: Long,
    val hasAttachment: Boolean = false,
    val accessMode: String = "IMMEDIATE_ACCESS",
    val expiresAt: Long? = null,
    val forwardingPolicy: String = "FORWARDING_DISABLED",
    val approvalRequired: Boolean = false,
    val policyOwnerId: String = "",
    val policyCreatedAt: Long = 0L,
    val policyVersion: Int = 1,
    val isAccessGranted: Boolean = true,
    val rootMessageId: String = id,
    val forwardCount: Int = 0,
    val isForwarded: Boolean = false,
    val originalSenderUsername: String? = null
)
