package com.cryptora.securechat.domain.model

data class Conversation(
    val id: String,
    val participantUser: User,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isSecretSession: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
