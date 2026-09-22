package com.cryptora.securechat.domain.model

data class AuthSession(
    val token: String,
    val user: User,
    val loggedInAt: Long = System.currentTimeMillis()
)
