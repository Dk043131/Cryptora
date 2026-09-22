package com.cryptora.securechat.domain.usecase.user

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.UserProfile
import com.cryptora.securechat.domain.repository.UserRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(rawQuery: String): Resource<List<UserProfile>> {
        val cleanQuery = rawQuery.trim().removePrefix("@").lowercase()

        if (cleanQuery.isEmpty()) {
            return Resource.Success(emptyList())
        }

        if (cleanQuery.length < 2) {
            return Resource.Error(AppError.Validation("Search query must be at least 2 characters"))
        }

        if (!cleanQuery.matches(Regex("^[a-z0-9_.]+$"))) {
            return Resource.Error(AppError.Validation("Username contains invalid characters"))
        }

        return userRepository.searchUsers(cleanQuery)
    }
}
