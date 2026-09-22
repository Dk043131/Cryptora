package com.cryptora.securechat.domain.usecase.forwarding

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.ForwardEvent
import com.cryptora.securechat.domain.repository.ForwardingRepository
import javax.inject.Inject

class ForwardSecureMessageUseCase @Inject constructor(
    private val forwardingRepository: ForwardingRepository
) {
    suspend operator fun invoke(
        messageId: String,
        targetConversationId: String,
        targetUserId: String,
        targetUsername: String
    ): Resource<ForwardEvent> {
        return forwardingRepository.forwardMessage(
            messageId = messageId,
            targetConversationId = targetConversationId,
            targetUserId = targetUserId,
            targetUsername = targetUsername
        )
    }
}
