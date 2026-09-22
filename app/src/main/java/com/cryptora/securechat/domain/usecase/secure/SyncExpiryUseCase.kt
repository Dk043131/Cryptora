package com.cryptora.securechat.domain.usecase.secure

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.SecureContentRepository
import javax.inject.Inject

class SyncExpiryUseCase @Inject constructor(
    private val secureContentRepository: SecureContentRepository
) {
    suspend operator fun invoke(): Resource<Int> {
        return secureContentRepository.syncAuthoritativeExpiry()
    }
}
