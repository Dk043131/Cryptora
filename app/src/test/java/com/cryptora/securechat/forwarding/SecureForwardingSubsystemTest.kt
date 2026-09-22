package com.cryptora.securechat.forwarding

import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.dao.ForwardEventDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.entity.ConversationEntity
import com.cryptora.securechat.core.database.entity.ForwardEventEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.data.repository.ForwardingRepositoryImpl
import com.cryptora.securechat.domain.model.AccessGrant
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardEvent
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AccessValidationResult
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.ForwardingRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import com.cryptora.securechat.domain.usecase.forwarding.ForwardSecureMessageUseCase
import com.cryptora.securechat.domain.usecase.forwarding.GetForwardChainUseCase
import com.cryptora.securechat.domain.usecase.forwarding.RespondToForwardRequestUseCase
import com.cryptora.securechat.domain.usecase.forwarding.VerifyForwardChainIntegrityUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureForwardingSubsystemTest {

    private lateinit var fakeMessageDao: FakeMessageDao
    private lateinit var fakeForwardEventDao: FakeForwardEventDao
    private lateinit var fakeConversationDao: FakeConversationDao
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeSecureContentRepo: FakeSecureContentRepository
    private lateinit var authoritativeTimeManager: AuthoritativeTimeManager

    private lateinit var forwardingRepository: ForwardingRepository
    private lateinit var forwardSecureMessageUseCase: ForwardSecureMessageUseCase
    private lateinit var getForwardChainUseCase: GetForwardChainUseCase
    private lateinit var verifyForwardChainIntegrityUseCase: VerifyForwardChainIntegrityUseCase
    private lateinit var respondToForwardRequestUseCase: RespondToForwardRequestUseCase

    private val testDispatcher = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val userHarshanth = User(id = "user_harshanth", username = "harshanth", mobileNumber = "+919876543210", publicKey = "pk_harshanth", fullName = "Harshanth")
    private val userDeepak = User(id = "user_deepak", username = "deepak", mobileNumber = "+919876543211", publicKey = "pk_deepak", fullName = "Deepak")
    private val userKarthiga = User(id = "user_karthiga", username = "karthiga", mobileNumber = "+919876543212", publicKey = "pk_karthiga", fullName = "Karthiga")
    private val userSarumathy = User(id = "user_sarumathy", username = "sarumathy", mobileNumber = "+919876543213", publicKey = "pk_sarumathy", fullName = "Sarumathy")

    private val baseTime = 1_700_000_000_000L

    @Before
    fun setUp() {
        fakeMessageDao = FakeMessageDao()
        fakeForwardEventDao = FakeForwardEventDao()
        fakeConversationDao = FakeConversationDao()
        fakeAuthRepo = FakeAuthRepository(userHarshanth)
        fakeSecureContentRepo = FakeSecureContentRepository()
        authoritativeTimeManager = AuthoritativeTimeManager()
        authoritativeTimeManager.updateServerTime(baseTime)

        forwardingRepository = ForwardingRepositoryImpl(
            forwardEventDao = fakeForwardEventDao,
            messageDao = fakeMessageDao,
            conversationDao = fakeConversationDao,
            authRepository = fakeAuthRepo,
            secureContentRepository = fakeSecureContentRepo,
            timeManager = authoritativeTimeManager,
            dispatchers = testDispatcher
        )

        forwardSecureMessageUseCase = ForwardSecureMessageUseCase(forwardingRepository)
        getForwardChainUseCase = GetForwardChainUseCase(forwardingRepository)
        verifyForwardChainIntegrityUseCase = VerifyForwardChainIntegrityUseCase(forwardingRepository)
        respondToForwardRequestUseCase = RespondToForwardRequestUseCase(forwardingRepository)
    }

    @Test
    fun `test forwarding disabled returns error and prevents forwarding`() = runBlocking {
        // Message with FORWARDING_DISABLED
        val originalMessage = createTestMessage(
            id = "msg_disabled_1",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED,
            expiresAt = baseTime + 600_000L
        )
        fakeMessageDao.insertOrReplace(originalMessage)

        // Switch active user to Deepak
        fakeAuthRepo.activeUser = userDeepak

        val result = forwardSecureMessageUseCase(
            messageId = "msg_disabled_1",
            targetConversationId = "conv_deepak_karthiga",
            targetUserId = userKarthiga.id,
            targetUsername = userKarthiga.username
        )

        assertTrue(result is Resource.Error)
        val errorMsg = (result as Resource.Error).error.message
        assertTrue(errorMsg.contains("disabled", ignoreCase = true))

        // Ensure no forward event was stored
        assertEquals(0, fakeForwardEventDao.events.size)
    }

    @Test
    fun `test forwarding allowed propagates hash chain E1 to E2 to E3 correctly`() = runBlocking {
        val rootExpiresAt = baseTime + 500_000L

        // 1. Root message
        val rootMsg = createTestMessage(
            id = "root_msg_chain",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.FORWARDING_ALLOWED,
            expiresAt = rootExpiresAt
        )
        fakeMessageDao.insertOrReplace(rootMsg)

        // Hop 1: Harshanth forwards to Deepak
        fakeAuthRepo.activeUser = userHarshanth
        val hop1Res = forwardSecureMessageUseCase(
            messageId = "root_msg_chain",
            targetConversationId = "conv_harshanth_deepak",
            targetUserId = userDeepak.id,
            targetUsername = userDeepak.username
        )
        assertTrue("Hop 1 should succeed: ${(hop1Res as? Resource.Error)?.error?.message}", hop1Res is Resource.Success)
        val event1 = (hop1Res as Resource.Success).data
        assertEquals(ForwardEvent.GENESIS_HASH, event1.previousEventHash)
        assertNull(event1.parentEventId)
        assertEquals(userHarshanth.id, event1.fromUserId)
        assertEquals(userDeepak.id, event1.toUserId)
        assertEquals(ForwardApprovalStatus.APPROVED, event1.approvalStatus)

        // Hop 2: Deepak forwards to Karthiga
        authoritativeTimeManager.updateServerTime(baseTime + 1000L)
        fakeAuthRepo.activeUser = userDeepak
        val hop2Res = forwardSecureMessageUseCase(
            messageId = event1.secureMessageId,
            targetConversationId = "conv_deepak_karthiga",
            targetUserId = userKarthiga.id,
            targetUsername = userKarthiga.username
        )
        assertTrue("Hop 2 should succeed: ${(hop2Res as? Resource.Error)?.error?.message}", hop2Res is Resource.Success)
        val event2 = (hop2Res as Resource.Success).data
        assertEquals(event1.eventId, event2.parentEventId)
        assertEquals(event1.eventHash, event2.previousEventHash)
        assertEquals(userDeepak.id, event2.fromUserId)
        assertEquals(userKarthiga.id, event2.toUserId)

        // Hop 3: Karthiga forwards to Sarumathy
        authoritativeTimeManager.updateServerTime(baseTime + 2000L)
        fakeAuthRepo.activeUser = userKarthiga
        val hop3Res = forwardSecureMessageUseCase(
            messageId = event2.secureMessageId,
            targetConversationId = "conv_karthiga_sarumathy",
            targetUserId = userSarumathy.id,
            targetUsername = userSarumathy.username
        )
        assertTrue("Hop 3 should succeed: ${(hop3Res as? Resource.Error)?.error?.message}", hop3Res is Resource.Success)
        val event3 = (hop3Res as Resource.Success).data
        assertEquals(event2.eventId, event3.parentEventId)
        assertEquals(event2.eventHash, event3.previousEventHash)
        assertEquals(userKarthiga.id, event3.fromUserId)
        assertEquals(userSarumathy.id, event3.toUserId)

        // Verify full chain integrity passes
        val integrityResult = verifyForwardChainIntegrityUseCase("root_msg_chain")
        assertTrue(integrityResult is Resource.Success)
        val integrity = (integrityResult as Resource.Success).data
        assertTrue(integrity.isVerified)
        assertEquals(3, integrity.chainDepth)
        assertNull(integrity.brokenAtEventId)
    }

    @Test
    fun `test strict expiry inheritance ceiling across hops`() = runBlocking {
        val rootExpiresAt = baseTime + 60_000L // 60 seconds from now

        val rootMsg = createTestMessage(
            id = "msg_ceiling_test",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.FORWARDING_ALLOWED,
            expiresAt = rootExpiresAt
        )
        fakeMessageDao.insertOrReplace(rootMsg)

        // Forward to Karthiga
        fakeAuthRepo.activeUser = userHarshanth
        val hop1Res = forwardSecureMessageUseCase(
            messageId = "msg_ceiling_test",
            targetConversationId = "conv_harshanth_karthiga",
            targetUserId = userKarthiga.id,
            targetUsername = userKarthiga.username
        )
        assertTrue(hop1Res is Resource.Success)
        val event1 = (hop1Res as Resource.Success).data

        // Forwarded message in DB must have identical expiresAt
        val fwdMessage1 = fakeMessageDao.getMessageById(event1.secureMessageId)
        assertNotNull(fwdMessage1)
        assertEquals(rootExpiresAt, fwdMessage1!!.expiresAt)

        // Advance authoritative time past expiry
        authoritativeTimeManager.updateServerTime(rootExpiresAt + 10_000L)

        // Attempting to forward an expired message must fail
        fakeAuthRepo.activeUser = userKarthiga
        val expiredForwardRes = forwardSecureMessageUseCase(
            messageId = event1.secureMessageId,
            targetConversationId = "conv_karthiga_sarumathy",
            targetUserId = userSarumathy.id,
            targetUsername = userSarumathy.username
        )
        assertTrue(expiredForwardRes is Resource.Error)
        val errorMsg = (expiredForwardRes as Resource.Error).error.message
        assertTrue(errorMsg.contains("expired", ignoreCase = true))
    }

    @Test
    fun `test cryptographic tamper detection detects modified event in chain`() = runBlocking {
        val rootMsg = createTestMessage(
            id = "root_tamper_test",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.FORWARDING_ALLOWED,
            expiresAt = baseTime + 500_000L
        )
        fakeMessageDao.insertOrReplace(rootMsg)

        // Hop 1
        fakeAuthRepo.activeUser = userHarshanth
        val hop1 = forwardSecureMessageUseCase(
            messageId = "root_tamper_test",
            targetConversationId = "conv_1",
            targetUserId = userDeepak.id,
            targetUsername = userDeepak.username
        )
        val event1 = (hop1 as Resource.Success).data

        // Hop 2
        authoritativeTimeManager.updateServerTime(baseTime + 1000L)
        fakeAuthRepo.activeUser = userDeepak
        val hop2 = forwardSecureMessageUseCase(
            messageId = event1.secureMessageId,
            targetConversationId = "conv_2",
            targetUserId = userKarthiga.id,
            targetUsername = userKarthiga.username
        )
        assertTrue(hop2 is Resource.Success)

        // Initial check: unbroken chain is valid
        val validCheck = verifyForwardChainIntegrityUseCase("root_tamper_test")
        assertTrue((validCheck as Resource.Success).data.isVerified)

        // Tamper with Hop 1's recipient in the DB
        val hop1EventEntity = fakeForwardEventDao.events[event1.eventId]!!
        fakeForwardEventDao.events[event1.eventId] = hop1EventEntity.copy(toUserId = "tampered_attacker")

        // Run integrity verification: Tampering MUST be detected!
        val tamperedCheck = verifyForwardChainIntegrityUseCase("root_tamper_test")
        assertTrue(tamperedCheck is Resource.Success)
        val integrity = (tamperedCheck as Resource.Success).data
        assertFalse("Tampered chain must not be verified", integrity.isVerified)
        assertEquals(event1.eventId, integrity.brokenAtEventId)
        assertTrue(integrity.validationMessage.contains("Tampered payload", ignoreCase = true))
    }

    @Test
    fun `test forward approval required workflow`() = runBlocking {
        val rootExpiresAt = baseTime + 600_000L

        // Message requiring original sender approval
        val rootMsg = createTestMessage(
            id = "approval_msg",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL,
            expiresAt = rootExpiresAt
        )
        fakeMessageDao.insertOrReplace(rootMsg)

        // Deepak forwards to Karthiga
        fakeAuthRepo.activeUser = userDeepak
        val fwdResult = forwardSecureMessageUseCase(
            messageId = "approval_msg",
            targetConversationId = "conv_deepak_karthiga",
            targetUserId = userKarthiga.id,
            targetUsername = userKarthiga.username
        )
        assertTrue(fwdResult is Resource.Success)
        val forwardedEvent = (fwdResult as Resource.Success).data

        // Event status should be PENDING
        assertEquals(ForwardApprovalStatus.PENDING, forwardedEvent.approvalStatus)

        // Forwarded message in DB must have isAccessGranted = false
        val storedFwdMsg = fakeMessageDao.getMessageById(forwardedEvent.secureMessageId)!!
        assertFalse("Forward requiring approval must have isAccessGranted=false", storedFwdMsg.isAccessGranted)

        // Original sender approves
        val approveResult = respondToForwardRequestUseCase(forwardedEvent.eventId, approved = true)
        assertTrue(approveResult is Resource.Success)

        // Stored event is now APPROVED
        val updatedEvent = fakeForwardEventDao.getEventById(forwardedEvent.eventId)!!
        assertEquals(ForwardApprovalStatus.APPROVED.name, updatedEvent.approvalStatus)

        // Message is unlocked
        val updatedMsg = fakeMessageDao.getMessageById(forwardedEvent.secureMessageId)!!
        assertTrue(updatedMsg.isAccessGranted)
        assertEquals(MessageDeliveryStatus.DELIVERED.name, updatedMsg.deliveryStatus)
    }

    @Test
    fun `test privacy protection - phone numbers not present in forward events`() = runBlocking {
        val rootMsg = createTestMessage(
            id = "privacy_test_msg",
            senderId = userHarshanth.id,
            recipientId = userDeepak.id,
            forwardingPolicy = ForwardingPolicy.FORWARDING_ALLOWED,
            expiresAt = baseTime + 600_000L
        )
        fakeMessageDao.insertOrReplace(rootMsg)

        fakeAuthRepo.activeUser = userHarshanth
        val fwd = forwardSecureMessageUseCase(
            messageId = "privacy_test_msg",
            targetConversationId = "conv_h_d",
            targetUserId = userDeepak.id,
            targetUsername = userDeepak.username
        )
        assertTrue(fwd is Resource.Success)

        val chain = fakeForwardEventDao.getEventsForRoot("privacy_test_msg")
        assertEquals(1, chain.size)
        val event = chain[0]

        // Only usernames and IDs are stored; no phone numbers in forward chain
        assertFalse(event.fromUsername.contains("+91"))
        assertFalse(event.toUsername.contains("+91"))
        assertEquals("harshanth", event.fromUsername)
        assertEquals("deepak", event.toUsername)
    }

    private fun createTestMessage(
        id: String,
        senderId: String,
        recipientId: String,
        forwardingPolicy: ForwardingPolicy,
        expiresAt: Long
    ): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = "conv_${senderId}_${recipientId}",
            senderId = senderId,
            recipientId = recipientId,
            encryptedContentBase64 = "encrypted_secret_data",
            keyAlias = "test_key",
            ivBase64 = "test_iv",
            messageType = "TEXT",
            deliveryStatus = "DELIVERED",
            isOutgoing = false,
            decryptedTextCache = "Secret message payload",
            timestamp = baseTime,
            hasAttachment = false,
            accessMode = "IMMEDIATE_ACCESS",
            expiresAt = expiresAt,
            forwardingPolicy = forwardingPolicy.name,
            approvalRequired = (forwardingPolicy == ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL),
            policyOwnerId = senderId,
            policyCreatedAt = baseTime,
            policyVersion = 1,
            isAccessGranted = true,
            rootMessageId = id,
            forwardCount = 0,
            isForwarded = false,
            originalSenderUsername = userHarshanth.username
        )
    }

    // --- Fakes ---

    private class FakeMessageDao : MessageDao {
        val messages = mutableMapOf<String, MessageEntity>()
        private val listFlow = MutableStateFlow<List<MessageEntity>>(emptyList())

        override fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> =
            flowOf(messages.values.filter { it.conversationId == conversationId }.sortedBy { it.timestamp })

        override suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): List<MessageEntity> =
            messages.values.filter { it.conversationId == conversationId }.drop(offset).take(limit)

        override suspend fun getMessageById(id: String): MessageEntity? = messages[id]

        override suspend fun insertOrReplace(message: MessageEntity) {
            messages[message.id] = message
            listFlow.value = messages.values.toList()
        }

        override suspend fun insertAll(messages: List<MessageEntity>) {
            messages.forEach { this.messages[it.id] = it }
            listFlow.value = this.messages.values.toList()
        }

        override suspend fun updateDeliveryStatus(messageId: String, status: String) {
            messages[messageId]?.let { messages[messageId] = it.copy(deliveryStatus = status) }
        }

        override suspend fun updateDecryptedCache(messageId: String, decryptedText: String) {
            messages[messageId]?.let { messages[messageId] = it.copy(decryptedTextCache = decryptedText) }
        }

        override suspend fun deleteMessage(id: String) {
            messages.remove(id)
            listFlow.value = messages.values.toList()
        }

        override suspend fun getFailedMessages(conversationId: String): List<MessageEntity> =
            messages.values.filter { it.conversationId == conversationId && it.deliveryStatus == "FAILED" }

        override suspend fun getExpiredCachedMessages(currentTime: Long): List<MessageEntity> =
            messages.values.filter { it.expiresAt != null && it.expiresAt <= currentTime && !it.decryptedTextCache.isNullEmpty() }

        override suspend fun shredExpiredDecryptedCaches(currentTime: Long): Int {
            var count = 0
            messages.values.forEach { entity ->
                if (entity.expiresAt != null && entity.expiresAt <= currentTime && !entity.decryptedTextCache.isNullEmpty()) {
                    messages[entity.id] = entity.copy(decryptedTextCache = "")
                    count++
                }
            }
            return count
        }

        override suspend fun searchLocalMessages(query: String, currentTime: Long): List<MessageEntity> {
            return messages.values.filter {
                (it.expiresAt == null || it.expiresAt > currentTime) &&
                        it.decryptedTextCache?.contains(query, ignoreCase = true) == true
            }
        }

        private fun String?.isNullEmpty(): Boolean = this == null || this.isEmpty()
    }

    private class FakeForwardEventDao : ForwardEventDao {
        val events = mutableMapOf<String, ForwardEventEntity>()

        override fun getEventsForRootFlow(rootMessageId: String): Flow<List<ForwardEventEntity>> =
            flowOf(events.values.filter { it.rootMessageId == rootMessageId }.sortedBy { it.timestamp })

        override suspend fun getEventsForRoot(rootMessageId: String): List<ForwardEventEntity> =
            events.values.filter { it.rootMessageId == rootMessageId }.sortedBy { it.timestamp }

        override suspend fun getEventsForMessage(secureMessageId: String): List<ForwardEventEntity> =
            events.values.filter { it.secureMessageId == secureMessageId }.sortedBy { it.timestamp }

        override suspend fun getEventById(eventId: String): ForwardEventEntity? = events[eventId]

        override suspend fun getLatestEventForRoot(rootMessageId: String): ForwardEventEntity? =
            events.values.filter { it.rootMessageId == rootMessageId }.maxByOrNull { it.timestamp }

        override suspend fun getApprovedHopCount(rootMessageId: String): Int =
            events.values.count { it.rootMessageId == rootMessageId && it.approvalStatus == ForwardApprovalStatus.APPROVED.name }

        override suspend fun insertEvent(event: ForwardEventEntity) {
            events[event.eventId] = event
        }

        override suspend fun updateStatus(eventId: String, status: String) {
            events[eventId]?.let { events[eventId] = it.copy(approvalStatus = status) }
        }

        override suspend fun deleteEventsForRoot(rootMessageId: String) {
            val toRemove = events.values.filter { it.rootMessageId == rootMessageId }.map { it.eventId }
            toRemove.forEach { events.remove(it) }
        }
    }

    private class FakeConversationDao : ConversationDao {
        val conversations = mutableMapOf<String, ConversationEntity>()

        override fun getConversationsFlow(): Flow<List<ConversationEntity>> =
            flowOf(conversations.values.sortedByDescending { it.updatedAt })

        override suspend fun getConversationById(id: String): ConversationEntity? = conversations[id]

        override suspend fun getConversationByParticipant(participantUserId: String): ConversationEntity? =
            conversations.values.firstOrNull { it.participantUserId == participantUserId }

        override suspend fun insertOrUpdate(conversation: ConversationEntity) {
            conversations[conversation.id] = conversation
        }

        override suspend fun deleteConversation(id: String) {
            conversations.remove(id)
        }

        override suspend fun searchConversations(query: String): List<ConversationEntity> {
            return conversations.values.filter {
                it.participantUsername.contains(query, ignoreCase = true) ||
                        it.lastMessageText?.contains(query, ignoreCase = true) == true
            }
        }
    }

    private class FakeAuthRepository(var activeUser: User) : AuthRepository {
        override fun getAuthenticatedUser(): Flow<User?> = flowOf(activeUser)
        override suspend fun checkUsernameAvailability(username: String): Resource<Boolean> = Resource.Success(true)
        override suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String> = Resource.Success("123456")
        override suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean> = Resource.Success(true)
        override suspend fun registerUser(fullName: String, username: String, mobileNumber: String, password: String, avatarUrl: String?, bio: String): Resource<User> = Resource.Success(activeUser)
        override suspend fun loginUser(username: String, password: String): Resource<User> = Resource.Success(activeUser)
        override suspend fun restoreSession(): Resource<User?> = Resource.Success(activeUser)
        override suspend fun logout(): Resource<Unit> = Resource.Success(Unit)
    }

    private class FakeSecureContentRepository : SecureContentRepository {
        val emittedNotifications = mutableListOf<AccessNotificationEvent>()
        private val notificationsFlow = MutableSharedFlow<AccessNotificationEvent>(extraBufferCapacity = 10)

        override suspend fun validateMessageAccess(messageId: String): Resource<AccessValidationResult> =
            Resource.Success(AccessValidationResult(messageId, isAuthorized = true, isExpired = false, remainingTimeMillis = 300000L, accessMode = "IMMEDIATE_ACCESS"))

        override suspend fun requestAccess(messageId: String, requestedDuration: Long, contentTitle: String): Resource<AccessRequest> =
            Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))

        override suspend fun respondToAccessRequest(requestId: String, approved: Boolean, finalDurationMillis: Long?): Resource<AccessGrant?> =
            Resource.Success(null)

        override suspend fun cancelAccessRequest(requestId: String): Resource<Unit> = Resource.Success(Unit)

        override fun getPendingAccessRequests(): Flow<List<AccessRequest>> = flowOf(emptyList())

        override fun getAllAccessRequests(): Flow<List<AccessRequest>> = flowOf(emptyList())

        override fun getAccessRequestForMessage(messageId: String): Flow<AccessRequest?> = flowOf(null)

        override suspend fun getActiveGrant(messageId: String): Resource<AccessGrant?> = Resource.Success(null)

        override suspend fun syncAuthoritativeExpiry(): Resource<Int> = Resource.Success(0)

        override suspend fun revokeMessageAccess(messageId: String): Resource<Unit> = Resource.Success(Unit)

        override suspend fun updateMessagePolicy(messageId: String, policy: SecureMessagePolicy): Resource<Unit> = Resource.Success(Unit)

        override fun getAuthoritativeServerTime(): Long = System.currentTimeMillis()

        override fun observeAccessNotifications(): Flow<AccessNotificationEvent> = notificationsFlow.asSharedFlow()

        override suspend fun emitNotification(event: AccessNotificationEvent) {
            emittedNotifications.add(event)
            notificationsFlow.emit(event)
        }
    }
}
