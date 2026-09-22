package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        fullName: String,
        username: String,
        mobileNumber: String,
        password: String,
        avatarUrl: String? = null,
        bio: String = ""
    ): Resource<User> {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim().lowercase()

        if (cleanName.isBlank()) {
            return Resource.Error(AppError.Validation("Please enter your name"))
        }
        if (cleanUsername.length < 3) {
            return Resource.Error(AppError.Validation("Username must be at least 3 characters"))
        }
        if (password.length < 8) {
            return Resource.Error(AppError.Validation("Password must be at least 8 characters long"))
        }
        if (!password.any { it.isUpperCase() }) {
            return Resource.Error(AppError.Validation("Password must contain at least one uppercase letter"))
        }
        if (!password.any { it.isDigit() }) {
            return Resource.Error(AppError.Validation("Password must contain at least one number"))
        }

        return authRepository.registerUser(
            fullName = cleanName,
            username = cleanUsername,
            mobileNumber = mobileNumber.trim(),
            password = password,
            avatarUrl = avatarUrl,
            bio = bio.trim()
        )
    }
}
