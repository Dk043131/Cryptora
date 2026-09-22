package com.cryptora.securechat.domain.model

enum class MessageDeliveryStatus {
    SENDING,   // Local processing & encrypting
    SENT,      // 🔒 Ciphertext stored on server
    DELIVERED, // 🔓 Red: Delivered to recipient device
    READ,      // 🔓 Green: Decrypted and viewed
    FAILED     // ⚠️ Failed; eligible for retry
}
