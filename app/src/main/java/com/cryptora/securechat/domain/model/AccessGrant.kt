package com.cryptora.securechat.domain.model

data class AccessGrant(
    val grantId: String,
    val requestId: String,
    val secureMessageId: String,
    val granteeId: String,
    val grantedDuration: Long,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isRevoked: Boolean = false
) {
    fun isExpired(serverTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return isRevoked || serverTimeMillis >= expiresAt
    }

    fun remainingTimeMillis(serverTimeMillis: Long = System.currentTimeMillis()): Long {
        if (isRevoked) return 0L
        val diff = expiresAt - serverTimeMillis
        return if (diff > 0) diff else 0L
    }
}
