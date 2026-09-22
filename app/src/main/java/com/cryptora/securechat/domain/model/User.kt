package com.cryptora.securechat.domain.model

data class User(
    val id: String,
    val username: String,
    val mobileNumber: String,
    val publicKey: String,
    val fullName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
