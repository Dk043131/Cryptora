package com.cryptora.securechat.domain.model

data class UserProfile(
    val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val publicKeyFingerprint: String = "",
    val isVerified: Boolean = true
)
