package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<User?> {
        return authRepository.restoreSession()
    }
}
