package com.cryptora.securechat.domain.model

data class EncryptionMetadata(
    val algorithm: String = "AES-256-GCM",
    val initializationVectorBase64: String,
    val keyAlias: String,
    val epoch: Long = 1L,
    val saltBase64: String? = null
)
