package com.cryptora.securechat.domain.model

enum class AccessRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    CANCELLED
}

data class AccessRequest(
    val requestId: String,
    val secureMessageId: String,
    val conversationId: String = "",
    val requesterId: String,
    val requesterUsername: String,
    val ownerId: String,
    val contentTitle: String = "Secure Content",
    val requestedDuration: Long = 30L * 60L * 1000L, // Default 30 minutes
    val status: AccessRequestStatus = AccessRequestStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val grantedDuration: Long? = null,
    val expiresAt: Long? = null
) {
    // Backwards compatibility alias
    val messageId: String
        get() = secureMessageId
}
