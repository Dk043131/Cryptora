package com.cryptora.securechat.domain.usecase.forwarding

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.ForwardEvent
import com.cryptora.securechat.domain.repository.ForwardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetForwardChainUseCase @Inject constructor(
    private val forwardingRepository: ForwardingRepository
) {
    operator fun invoke(rootMessageId: String): Flow<List<ForwardEvent>> {
        return forwardingRepository.getForwardChainFlow(rootMessageId)
    }

    suspend fun getList(rootMessageId: String): Resource<List<ForwardEvent>> {
        return forwardingRepository.getForwardChain(rootMessageId)
    }
}
