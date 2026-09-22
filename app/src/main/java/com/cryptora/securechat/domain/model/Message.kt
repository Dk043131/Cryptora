package com.cryptora.securechat.domain.model

enum class MessageStatus {
    ENCRYPTED_SENT,   // 🔒 Ciphertext stored; not yet fetched
    DELIVERED,        // 🔓 Red/Amber: Arrived at recipient device
    DECRYPTED_SEEN,   // 🔓 Green: Decrypted and viewed
    EXPIRED,          // ⏱️ Expiry reached; key shredded
    REVOKED           // 🚫 Manually revoked by sender
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val encryptedContentBase64: String,
    val encryptionMetadata: EncryptionMetadata,
    val policy: SecureMessagePolicy,
    val status: MessageStatus = MessageStatus.ENCRYPTED_SENT,
    val messageType: MessageType = MessageType.TEXT,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val isOutgoing: Boolean = true,
    val decryptedTextCache: String? = null,
    val forwardCount: Int = 0,
    val isForwarded: Boolean = false,
    val rootMessageId: String = id,
    val originalSenderUsername: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attachment: Attachment? = null
) {
    val isExpiredOrRevoked: Boolean
        get() = status == MessageStatus.EXPIRED || status == MessageStatus.REVOKED || policy.isExpired(timestamp)
}
