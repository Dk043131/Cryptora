package com.cryptora.securechat.domain.usecase.forwarding

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.repository.ForwardingRepository
import javax.inject.Inject

class VerifyForwardChainIntegrityUseCase @Inject constructor(
    private val forwardingRepository: ForwardingRepository
) {
    suspend operator fun invoke(rootMessageId: String): Resource<ForwardChainIntegrity> {
        return forwardingRepository.verifyChainIntegrity(rootMessageId)
    }
}
