package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.repository.ChatRepository
import javax.inject.Inject

class GetConversationByIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Resource<Conversation?> {
        return chatRepository.getConversationById(conversationId)
    }
}
