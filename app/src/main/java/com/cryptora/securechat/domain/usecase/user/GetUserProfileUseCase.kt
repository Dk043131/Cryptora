package com.cryptora.securechat.domain.usecase.user

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.UserProfile
import com.cryptora.securechat.domain.repository.UserRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Resource<UserProfile> {
        if (userId.isBlank()) {
            return Resource.Error(AppError.Validation("User ID cannot be blank"))
        }
        return userRepository.getUserProfile(userId)
    }
}
