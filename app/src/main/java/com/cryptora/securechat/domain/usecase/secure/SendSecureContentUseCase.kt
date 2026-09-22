package com.cryptora.securechat.domain.usecase.secure

import android.net.Uri
import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class SendSecureContentUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        recipientId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUri: Uri? = null,
        policy: SecureMessagePolicy = SecureMessagePolicy()
    ): Resource<Message> {
        return when (messageType) {
            MessageType.TEXT -> {
                if (content.isBlank()) {
                    return Resource.Error(AppError.Validation("Secure message content cannot be blank"))
                }
                messageRepository.sendTextMessage(conversationId, recipientId, content, policy)
            }
            MessageType.IMAGE, MessageType.FILE -> {
                if (attachmentUri == null) {
                    return Resource.Error(AppError.Validation("Attachment URI is required for media/file message"))
                }
                messageRepository.sendAttachmentMessage(conversationId, recipientId, attachmentUri, messageType, policy)
            }
        }
    }
}
