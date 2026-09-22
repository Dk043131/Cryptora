package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantUserId: String,
    val participantUsername: String,
    val participantFullName: String,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val lastMessageDeliveryStatus: String?,
    val unreadCount: Int = 0,
    val isSecretSession: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
