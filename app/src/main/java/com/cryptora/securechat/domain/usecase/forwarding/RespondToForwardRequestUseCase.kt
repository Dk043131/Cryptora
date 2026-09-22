package com.cryptora.securechat.domain.usecase.forwarding

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.ForwardingRepository
import javax.inject.Inject

class RespondToForwardRequestUseCase @Inject constructor(
    private val forwardingRepository: ForwardingRepository
) {
    suspend operator fun invoke(eventId: String, approved: Boolean): Resource<Unit> {
        return forwardingRepository.respondToForwardRequest(eventId, approved)
    }
}
