package com.cryptora.securechat.secure

import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.AccessGrantDao
import com.cryptora.securechat.core.database.dao.AccessRequestDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.dao.NoteDao
import com.cryptora.securechat.core.database.entity.AccessGrantEntity
import com.cryptora.securechat.core.database.entity.AccessRequestEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.database.entity.NoteEntity
import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.CryptoManagerImpl
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.KeyManagerImpl
import com.cryptora.securechat.data.notes.NoteRepositoryImpl
import com.cryptora.securechat.data.remote.SecureContentApi
import com.cryptora.securechat.data.remote.SecureContentApiImpl
import com.cryptora.securechat.data.remote.ServerPolicyMetadata
import com.cryptora.securechat.data.repository.SecureContentRepositoryImpl
import com.cryptora.securechat.domain.model.AccessGrant
import com.cryptora.securechat.domain.model.AccessMode
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequestStatus
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.MessageRepository
import com.cryptora.securechat.domain.usecase.notes.SendNoteSecurely
import com.cryptora.securechat.domain.usecase.secure.CancelAccessRequestUseCase
import com.cryptora.securechat.domain.usecase.secure.GetAllAccessRequestsUseCase
import com.cryptora.securechat.domain.usecase.secure.GetPendingAccessRequestsUseCase
import com.cryptora.securechat.domain.usecase.secure.RequestAccessUseCase
import com.cryptora.securechat.domain.usecase.secure.RespondToAccessRequestUseCase
import com.cryptora.securechat.domain.usecase.secure.ValidateAccessUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SecureContentSubsystemTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cryptoManager: CryptoManager
    private lateinit var fakeMessageDao: FakeMessageDao
    private lateinit var fakeAccessRequestDao: FakeAccessRequestDao
    private lateinit var fakeAccessGrantDao: FakeAccessGrantDao
    private lateinit var secureContentApi: SecureContentApi
    private lateinit var timeManager: AuthoritativeTimeManager
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var secureContentRepository: SecureContentRepositoryImpl

    private lateinit var validateAccessUseCase: ValidateAccessUseCase
    private lateinit var requestAccessUseCase: RequestAccessUseCase
    private lateinit var respondToAccessRequestUseCase: RespondToAccessRequestUseCase
    private lateinit var cancelAccessRequestUseCase: CancelAccessRequestUseCase
    private lateinit var getPendingAccessRequestsUseCase: GetPendingAccessRequestsUseCase
    private lateinit var getAllAccessRequestsUseCase: GetAllAccessRequestsUseCase

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        keyManager = KeyManagerImpl()
        cryptoManager = CryptoManagerImpl(keyManager)
        fakeMessageDao = FakeMessageDao()
        fakeAccessRequestDao = FakeAccessRequestDao()
        fakeAccessGrantDao = FakeAccessGrantDao()
        timeManager = AuthoritativeTimeManager()
        secureContentApi = SecureContentApiImpl(timeManager)
        fakeAuthRepository = FakeAuthRepository(
            activeUser = User(
                id = "user_alice",
                username = "alice",
                mobileNumber = "+1234567890",
                publicKey = "pk_alice",
                fullName = "Alice Liddell"
            )
        )

        secureContentRepository = SecureContentRepositoryImpl(
            messageDao = fakeMessageDao,
            accessRequestDao = fakeAccessRequestDao,
            accessGrantDao = fakeAccessGrantDao,
            secureContentApi = secureContentApi,
            timeManager = timeManager,
            authRepository = fakeAuthRepository,
            dispatchers = testDispatchers
        )

        validateAccessUseCase = ValidateAccessUseCase(secureContentRepository)
        requestAccessUseCase = RequestAccessUseCase(secureContentRepository)
        respondToAccessRequestUseCase = RespondToAccessRequestUseCase(secureContentRepository)
        cancelAccessRequestUseCase = CancelAccessRequestUseCase(secureContentRepository)
        getPendingAccessRequestsUseCase = GetPendingAccessRequestsUseCase(secureContentRepository)
        getAllAccessRequestsUseCase = GetAllAccessRequestsUseCase(secureContentRepository)
    }

    @Test
    fun testAuthoritativeTimeManager_preventsClockRollback() {
        val serverTimeNow = 1700000000000L
        timeManager.updateServerTime(serverTimeNow)

        val authoritativeFirst = timeManager.getAuthoritativeTime()
        assertTrue(authoritativeFirst >= serverTimeNow)

        val authoritativeSecond = timeManager.getAuthoritativeTime()
        assertTrue(authoritativeSecond >= authoritativeFirst)
    }

    @Test
    fun testValidateMessageAccess_authorizedWhenNotExpired() = runBlocking {
        val messageId = "msg_timelock_001"
        val now = System.currentTimeMillis()
        val expiryTime = now + 300_000L // 5 minutes in future

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                expiresAt = expiryTime,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = false,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(
                id = messageId,
                expiresAt = expiryTime,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                cachedText = "Confidential project credentials"
            )
        )

        val result = validateAccessUseCase(messageId)
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertNotNull(data)
        assertTrue(data!!.isAuthorized)
        assertFalse(data.isExpired)
        assertTrue(data.remainingTimeMillis > 0)
    }

    @Test
    fun testValidateMessageAccess_expiredContent_cryptoShredsLocalCache() = runBlocking {
        val messageId = "msg_expired_001"
        val pastTime = 1600000000000L
        val expiredAt = pastTime + 30_000L

        timeManager.updateServerTime(expiredAt + 100_000L)

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                expiresAt = expiredAt,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = false,
                ownerId = "user_alice",
                createdAt = pastTime
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(
                id = messageId,
                expiresAt = expiredAt,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                cachedText = "Secret Password 12345"
            )
        )

        val result = validateAccessUseCase(messageId)
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertNotNull(data)
        assertFalse(data!!.isAuthorized)
        assertTrue(data.isExpired)
        assertEquals(0L, data.remainingTimeMillis)

        val entityAfterValidation = fakeMessageDao.getMessageById(messageId)
        assertNotNull(entityAfterValidation)
        assertEquals("", entityAfterValidation!!.decryptedTextCache)
    }

    @Test
    fun testRequestAccessMode_lifecycle() = runBlocking {
        val messageId = "msg_request_access_001"
        val now = System.currentTimeMillis()

        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.REQUEST_ACCESS.name,
                expiresAt = now + 3600_000L,
                forwardingPolicy = ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL.name,
                approvalRequired = true,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(
                id = messageId,
                accessMode = AccessMode.REQUEST_ACCESS.name,
                isAccessGranted = false,
                cachedText = ""
            )
        )

        val initialValidation = validateAccessUseCase(messageId)
        assertTrue(initialValidation is Resource.Success)
        assertFalse((initialValidation as Resource.Success).data!!.isAuthorized)

        val requestResult = requestAccessUseCase(
            messageId = messageId,
            requestedDuration = 30L * 60L * 1000L,
            contentTitle = "Project Credentials"
        )
        assertTrue(requestResult is Resource.Success)
        val accessReq = (requestResult as Resource.Success).data
        assertNotNull(accessReq)
        assertEquals(AccessRequestStatus.PENDING, accessReq!!.status)
        assertEquals("Project Credentials", accessReq.contentTitle)
        assertEquals(30L * 60L * 1000L, accessReq.requestedDuration)

        val pendingList = getPendingAccessRequestsUseCase().first()
        assertTrue(pendingList.any { it.messageId == messageId })

        // Switch active user to Alice (owner) to approve request
        fakeAuthRepository.activeUser = User(
            id = "user_alice",
            username = "alice",
            mobileNumber = "+1234567890",
            publicKey = "pk_alice",
            fullName = "Alice Liddell"
        )

        val approveResult = respondToAccessRequestUseCase(accessReq.requestId, approved = true, finalDurationMillis = 15L * 60L * 1000L)
        assertTrue(approveResult is Resource.Success)
        val grant = (approveResult as Resource.Success).data
        assertNotNull(grant)
        assertEquals(15L * 60L * 1000L, grant!!.grantedDuration)

        // Switch back to Bob (recipient) to validate access
        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        val postApprovalValidation = validateAccessUseCase(messageId)
        assertTrue(postApprovalValidation is Resource.Success)
        val valResult = (postApprovalValidation as Resource.Success).data!!
        assertTrue(valResult.isAuthorized)
        assertNotNull(valResult.activeGrant)
    }

    @Test
    fun testAccessRequest_policyCeiling_receiverCannotExceedSenderMax() = runBlocking {
        val messageId = "msg_ceiling_001"
        val now = System.currentTimeMillis()
        val maxCeilingTime = now + (10L * 60L * 1000L) // 10 minutes maximum

        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.REQUEST_ACCESS.name,
                expiresAt = maxCeilingTime,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = true,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(id = messageId, accessMode = AccessMode.REQUEST_ACCESS.name, isAccessGranted = false)
        )

        val reqResult = requestAccessUseCase(messageId, requestedDuration = 24L * 60L * 60L * 1000L)
        val reqId = (reqResult as Resource.Success).data!!.requestId

        fakeAuthRepository.activeUser = User(
            id = "user_alice",
            username = "alice",
            mobileNumber = "+1234567890",
            publicKey = "pk_alice",
            fullName = "Alice Liddell"
        )

        val approveResult = respondToAccessRequestUseCase(reqId, approved = true, finalDurationMillis = 24L * 60L * 60L * 1000L)
        assertTrue(approveResult is Resource.Success)
        val grant = (approveResult as Resource.Success).data
        assertNotNull(grant)

        assertTrue(grant!!.expiresAt <= maxCeilingTime)
    }

    @Test
    fun testCancelAccessRequest_transition() = runBlocking {
        val messageId = "msg_cancel_001"
        val now = System.currentTimeMillis()

        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.REQUEST_ACCESS.name,
                expiresAt = now + 3600_000L,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = true,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(id = messageId, accessMode = AccessMode.REQUEST_ACCESS.name, isAccessGranted = false)
        )

        val reqResult = requestAccessUseCase(messageId)
        val reqId = (reqResult as Resource.Success).data!!.requestId

        val cancelResult = cancelAccessRequestUseCase(reqId)
        assertTrue(cancelResult is Resource.Success)

        val cancelledEntity = fakeAccessRequestDao.getRequestById(reqId)
        assertNotNull(cancelledEntity)
        assertEquals("CANCELLED", cancelledEntity!!.status)

        val validation = validateAccessUseCase(messageId)
        assertFalse((validation as Resource.Success).data!!.isAuthorized)
    }

    @Test
    fun testRejectAccessRequest() = runBlocking {
        val messageId = "msg_reject_001"
        val now = System.currentTimeMillis()

        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.REQUEST_ACCESS.name,
                expiresAt = now + 3600_000L,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = true,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(id = messageId, accessMode = AccessMode.REQUEST_ACCESS.name, isAccessGranted = false)
        )

        val reqResult = requestAccessUseCase(messageId)
        val reqId = (reqResult as Resource.Success).data!!.requestId

        fakeAuthRepository.activeUser = User(
            id = "user_alice",
            username = "alice",
            mobileNumber = "+1234567890",
            publicKey = "pk_alice",
            fullName = "Alice Liddell"
        )

        val rejectResult = respondToAccessRequestUseCase(reqId, approved = false)
        assertTrue(rejectResult is Resource.Success)

        fakeAuthRepository.activeUser = User(
            id = "user_bob",
            username = "bob",
            mobileNumber = "+9876543210",
            publicKey = "pk_bob",
            fullName = "Bob Builder"
        )

        val validation = validateAccessUseCase(messageId)
        assertFalse((validation as Resource.Success).data!!.isAuthorized)
    }

    @Test
    fun testRevokeMessageAccess_shredsDecryptedCacheImmediately() = runBlocking {
        val messageId = "msg_revoke_001"
        val now = System.currentTimeMillis()

        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                expiresAt = now + 3600_000L,
                forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED.name,
                approvalRequired = false,
                ownerId = "user_alice",
                createdAt = now
            )
        )

        fakeMessageDao.insertOrReplace(
            createMessageEntity(
                id = messageId,
                expiresAt = now + 3600_000L,
                accessMode = AccessMode.IMMEDIATE_ACCESS.name,
                cachedText = "Secret Financial Records"
            )
        )

        val revokeResult = secureContentRepository.revokeMessageAccess(messageId)
        assertTrue(revokeResult is Resource.Success)

        val msgAfterRevoke = fakeMessageDao.getMessageById(messageId)
        assertEquals("", msgAfterRevoke!!.decryptedTextCache)

        val validation = validateAccessUseCase(messageId)
        assertFalse((validation as Resource.Success).data!!.isAuthorized)
    }

    @Test
    fun testSendNoteSecurely_endToEndEncrypted() = runBlocking {
        val noteDao = FakeNoteDao()
        val fakeMessageRepo = FakeTestMessageRepository()
        val noteRepo = NoteRepositoryImpl(
            noteDao = noteDao,
            cryptoManager = cryptoManager,
            messageRepository = fakeMessageRepo,
            dispatchers = testDispatchers
        )

        val createdNoteRes = noteRepo.createNote("Server Root Credentials", "root:SuperSecretPassword!2024")
        assertTrue(createdNoteRes is Resource.Success)
        val createdNote = (createdNoteRes as Resource.Success).data!!

        val sendNoteSecurely = SendNoteSecurely(
            noteRepository = noteRepo
        )

        val policy = SecureMessagePolicy(
            accessMode = AccessMode.REQUEST_ACCESS,
            expiryDurationMillis = 600_000L,
            forwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED,
            approvalRequired = true
        )

        val result = sendNoteSecurely(
            noteId = createdNote.id,
            conversationId = "conv_alice_bob",
            recipientId = "user_bob",
            policy = policy
        )

        assertTrue(result is Resource.Success)
        val sentMessage = fakeMessageRepo.lastSentMessage
        assertNotNull(sentMessage)
        assertEquals("conv_alice_bob", sentMessage!!.conversationId)
        assertEquals("user_bob", sentMessage.recipientId)
        assertEquals(AccessMode.REQUEST_ACCESS, sentMessage.policy.accessMode)
        assertTrue(sentMessage.policy.approvalRequired)
        assertTrue(sentMessage.decryptedTextCache!!.contains("Server Root Credentials"))
    }

    private fun createMessageEntity(
        id: String,
        expiresAt: Long? = null,
        accessMode: String = "IMMEDIATE_ACCESS",
        isAccessGranted: Boolean = true,
        cachedText: String = ""
    ): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = "conv_test",
            senderId = "user_alice",
            recipientId = "user_bob",
            encryptedContentBase64 = "cipher_data",
            keyAlias = "alias_data",
            ivBase64 = "iv_data",
            messageType = "TEXT",
            deliveryStatus = "SENT",
            isOutgoing = true,
            decryptedTextCache = cachedText,
            timestamp = System.currentTimeMillis(),
            hasAttachment = false,
            accessMode = accessMode,
            expiresAt = expiresAt,
            forwardingPolicy = "FORWARDING_DISABLED",
            approvalRequired = (accessMode == "REQUEST_ACCESS"),
            isAccessGranted = isAccessGranted
        )
    }

    private class FakeMessageDao : MessageDao {
        val messages = mutableMapOf<String, MessageEntity>()
        private val flow = MutableStateFlow<List<MessageEntity>>(emptyList())

        override fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> = flow.asStateFlow()
        override suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): List<MessageEntity> = messages.values.toList()
        override suspend fun getMessageById(id: String): MessageEntity? = messages[id]

        override suspend fun insertOrReplace(message: MessageEntity) {
            messages[message.id] = message
            flow.value = messages.values.toList()
        }

        override suspend fun insertAll(msgs: List<MessageEntity>) {
            msgs.forEach { messages[it.id] = it }
            flow.value = messages.values.toList()
        }

        override suspend fun updateDeliveryStatus(messageId: String, status: String) {
            messages[messageId]?.let {
                messages[messageId] = it.copy(deliveryStatus = status)
                flow.value = messages.values.toList()
            }
        }

        override suspend fun updateDecryptedCache(messageId: String, decryptedText: String) {
            messages[messageId]?.let {
                messages[messageId] = it.copy(decryptedTextCache = decryptedText)
                flow.value = messages.values.toList()
            }
        }

        override suspend fun deleteMessage(id: String) {
            messages.remove(id)
            flow.value = messages.values.toList()
        }

        override suspend fun getFailedMessages(conversationId: String): List<MessageEntity> {
            return messages.values.filter { it.conversationId == conversationId && it.deliveryStatus == "FAILED" }
        }

        override suspend fun getExpiredCachedMessages(currentTime: Long): List<MessageEntity> {
            return messages.values.filter { it.expiresAt != null && it.expiresAt <= currentTime && it.decryptedTextCache != "" }
        }

        override suspend fun shredExpiredDecryptedCaches(currentTime: Long): Int {
            var count = 0
            messages.values.forEach { entity ->
                if (entity.expiresAt != null && entity.expiresAt <= currentTime && !entity.decryptedTextCache.isNullOrEmpty()) {
                    messages[entity.id] = entity.copy(decryptedTextCache = "")
                    count++
                }
            }
            flow.value = messages.values.toList()
            return count
        }

        override suspend fun searchLocalMessages(query: String, currentTime: Long): List<MessageEntity> {
            return messages.values.filter {
                (it.expiresAt == null || it.expiresAt > currentTime) &&
                        it.decryptedTextCache?.contains(query, ignoreCase = true) == true
            }
        }
    }

    private class FakeAccessRequestDao : AccessRequestDao {
        val requests = mutableMapOf<String, AccessRequestEntity>()
        private val flow = MutableStateFlow<List<AccessRequestEntity>>(emptyList())
        private val allFlow = MutableStateFlow<List<AccessRequestEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = requests.values.filter { it.status == "PENDING" }
            allFlow.value = requests.values.toList()
        }

        override suspend fun insertOrUpdate(request: AccessRequestEntity) {
            requests[request.requestId] = request
            updateFlow()
        }

        override suspend fun getRequestById(requestId: String): AccessRequestEntity? = requests[requestId]

        override suspend fun getRequestByMessageId(messageId: String): AccessRequestEntity? =
            requests.values.firstOrNull { it.messageId == messageId }

        override fun getPendingRequestsFlow(): Flow<List<AccessRequestEntity>> = flow.asStateFlow()

        override fun getAllRequestsFlow(): Flow<List<AccessRequestEntity>> = allFlow.asStateFlow()

        override fun getLatestRequestFlowForMessage(messageId: String): Flow<AccessRequestEntity?> =
            flowOf(requests.values.filter { it.messageId == messageId }.maxByOrNull { it.requestedAt })

        override suspend fun updateStatus(requestId: String, status: String, respondedAt: Long) {
            requests[requestId]?.let {
                requests[requestId] = it.copy(status = status, respondedAt = respondedAt)
                updateFlow()
            }
        }

        override suspend fun updateStatusWithGrant(
            requestId: String,
            status: String,
            grantedDuration: Long?,
            expiresAt: Long?,
            respondedAt: Long
        ) {
            requests[requestId]?.let {
                requests[requestId] = it.copy(
                    status = status,
                    grantedDuration = grantedDuration,
                    expiresAt = expiresAt,
                    respondedAt = respondedAt
                )
                updateFlow()
            }
        }

        override suspend fun deleteRequest(requestId: String) {
            requests.remove(requestId)
            updateFlow()
        }
    }

    private class FakeAccessGrantDao : AccessGrantDao {
        val grants = mutableMapOf<String, AccessGrantEntity>()

        override suspend fun insertOrUpdate(grant: AccessGrantEntity) {
            grants[grant.grantId] = grant
        }

        override suspend fun getActiveGrant(messageId: String, granteeId: String): AccessGrantEntity? {
            val now = System.currentTimeMillis()
            return grants.values.firstOrNull { it.secureMessageId == messageId && it.granteeId == granteeId && !it.isRevoked && it.expiresAt > now }
        }

        override suspend fun getGrantByMessageId(messageId: String): AccessGrantEntity? {
            val now = System.currentTimeMillis()
            return grants.values.firstOrNull { it.secureMessageId == messageId && !it.isRevoked && it.expiresAt > now }
        }

        override suspend fun getGrantById(grantId: String): AccessGrantEntity? = grants[grantId]

        override fun getAllActiveGrantsFlow(): Flow<List<AccessGrantEntity>> {
            return flowOf(grants.values.filter { !it.isRevoked })
        }

        override suspend fun revokeGrant(grantId: String) {
            grants[grantId]?.let { grants[grantId] = it.copy(isRevoked = true) }
        }

        override suspend fun revokeGrantsForMessage(messageId: String) {
            grants.values.filter { it.secureMessageId == messageId }.forEach {
                grants[it.grantId] = it.copy(isRevoked = true)
            }
        }

        override suspend fun purgeExpiredGrants(currentTime: Long): Int {
            val toRemove = grants.values.filter { it.expiresAt <= currentTime }.map { it.grantId }
            toRemove.forEach { grants.remove(it) }
            return toRemove.size
        }
    }

    private class FakeNoteDao : NoteDao {
        private val notesMap = mutableMapOf<String, NoteEntity>()
        private val flow = MutableStateFlow<List<NoteEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = notesMap.values.toList()
        }

        override fun getNotesFlow(): Flow<List<NoteEntity>> = flow.asStateFlow()
        override suspend fun getNoteById(id: String): NoteEntity? = notesMap[id]

        override suspend fun insertOrUpdate(note: NoteEntity) {
            notesMap[note.id] = note
            updateFlow()
        }

        override suspend fun deleteNote(id: String) {
            notesMap.remove(id)
            updateFlow()
        }

        override suspend fun updatePinStatus(id: String, isPinned: Boolean, updatedAt: Long) {
            notesMap[id]?.let {
                notesMap[id] = it.copy(isPinned = isPinned, updatedAt = updatedAt)
                updateFlow()
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

    private class FakeTestMessageRepository : MessageRepository {
        var lastSentMessage: Message? = null

        override fun getMessages(conversationId: String): Flow<List<Message>> = flowOf(emptyList())
        override suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): Resource<List<Message>> = Resource.Success(emptyList())

        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String): Resource<Message> {
            return sendTextMessage(conversationId, recipientId, text, SecureMessagePolicy())
        }

        override suspend fun sendTextMessage(conversationId: String, recipientId: String, text: String, policy: SecureMessagePolicy): Resource<Message> {
            val msg = Message(
                id = "msg_${System.currentTimeMillis()}",
                conversationId = conversationId,
                senderId = "me",
                recipientId = recipientId,
                encryptedContentBase64 = "encrypted",
                encryptionMetadata = com.cryptora.securechat.domain.model.EncryptionMetadata(
                    initializationVectorBase64 = "",
                    keyAlias = "test"
                ),
                policy = policy,
                decryptedTextCache = text
            )
            lastSentMessage = msg
            return Resource.Success(msg)
        }

        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType): Resource<Message> {
            return sendAttachmentMessage(conversationId, recipientId, uri, messageType, SecureMessagePolicy())
        }

        override suspend fun sendAttachmentMessage(conversationId: String, recipientId: String, uri: android.net.Uri, messageType: MessageType, policy: SecureMessagePolicy): Resource<Message> {
            return Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not implemented"))
        }

        override suspend fun retryMessage(messageId: String): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not implemented"))
        override suspend fun markMessageAsRead(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun decryptMessage(message: Message): Resource<String> = Resource.Success(message.decryptedTextCache ?: "")
        override suspend fun decryptAttachment(attachment: com.cryptora.securechat.domain.model.Attachment): Resource<File> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not implemented"))
        override suspend fun revokeMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun purgeExpiredMessages(): Resource<Int> = Resource.Success(0)
        override suspend fun sendEncryptedMessage(message: Message): Resource<Message> = Resource.Success(message)
        override suspend fun searchLocalMessages(query: String): Resource<List<Message>> = Resource.Success(emptyList())
        override suspend fun deleteLocalMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)
    }
}
