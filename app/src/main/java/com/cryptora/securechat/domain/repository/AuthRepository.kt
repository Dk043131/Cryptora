package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthenticatedUser(): Flow<User?>
    suspend fun checkUsernameAvailability(username: String): Resource<Boolean>
    suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String>
    suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean>
    suspend fun registerUser(
        fullName: String,
        username: String,
        mobileNumber: String,
        password: String,
        avatarUrl: String? = null,
        bio: String = ""
    ): Resource<User>
    suspend fun loginUser(username: String, password: String): Resource<User>
    suspend fun restoreSession(): Resource<User?>
    suspend fun logout(): Resource<Unit>
}
