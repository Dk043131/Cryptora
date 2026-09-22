package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class CheckUsernameAvailabilityUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String): Resource<Boolean> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.length < 3) {
            return Resource.Error(AppError.Validation("Username must be at least 3 characters long"))
        }
        if (cleanUsername.length > 20) {
            return Resource.Error(AppError.Validation("Username must not exceed 20 characters"))
        }
        if (!cleanUsername.matches(Regex("^[a-z0-9_.]+$"))) {
            return Resource.Error(AppError.Validation("Username can only contain lowercase letters, numbers, underscores, and dots"))
        }
        return authRepository.checkUsernameAvailability(cleanUsername)
    }
}
