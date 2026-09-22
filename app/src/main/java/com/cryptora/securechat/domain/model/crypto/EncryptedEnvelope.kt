package com.cryptora.securechat.domain.model.crypto

data class EncryptedEnvelope(
    val ciphertextBase64: String,
    val ivBase64: String,
    val keyVersion: Int,
    val algorithm: String = "AES-256-GCM",
    val ephemeralPublicKeyBase64: String? = null,
    val wrappedKeyBase64: String? = null,
    val senderFingerprint: String? = null
)
