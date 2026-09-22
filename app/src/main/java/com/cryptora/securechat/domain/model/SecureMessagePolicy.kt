package com.cryptora.securechat.domain.model

enum class AccessType {
    UNRESTRICTED,
    TIME_LOCKED,
    APPROVAL_REQUIRED,
    REVOKED
}

data class SecureMessagePolicy(
    val accessMode: AccessMode = AccessMode.IMMEDIATE_ACCESS,
    val expiresAt: Long? = null, // Authoritative server timestamp (epoch millis)
    val forwardingPolicy: ForwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED,
    val approvalRequired: Boolean = false,
    val ownerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val policyVersion: Int = 1,
    val isAccessGranted: Boolean = true,
    // Backwards compatibility fields
    val expiryDurationMillis: Long? = null,
    val requiresApprovalToForward: Boolean = false,
    val maxForwardCount: Int = 1,
    val isRevoked: Boolean = false,
    val revokedAt: Long? = null,
    val accessType: AccessType = AccessType.UNRESTRICTED
) {
    fun isExpired(authoritativeServerTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (isRevoked) return true
        val targetExpiry = expiresAt ?: expiryDurationMillis?.let { createdAt + it } ?: return false
        return authoritativeServerTimeMillis >= targetExpiry
    }

    fun remainingTimeMillis(authoritativeServerTimeMillis: Long = System.currentTimeMillis()): Long {
        if (isRevoked) return 0L
        val targetExpiry = expiresAt ?: expiryDurationMillis?.let { createdAt + it } ?: return Long.MAX_VALUE
        val remaining = targetExpiry - authoritativeServerTimeMillis
        return if (remaining > 0) remaining else 0L
    }

    val isLocked: Boolean
        get() = accessMode == AccessMode.REQUEST_ACCESS && !isAccessGranted
}
