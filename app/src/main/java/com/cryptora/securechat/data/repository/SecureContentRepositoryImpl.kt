package com.cryptora.securechat.data.repository

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.AccessGrantDao
import com.cryptora.securechat.core.database.dao.AccessRequestDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.entity.AccessGrantEntity
import com.cryptora.securechat.core.database.entity.AccessRequestEntity
import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.core.network.NetworkResult
import com.cryptora.securechat.data.remote.SecureContentApi
import com.cryptora.securechat.domain.model.AccessGrant
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.repository.AccessValidationResult
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureContentRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val accessRequestDao: AccessRequestDao,
    private val accessGrantDao: AccessGrantDao,
    private val secureContentApi: SecureContentApi,
    private val timeManager: AuthoritativeTimeManager,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : SecureContentRepository {

    private val _notificationEvents = MutableSharedFlow<AccessNotificationEvent>(replay = 1, extraBufferCapacity = 64)

    override fun observeAccessNotifications(): Flow<AccessNotificationEvent> = _notificationEvents.asSharedFlow()

    override suspend fun emitNotification(event: AccessNotificationEvent) {
        _notificationEvents.emit(event)
    }

    override suspend fun validateMessageAccess(
        messageId: String
    ): Resource<AccessValidationResult> = withContext(dispatchers.io) {
        val existing = messageDao.getMessageById(messageId)
            ?: return@withContext Resource.Error(AppError.NotFound("Message $messageId not found"))

        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"

        when (val netResult = secureContentApi.validateAccess(messageId, currentUserId)) {
            is NetworkResult.Success -> {
                val resp = netResult.data
                val authoritativeTime = resp.authoritativeServerTime

                if (resp.isExpired) {
                    // Crypto-shredding: wipe cached decrypted text immediately
                    messageDao.updateDecryptedCache(messageId, "")
                    messageDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED.name)
                    accessGrantDao.revokeGrantsForMessage(messageId)

                    emitNotification(
                        AccessNotificationEvent.AccessExpired(
                            secureMessageId = messageId,
                            contentTitle = existing.decryptedTextCache?.take(30) ?: "Secure Content"
                        )
                    )

                    Resource.Success(
                        AccessValidationResult(
                            messageId = messageId,
                            isAuthorized = false,
                            isExpired = true,
                            remainingTimeMillis = 0L,
                            accessMode = resp.accessMode
                        )
                    )
                } else {
                    val activeGrantEntity = accessGrantDao.getActiveGrant(messageId, currentUserId)
                    val activeGrant = activeGrantEntity?.toDomain()

                    val targetExpiry = activeGrant?.expiresAt ?: resp.expiresAt
                    val remaining = targetExpiry?.let { exp ->
                        val diff = exp - authoritativeTime
                        if (diff > 0) diff else 0L
                    } ?: Long.MAX_VALUE

                    if (resp.isAuthorized && !existing.isAccessGranted) {
                        messageDao.insertOrReplace(
                            existing.copy(
                                isAccessGranted = true,
                                expiresAt = targetExpiry ?: existing.expiresAt
                            )
                        )
                    }

                    Resource.Success(
                        AccessValidationResult(
                            messageId = messageId,
                            isAuthorized = resp.isAuthorized,
                            isExpired = false,
                            remainingTimeMillis = remaining,
                            accessMode = resp.accessMode,
                            activeGrant = activeGrant
                        )
                    )
                }
            }
            is NetworkResult.Error -> {
                Resource.Error(AppError.Network(netResult.message, netResult.statusCode))
            }
            is NetworkResult.Exception -> {
                Resource.Error(AppError.Unknown("Validation network error", netResult.throwable))
            }
        }
    }

    override suspend fun requestAccess(
        messageId: String,
        requestedDuration: Long,
        contentTitle: String
    ): Resource<AccessRequest> = withContext(dispatchers.io) {
        val existing = messageDao.getMessageById(messageId)
            ?: return@withContext Resource.Error(AppError.NotFound("Message not found"))

        val currentUser = authRepository.getAuthenticatedUser().firstOrNull()
        val requesterId = currentUser?.id ?: "me"
        val requesterUsername = currentUser?.username ?: "me"

        when (val netResult = secureContentApi.requestAccess(
            messageId = messageId,
            requesterId = requesterId,
            requesterUsername = requesterUsername,
            requestedDuration = requestedDuration,
            contentTitle = contentTitle
        )) {
            is NetworkResult.Success -> {
                val reqEntity = AccessRequestEntity(
                    requestId = netResult.data.requestId,
                    messageId = messageId,
                    conversationId = existing.conversationId,
                    requesterId = requesterId,
                    requesterUsername = requesterUsername,
                    ownerId = existing.policyOwnerId.ifBlank { existing.senderId },
                    contentTitle = contentTitle,
                    requestedDuration = requestedDuration,
                    status = "PENDING",
                    requestedAt = System.currentTimeMillis()
                )
                accessRequestDao.insertOrUpdate(reqEntity)

                // Emit event to notify sender
                emitNotification(
                    AccessNotificationEvent.RequestReceived(
                        requestId = reqEntity.requestId,
                        requesterUsername = requesterUsername,
                        contentTitle = contentTitle,
                        requestedDuration = requestedDuration,
                        secureMessageId = messageId,
                        conversationId = existing.conversationId
                    )
                )

                Resource.Success(reqEntity.toDomain())
            }
            is NetworkResult.Error -> {
                Resource.Error(AppError.Network(netResult.message, netResult.statusCode))
            }
            is NetworkResult.Exception -> {
                Resource.Error(AppError.Unknown("Request access network error", netResult.throwable))
            }
        }
    }

    override suspend fun respondToAccessRequest(
        requestId: String,
        approved: Boolean,
        finalDurationMillis: Long?
    ): Resource<AccessGrant?> = withContext(dispatchers.io) {
        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
        when (val netResult = secureContentApi.respondToAccessRequest(
            requestId = requestId,
            responderUserId = currentUserId,
            approved = approved,
            finalDurationMillis = finalDurationMillis
        )) {
            is NetworkResult.Success -> {
                val grantResp = netResult.data
                val respondedAt = System.currentTimeMillis()

                if (approved && grantResp != null) {
                    val grant = AccessGrant(
                        grantId = grantResp.grantId,
                        requestId = grantResp.requestId,
                        secureMessageId = grantResp.messageId,
                        granteeId = grantResp.granteeId,
                        grantedDuration = grantResp.grantedDuration,
                        grantedAt = grantResp.grantedAt,
                        expiresAt = grantResp.expiresAt,
                        isRevoked = false
                    )

                    accessGrantDao.insertOrUpdate(AccessGrantEntity.fromDomain(grant))

                    accessRequestDao.updateStatusWithGrant(
                        requestId = requestId,
                        status = "APPROVED",
                        grantedDuration = grantResp.grantedDuration,
                        expiresAt = grantResp.expiresAt,
                        respondedAt = respondedAt
                    )

                    val requestEntity = accessRequestDao.getRequestById(requestId)
                    if (requestEntity != null) {
                        val msg = messageDao.getMessageById(requestEntity.messageId)
                        if (msg != null) {
                            messageDao.insertOrReplace(
                                msg.copy(
                                    isAccessGranted = true,
                                    expiresAt = grantResp.expiresAt
                                )
                            )
                        }

                        emitNotification(
                            AccessNotificationEvent.RequestApproved(
                                requestId = requestId,
                                contentTitle = requestEntity.contentTitle,
                                grantedDuration = grantResp.grantedDuration,
                                secureMessageId = requestEntity.messageId
                            )
                        )
                    }

                    Resource.Success(grant)
                } else {
                    accessRequestDao.updateStatus(requestId, "REJECTED", respondedAt)
                    val requestEntity = accessRequestDao.getRequestById(requestId)
                    if (requestEntity != null) {
                        emitNotification(
                            AccessNotificationEvent.RequestRejected(
                                requestId = requestId,
                                contentTitle = requestEntity.contentTitle,
                                secureMessageId = requestEntity.messageId
                            )
                        )
                    }
                    Resource.Success(null)
                }
            }
            is NetworkResult.Error -> Resource.Error(AppError.Network(netResult.message, netResult.statusCode))
            is NetworkResult.Exception -> Resource.Error(AppError.Unknown("Respond to access error", netResult.throwable))
        }
    }

    override suspend fun cancelAccessRequest(requestId: String): Resource<Unit> = withContext(dispatchers.io) {
        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
        when (val netResult = secureContentApi.cancelAccessRequest(requestId, currentUserId)) {
            is NetworkResult.Success -> {
                accessRequestDao.updateStatus(requestId, "CANCELLED", System.currentTimeMillis())
                Resource.Success(Unit)
            }
            is NetworkResult.Error -> Resource.Error(AppError.Network(netResult.message, netResult.statusCode))
            is NetworkResult.Exception -> Resource.Error(AppError.Unknown("Cancel request error", netResult.throwable))
        }
    }

    override fun getPendingAccessRequests(): Flow<List<AccessRequest>> {
        return accessRequestDao.getPendingRequestsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAccessRequests(): Flow<List<AccessRequest>> {
        return accessRequestDao.getAllRequestsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccessRequestForMessage(messageId: String): Flow<AccessRequest?> {
        return accessRequestDao.getLatestRequestFlowForMessage(messageId).map { it?.toDomain() }
    }

    override suspend fun getActiveGrant(messageId: String): Resource<AccessGrant?> = withContext(dispatchers.io) {
        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
        val entity = accessGrantDao.getActiveGrant(messageId, currentUserId)
        Resource.Success(entity?.toDomain())
    }

    override suspend fun syncAuthoritativeExpiry(): Resource<Int> = withContext(dispatchers.io) {
        val serverTime = timeManager.getAuthoritativeTime()

        when (val netResult = secureContentApi.syncAuthoritativeServerTime()) {
            is NetworkResult.Success -> {
                timeManager.updateServerTime(netResult.data)
            }
            else -> Unit
        }

        // Atomically shred caches for expired messages
        val shreddedCount = messageDao.shredExpiredDecryptedCaches(serverTime)
        accessGrantDao.purgeExpiredGrants(serverTime)
        Resource.Success(shreddedCount)
    }

    override suspend fun revokeMessageAccess(messageId: String): Resource<Unit> = withContext(dispatchers.io) {
        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
        when (val netResult = secureContentApi.revokePolicy(messageId, currentUserId)) {
            is NetworkResult.Error -> return@withContext Resource.Error(AppError.Unauthorized(netResult.message))
            is NetworkResult.Exception -> return@withContext Resource.Error(AppError.Unknown("Revocation network error", netResult.throwable))
            is NetworkResult.Success -> Unit
        }

        // Shred local cache and mark failed
        val existing = messageDao.getMessageById(messageId)
        messageDao.updateDecryptedCache(messageId, "")
        messageDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED.name)
        accessGrantDao.revokeGrantsForMessage(messageId)

        emitNotification(
            AccessNotificationEvent.AccessRevoked(
                secureMessageId = messageId,
                contentTitle = existing?.decryptedTextCache?.take(30) ?: "Secure Content"
            )
        )

        Resource.Success(Unit)
    }

    override suspend fun updateMessagePolicy(
        messageId: String,
        policy: SecureMessagePolicy
    ): Resource<Unit> = withContext(dispatchers.io) {
        val existing = messageDao.getMessageById(messageId) ?: return@withContext Resource.Success(Unit)

        val updated = existing.copy(
            accessMode = policy.accessMode.name,
            expiresAt = policy.expiresAt,
            forwardingPolicy = policy.forwardingPolicy.name,
            approvalRequired = policy.approvalRequired,
            policyOwnerId = policy.ownerId,
            policyCreatedAt = policy.createdAt,
            policyVersion = policy.policyVersion,
            isAccessGranted = policy.isAccessGranted
        )
        messageDao.insertOrReplace(updated)
        Resource.Success(Unit)
    }

    override fun getAuthoritativeServerTime(): Long {
        return timeManager.getAuthoritativeTime()
    }
}
