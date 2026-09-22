package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class SendTextMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        recipientId: String,
        text: String
    ): Resource<Message> {
        return messageRepository.sendTextMessage(conversationId, recipientId, text)
    }
}
