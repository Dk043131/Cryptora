package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class MarkMessageReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(messageId: String): Resource<Unit> {
        return messageRepository.markMessageAsRead(messageId)
    }
}
