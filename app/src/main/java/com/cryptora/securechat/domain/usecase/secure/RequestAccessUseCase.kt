package com.cryptora.securechat.domain.usecase.secure

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.repository.SecureContentRepository
import javax.inject.Inject

class RequestAccessUseCase @Inject constructor(
    private val secureContentRepository: SecureContentRepository
) {
    suspend operator fun invoke(
        messageId: String,
        requestedDuration: Long = 30L * 60L * 1000L,
        contentTitle: String = "Secure Content"
    ): Resource<AccessRequest> {
        return secureContentRepository.requestAccess(messageId, requestedDuration, contentTitle)
    }
}
