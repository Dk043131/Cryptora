package com.cryptora.securechat.domain.usecase.message

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class DeleteLocalMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(messageId: String): Resource<Unit> {
        return messageRepository.deleteLocalMessage(messageId)
    }
}
