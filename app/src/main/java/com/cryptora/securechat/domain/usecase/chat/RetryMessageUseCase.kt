package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class RetryMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(messageId: String): Resource<Message> {
        return messageRepository.retryMessage(messageId)
    }
}
