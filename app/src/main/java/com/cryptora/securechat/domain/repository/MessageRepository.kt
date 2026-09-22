package com.cryptora.securechat.domain.repository

import android.net.Uri
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MessageRepository {
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): Resource<List<Message>>
    suspend fun sendTextMessage(
        conversationId: String,
        recipientId: String,
        text: String
    ): Resource<Message>
    suspend fun sendTextMessage(
        conversationId: String,
        recipientId: String,
        text: String,
        policy: SecureMessagePolicy
    ): Resource<Message>
    suspend fun sendAttachmentMessage(
        conversationId: String,
        recipientId: String,
        uri: Uri,
        messageType: MessageType
    ): Resource<Message>
    suspend fun sendAttachmentMessage(
        conversationId: String,
        recipientId: String,
        uri: Uri,
        messageType: MessageType,
        policy: SecureMessagePolicy
    ): Resource<Message>
    suspend fun retryMessage(messageId: String): Resource<Message>
    suspend fun markMessageAsRead(messageId: String): Resource<Unit>
    suspend fun decryptMessage(message: Message): Resource<String>
    suspend fun decryptAttachment(attachment: Attachment): Resource<File>
    suspend fun revokeMessage(messageId: String): Resource<Unit>
    suspend fun purgeExpiredMessages(): Resource<Int>
    suspend fun sendEncryptedMessage(message: Message): Resource<Message>
    suspend fun searchLocalMessages(query: String): Resource<List<Message>>
    suspend fun deleteLocalMessage(messageId: String): Resource<Unit>
}
