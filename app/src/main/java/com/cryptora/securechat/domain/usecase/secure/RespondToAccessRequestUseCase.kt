package com.cryptora.securechat.domain.usecase.secure

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.AccessGrant
import com.cryptora.securechat.domain.repository.SecureContentRepository
import javax.inject.Inject

class RespondToAccessRequestUseCase @Inject constructor(
    private val secureContentRepository: SecureContentRepository
) {
    suspend operator fun invoke(
        requestId: String,
        approved: Boolean,
        finalDurationMillis: Long? = null
    ): Resource<AccessGrant?> {
        return secureContentRepository.respondToAccessRequest(requestId, approved, finalDurationMillis)
    }
}
