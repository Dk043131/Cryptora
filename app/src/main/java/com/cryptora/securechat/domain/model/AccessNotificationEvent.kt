package com.cryptora.securechat.domain.model

sealed class AccessNotificationEvent(
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    data class RequestReceived(
        val requestId: String,
        val requesterUsername: String,
        val contentTitle: String,
        val requestedDuration: Long,
        val secureMessageId: String,
        val conversationId: String
    ) : AccessNotificationEvent(
        title = "🔐 Access Request",
        message = "@$requesterUsername wants access to: $contentTitle"
    )

    data class RequestApproved(
        val requestId: String,
        val contentTitle: String,
        val grantedDuration: Long,
        val secureMessageId: String
    ) : AccessNotificationEvent(
        title = "🔓 Access Granted",
        message = "Your request to access \"$contentTitle\" was approved."
    )

    data class RequestRejected(
        val requestId: String,
        val contentTitle: String,
        val secureMessageId: String
    ) : AccessNotificationEvent(
        title = "🚫 Access Request Rejected",
        message = "Your request to access \"$contentTitle\" was declined by the sender."
    )

    data class AccessExpired(
        val secureMessageId: String,
        val contentTitle: String
    ) : AccessNotificationEvent(
        title = "⌛ Access Expired",
        message = "Access window for \"$contentTitle\" has expired. Content keys have been shredded."
    )

    data class AccessRevoked(
        val secureMessageId: String,
        val contentTitle: String
    ) : AccessNotificationEvent(
        title = "🛑 Access Revoked",
        message = "Access to \"$contentTitle\" was revoked by the sender."
    )
}
