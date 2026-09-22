package com.cryptora.securechat.ux

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.notification.InAppNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationType
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.MessageRepository
import com.cryptora.securechat.domain.usecase.message.DeleteLocalMessageUseCase
import com.cryptora.securechat.domain.usecase.search.SearchConversationsUseCase
import com.cryptora.securechat.domain.usecase.search.SearchLocalMessagesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class UserExperienceSubsystemTest {

    private lateinit var fakeMessageRepo: FakeMessageRepository
    private lateinit var fakeChatRepo: FakeChatRepository
    private lateinit var searchLocalMessagesUseCase: SearchLocalMessagesUseCase
    private lateinit var searchConversationsUseCase: SearchConversationsUseCase
    private lateinit var deleteLocalMessageUseCase: DeleteLocalMessageUseCase
    private lateinit var inAppNotificationManager: InAppNotificationManager

    private val testDispatchers = object : com.cryptora.securechat.core.common.DispatcherProvider {
        override val main: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        override val io: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        override val default: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        override val unconfined: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        fakeMessageRepo = FakeMessageRepository()
        fakeChatRepo = FakeChatRepository()
        searchLocalMessagesUseCase = SearchLocalMessagesUseCase(fakeMessageRepo)
        searchConversationsUseCase = SearchConversationsUseCase(fakeChatRepo)
        deleteLocalMessageUseCase = DeleteLocalMessageUseCase(fakeMessageRepo)
        inAppNotificationManager = InAppNotificationManager(testDispatchers)
    }

    @Test
    fun testSearchLocalMessagesFindsMatchingNonExpired() = runBlocking {
        val msg1 = createSampleMessage(
            id = "msg_1",
            text = "Here are the project credentials for AWS",
            isExpired = false
        )
        val msg2 = createSampleMessage(
            id = "msg_2",
            text = "Meeting tomorrow morning at 10 AM",
            isExpired = false
        )
        val msg3 = createSampleMessage(
            id = "msg_3",
            text = "Expired credentials that should not appear",
            isExpired = true
        )

        fakeMessageRepo.messages.addAll(listOf(msg1, msg2, msg3))

        val result = searchLocalMessagesUseCase("credentials")
        assertTrue(result is Resource.Success)
        val results = (result as Resource.Success).data

        assertEquals(1, results.size)
        assertEquals("msg_1", results[0].id)
        assertEquals("Here are the project credentials for AWS", results[0].decryptedTextCache)
    }

    @Test
    fun testSearchLocalMessagesWithEmptyQuery() = runBlocking {
        val msg = createSampleMessage("msg_1", "Secret test", false)
        fakeMessageRepo.messages.add(msg)

        val emptyResult = searchLocalMessagesUseCase("")
        val blankResult = searchLocalMessagesUseCase("   ")

        assertTrue(emptyResult is Resource.Success)
        assertTrue((emptyResult as Resource.Success).data.isEmpty())

        assertTrue(blankResult is Resource.Success)
        assertTrue((blankResult as Resource.Success).data.isEmpty())
    }

    @Test
    fun testSearchConversationsMatchesNameAndUsername() = runBlocking {
        val user1 = User(
            id = "user_1",
            username = "harshanth",
            fullName = "Harshanth Kumar",
            mobileNumber = "+919876543210",
            publicKey = "pubkey_1"
        )
        val conv1 = Conversation(
            id = "conv_1",
            participantUser = user1,
            lastMessage = null,
            updatedAt = System.currentTimeMillis()
        )

        val user2 = User(
            id = "user_2",
            username = "karthiga",
            fullName = "Karthiga S",
            mobileNumber = "+919876543211",
            publicKey = "pubkey_2"
        )
        val conv2 = Conversation(
            id = "conv_2",
            participantUser = user2,
            lastMessage = null,
            updatedAt = System.currentTimeMillis()
        )

        fakeChatRepo.conversations.addAll(listOf(conv1, conv2))

        val nameResult = searchConversationsUseCase("Harshanth")
        val usernameResult = searchConversationsUseCase("karthiga")
        val noMatchResult = searchConversationsUseCase("unknown_user")

        assertTrue(nameResult is Resource.Success)
        val nameMatches = (nameResult as Resource.Success).data
        assertEquals(1, nameMatches.size)
        assertEquals("conv_1", nameMatches[0].id)

        assertTrue(usernameResult is Resource.Success)
        val usernameMatches = (usernameResult as Resource.Success).data
        assertEquals(1, usernameMatches.size)
        assertEquals("conv_2", usernameMatches[0].id)

        assertTrue(noMatchResult is Resource.Success)
        assertTrue((noMatchResult as Resource.Success).data.isEmpty())
    }

    @Test
    fun testDeleteLocalMessageRemovesFromStore() = runBlocking {
        val msg1 = createSampleMessage("msg_1", "Keep this message", false)
        val msg2 = createSampleMessage("msg_2", "Delete this message locally", false)

        fakeMessageRepo.messages.addAll(listOf(msg1, msg2))

        val deleteResult = deleteLocalMessageUseCase("msg_2")
        assertTrue(deleteResult is Resource.Success)

        assertEquals(1, fakeMessageRepo.messages.size)
        assertEquals("msg_1", fakeMessageRepo.messages[0].id)
    }

    @Test
    fun testInAppNotificationManagerAntiSpamDeduplication() {
        inAppNotificationManager.showNotification(
            title = "✓ Secure message sent",
            type = InAppNotificationType.SUCCESS,
            durationMillis = 3500L
        )

        val firstNotif = inAppNotificationManager.currentNotification.value
        assertNotNull(firstNotif)
        assertEquals("✓ Secure message sent", firstNotif?.title)
        assertEquals(InAppNotificationType.SUCCESS, firstNotif?.type)

        // Posting the exact same message immediately must be dropped by anti-spam throttling
        inAppNotificationManager.showNotification(
            title = "✓ Secure message sent",
            type = InAppNotificationType.SUCCESS,
            durationMillis = 3500L
        )

        // Notification should remain the same instance, not re-triggered
        assertEquals(firstNotif?.id, inAppNotificationManager.currentNotification.value?.id)

        // Different message is allowed
        inAppNotificationManager.showNotification(
            title = "🔐 Access request received",
            type = InAppNotificationType.ACCESS,
            durationMillis = 3500L
        )

        val secondNotif = inAppNotificationManager.currentNotification.value
        assertNotNull(secondNotif)
        assertEquals("🔐 Access request received", secondNotif?.title)
        assertEquals(InAppNotificationType.ACCESS, secondNotif?.type)

        // Dismiss clearing
        inAppNotificationManager.dismiss()
        assertNull(inAppNotificationManager.currentNotification.value)
    }

    // --- Helper Models & Test Fakes ---

    private fun createSampleMessage(id: String, text: String, isExpired: Boolean): Message {
        return Message(
            id = id,
            conversationId = "conv_test",
            senderId = "user_sender",
            recipientId = "user_recipient",
            encryptedContentBase64 = "encrypted_base64_blob",
            encryptionMetadata = EncryptionMetadata(
                initializationVectorBase64 = "iv==",
                keyAlias = "alias"
            ),
            policy = SecureMessagePolicy(),
            status = if (isExpired) MessageStatus.EXPIRED else MessageStatus.ENCRYPTED_SENT,
            deliveryStatus = MessageDeliveryStatus.SENT,
            messageType = MessageType.TEXT,
            decryptedTextCache = text
        )
    }

    private class FakeMessageRepository : MessageRepository {
        val messages = mutableListOf<Message>()

        override suspend fun searchLocalMessages(query: String): Resource<List<Message>> {
            val q = query.trim().lowercase()
            val matches = messages.filter {
                !it.isExpiredOrRevoked && it.decryptedTextCache?.lowercase()?.contains(q) == true
            }
            return Resource.Success(matches)
        }

        override suspend fun deleteLocalMessage(messageId: String): Resource<Unit> {
            messages.removeAll { it.id == messageId }
            return Resource.Success(Unit)
        }

        override fun getMessages(conversationId: String): Flow<List<Message>> = flowOf(messages)
        override suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): Resource<List<Message>> = Resource.Success(messages)
        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String, policy: SecureMessagePolicy): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType, policy: SecureMessagePolicy): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun retryMessage(messageId: String): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun markMessageAsRead(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun decryptMessage(message: Message): Resource<String> = Resource.Success(message.decryptedTextCache ?: "")
        override suspend fun decryptAttachment(attachment: Attachment): Resource<File> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        override suspend fun revokeMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun purgeExpiredMessages(): Resource<Int> = Resource.Success(0)
        override suspend fun sendEncryptedMessage(message: Message): Resource<Message> = Resource.Success(message)
    }

    private class FakeChatRepository : ChatRepository {
        val conversations = mutableListOf<Conversation>()

        override fun getConversations(): Flow<List<Conversation>> = flowOf(conversations)

        override suspend fun getConversationById(conversationId: String): Resource<Conversation?> {
            val conv = conversations.find { it.id == conversationId }
            return Resource.Success(conv)
        }

        override suspend fun getOrCreateConversation(participantUserId: String): Resource<Conversation> {
            return Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))
        }

        override suspend fun deleteConversation(conversationId: String): Resource<Unit> {
            conversations.removeAll { it.id == conversationId }
            return Resource.Success(Unit)
        }

        override suspend fun searchConversations(query: String): Resource<List<Conversation>> {
            val q = query.trim().lowercase()
            val matches = conversations.filter {
                it.participantUser.fullName.lowercase().contains(q) ||
                        it.participantUser.username.lowercase().contains(q)
            }
            return Resource.Success(matches)
        }
    }
}
