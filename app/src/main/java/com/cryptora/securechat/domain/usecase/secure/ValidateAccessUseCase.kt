package com.cryptora.securechat.domain.usecase.secure

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.AccessValidationResult
import com.cryptora.securechat.domain.repository.SecureContentRepository
import javax.inject.Inject

class ValidateAccessUseCase @Inject constructor(
    private val secureContentRepository: SecureContentRepository
) {
    suspend operator fun invoke(messageId: String): Resource<AccessValidationResult> {
        return secureContentRepository.validateMessageAccess(messageId)
    }
}
