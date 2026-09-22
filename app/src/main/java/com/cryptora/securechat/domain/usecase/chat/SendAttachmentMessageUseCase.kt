package com.cryptora.securechat.domain.usecase.chat

import android.net.Uri
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class SendAttachmentMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        recipientId: String,
        uri: Uri,
        messageType: MessageType
    ): Resource<Message> {
        return messageRepository.sendAttachmentMessage(conversationId, recipientId, uri, messageType)
    }
}
