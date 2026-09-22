package com.cryptora.securechat.security

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.usecase.chat.RetryMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MessageDeliveryAndResilienceTest {

    private lateinit var fakeMessageRepo: FakeDeliveryMessageRepo
    private lateinit var retryMessageUseCase: RetryMessageUseCase

    @Before
    fun setUp() {
        fakeMessageRepo = FakeDeliveryMessageRepo()
        retryMessageUseCase = RetryMessageUseCase(fakeMessageRepo)
    }

    @Test
    fun `retrying failed message transitions delivery status from FAILED to SENT upon network recovery`() = runBlocking {
        val failedMessage = Message(
            id = "msg_fail_01",
            conversationId = "conv_1",
            senderId = "me",
            recipientId = "bob",
            encryptedContentBase64 = "encrypted_blob",
            encryptionMetadata = EncryptionMetadata(initializationVectorBase64 = "", keyAlias = "key"),
            policy = SecureMessagePolicy(),
            deliveryStatus = MessageDeliveryStatus.FAILED,
            messageType = MessageType.TEXT
        )
        fakeMessageRepo.messages[failedMessage.id] = failedMessage

        val result = retryMessageUseCase("msg_fail_01")
        assertTrue(result is Resource.Success)
        val retried = (result as Resource.Success).data

        assertEquals(MessageDeliveryStatus.SENT, retried.deliveryStatus)
    }

    @Test
    fun `retrying unknown message returns NotFound error`() = runBlocking {
        val result = retryMessageUseCase("msg_nonexistent")
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).error is AppError.NotFound)
    }

    private class FakeDeliveryMessageRepo : com.cryptora.securechat.domain.repository.MessageRepository {
        val messages = mutableMapOf<String, Message>()

        override suspend fun retryMessage(messageId: String): Resource<Message> {
            val msg = messages[messageId]
                ?: return Resource.Error(AppError.NotFound("Message $messageId not found"))
            val updated = msg.copy(deliveryStatus = MessageDeliveryStatus.SENT)
            messages[messageId] = updated
            return Resource.Success(updated)
        }

        override fun getMessages(conversationId: String): Flow<List<Message>> = flowOf(messages.values.toList())
        override suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): Resource<List<Message>> = Resource.Success(messages.values.toList())
        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String): Resource<Message> = Resource.Error(AppError.Unknown("Not used"))
        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String, policy: SecureMessagePolicy): Resource<Message> = Resource.Error(AppError.Unknown("Not used"))
        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType): Resource<Message> = Resource.Error(AppError.Unknown("Not used"))
        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType, policy: SecureMessagePolicy): Resource<Message> = Resource.Error(AppError.Unknown("Not used"))
        override suspend fun markMessageAsRead(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun decryptMessage(message: Message): Resource<String> = Resource.Success(message.decryptedTextCache ?: "")
        override suspend fun decryptAttachment(attachment: com.cryptora.securechat.domain.model.Attachment): Resource<File> = Resource.Error(AppError.Unknown("Not used"))
        override suspend fun revokeMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun purgeExpiredMessages(): Resource<Int> = Resource.Success(0)
        override suspend fun sendEncryptedMessage(message: Message): Resource<Message> = Resource.Success(message)
        override suspend fun searchLocalMessages(query: String): Resource<List<Message>> = Resource.Success(emptyList())
        override suspend fun deleteLocalMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)
    }
}
