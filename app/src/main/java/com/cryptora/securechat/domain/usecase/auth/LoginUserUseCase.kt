package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Resource<User> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank()) {
            return Resource.Error(AppError.Validation("Username cannot be empty"))
        }
        if (password.isBlank()) {
            return Resource.Error(AppError.Validation("Password cannot be empty"))
        }
        return authRepository.loginUser(cleanUsername, password)
    }
}
