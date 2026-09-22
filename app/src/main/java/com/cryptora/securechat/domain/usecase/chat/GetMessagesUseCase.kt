package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return messageRepository.getMessages(conversationId)
    }
}
