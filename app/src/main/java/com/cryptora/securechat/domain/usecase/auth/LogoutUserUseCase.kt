package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return authRepository.logout()
    }
}
