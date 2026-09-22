package com.cryptora.securechat.domain.model.crypto

data class DeviceIdentity(
    val deviceId: String,
    val publicKeyBase64: String,
    val keyVersion: Int,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)
