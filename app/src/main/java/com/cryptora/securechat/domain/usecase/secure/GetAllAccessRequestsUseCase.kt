package com.cryptora.securechat.domain.usecase.secure

import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.repository.SecureContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAccessRequestsUseCase @Inject constructor(
    private val secureContentRepository: SecureContentRepository
) {
    operator fun invoke(): Flow<List<AccessRequest>> {
        return secureContentRepository.getAllAccessRequests()
    }
}
