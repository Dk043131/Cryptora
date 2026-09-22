package com.cryptora.securechat.data.repository

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.dao.ForwardEventDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.entity.ForwardEventEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.model.ForwardEvent
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.ForwardingRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForwardingRepositoryImpl @Inject constructor(
    private val forwardEventDao: ForwardEventDao,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val authRepository: AuthRepository,
    private val secureContentRepository: SecureContentRepository,
    private val timeManager: AuthoritativeTimeManager,
    private val dispatchers: DispatcherProvider
) : ForwardingRepository {

    override suspend fun forwardMessage(
        messageId: String,
        targetConversationId: String,
        targetUserId: String,
        targetUsername: String
    ): Resource<ForwardEvent> = withContext(dispatchers.io) {
        val sourceMessage = messageDao.getMessageById(messageId)
            ?: return@withContext Resource.Error(AppError.NotFound("Source message not found: $messageId"))

        val policy = try {
            ForwardingPolicy.valueOf(sourceMessage.forwardingPolicy)
        } catch (e: Exception) {
            ForwardingPolicy.FORWARDING_DISABLED
        }

        // Rule 1: Forwarding Disabled
        if (policy == ForwardingPolicy.FORWARDING_DISABLED) {
            return@withContext Resource.Error(AppError.Validation("Forwarding is disabled by the content owner."))
        }

        val authoritativeTime = timeManager.getAuthoritativeTime()

        // Rule 2: Cannot forward already expired content
        if (sourceMessage.expiresAt != null && authoritativeTime >= sourceMessage.expiresAt) {
            return@withContext Resource.Error(AppError.Validation("Cannot forward expired content."))
        }

        val rootId = if (sourceMessage.rootMessageId.isNotBlank()) sourceMessage.rootMessageId else sourceMessage.id
        val latestEvent = forwardEventDao.getLatestEventForRoot(rootId)
        val previousHash = latestEvent?.eventHash ?: ForwardEvent.GENESIS_HASH
        val parentEventId = latestEvent?.eventId

        val currentUser = authRepository.getAuthenticatedUser().firstOrNull()
        val fromUserId = currentUser?.id ?: sourceMessage.recipientId
        val fromUsername = currentUser?.username ?: "me"

        // Rule 3: Approval required vs Allowed
        val requiresApproval = (policy == ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL)
        val approvalStatus = if (requiresApproval) ForwardApprovalStatus.PENDING else ForwardApprovalStatus.APPROVED

        val eventId = "fwd_evt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val eventHash = ForwardEvent.calculateHash(
            previousEventHash = previousHash,
            eventId = eventId,
            rootMessageId = rootId,
            fromUserId = fromUserId,
            toUserId = targetUserId,
            timestamp = authoritativeTime,
            policyVersion = sourceMessage.policyVersion,
            approvalStatus = approvalStatus
        )

        val newForwardedMessageId = "msg_fwd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val forwardEvent = ForwardEvent(
            eventId = eventId,
            rootMessageId = rootId,
            secureMessageId = newForwardedMessageId,
            parentEventId = parentEventId,
            fromUserId = fromUserId,
            fromUsername = fromUsername,
            toUserId = targetUserId,
            toUsername = targetUsername,
            timestamp = authoritativeTime,
            policyVersion = sourceMessage.policyVersion,
            approvalStatus = approvalStatus,
            previousEventHash = previousHash,
            eventHash = eventHash,
            signatureMetadata = "sig_${fromUserId}_${authoritativeTime}"
        )

        forwardEventDao.insertEvent(ForwardEventEntity.fromDomain(forwardEvent))

        // Chain depth calculation
        val currentHops = forwardEventDao.getApprovedHopCount(rootId)
        val originalOwnerUsername = sourceMessage.originalSenderUsername ?: sourceMessage.senderId

        // Create forwarded message instance with STRICT EXPIRY INHERITANCE
        val forwardedMessageEntity = MessageEntity(
            id = newForwardedMessageId,
            conversationId = targetConversationId,
            senderId = fromUserId,
            recipientId = targetUserId,
            encryptedContentBase64 = sourceMessage.encryptedContentBase64,
            keyAlias = sourceMessage.keyAlias,
            ivBase64 = sourceMessage.ivBase64,
            messageType = sourceMessage.messageType,
            deliveryStatus = MessageDeliveryStatus.SENT.name,
            isOutgoing = true,
            decryptedTextCache = if (approvalStatus == ForwardApprovalStatus.APPROVED) sourceMessage.decryptedTextCache else "",
            timestamp = authoritativeTime,
            hasAttachment = sourceMessage.hasAttachment,
            accessMode = sourceMessage.accessMode,
            expiresAt = sourceMessage.expiresAt, // STRICT INHERITANCE: original expiry ceiling maintained
            forwardingPolicy = sourceMessage.forwardingPolicy,
            approvalRequired = requiresApproval,
            policyOwnerId = sourceMessage.policyOwnerId.ifBlank { sourceMessage.senderId },
            policyCreatedAt = sourceMessage.policyCreatedAt,
            policyVersion = sourceMessage.policyVersion,
            isAccessGranted = (approvalStatus == ForwardApprovalStatus.APPROVED),
            rootMessageId = rootId,
            forwardCount = currentHops,
            isForwarded = true,
            originalSenderUsername = originalOwnerUsername
        )

        messageDao.insertOrReplace(forwardedMessageEntity)

        // Update target conversation snippet
        val preview = if (forwardedMessageEntity.decryptedTextCache.isNullOrBlank()) {
            "Forwarded secure message"
        } else {
            forwardedMessageEntity.decryptedTextCache
        }
        val targetConv = conversationDao.getConversationById(targetConversationId)
        if (targetConv != null) {
            conversationDao.insertOrUpdate(
                targetConv.copy(
                    lastMessageText = preview,
                    lastMessageTimestamp = authoritativeTime,
                    lastMessageDeliveryStatus = MessageDeliveryStatus.SENT.name,
                    updatedAt = authoritativeTime
                )
            )
        }

        // Emit notification if approval required
        if (requiresApproval) {
            secureContentRepository.emitNotification(
                AccessNotificationEvent.RequestReceived(
                    requestId = eventId,
                    requesterUsername = fromUsername,
                    contentTitle = "Forward request to @$targetUsername",
                    requestedDuration = sourceMessage.expiresAt?.let { it - authoritativeTime } ?: 1800000L,
                    secureMessageId = forwardedMessageEntity.id,
                    conversationId = targetConversationId
                )
            )
        }

        Resource.Success(forwardEvent)
    }

    override fun getForwardChainFlow(rootMessageId: String): Flow<List<ForwardEvent>> {
        return forwardEventDao.getEventsForRootFlow(rootMessageId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getForwardChain(rootMessageId: String): Resource<List<ForwardEvent>> = withContext(dispatchers.io) {
        val entities = forwardEventDao.getEventsForRoot(rootMessageId)
        Resource.Success(entities.map { it.toDomain() })
    }

    override suspend fun verifyChainIntegrity(rootMessageId: String): Resource<ForwardChainIntegrity> = withContext(dispatchers.io) {
        val events = forwardEventDao.getEventsForRoot(rootMessageId).map { it.toDomain() }

        if (events.isEmpty()) {
            return@withContext Resource.Success(
                ForwardChainIntegrity(
                    rootMessageId = rootMessageId,
                    isVerified = true,
                    chainDepth = 0,
                    events = emptyList(),
                    validationMessage = "Genesis state (no forward hops recorded)."
                )
            )
        }

        var expectedPrevHash = ForwardEvent.GENESIS_HASH

        for ((index, event) in events.withIndex()) {
            // Check continuity of previousEventHash
            if (event.previousEventHash != expectedPrevHash) {
                return@withContext Resource.Success(
                    ForwardChainIntegrity(
                        rootMessageId = rootMessageId,
                        isVerified = false,
                        chainDepth = events.size,
                        events = events,
                        brokenAtEventId = event.eventId,
                        validationMessage = "Tampered chain link at step ${index + 1}. Expected previous hash $expectedPrevHash, but found ${event.previousEventHash}."
                    )
                )
            }

            // Recompute SHA-256 hash of event fields
            val recalculated = ForwardEvent.calculateHash(
                previousEventHash = event.previousEventHash,
                eventId = event.eventId,
                rootMessageId = event.rootMessageId,
                fromUserId = event.fromUserId,
                toUserId = event.toUserId,
                timestamp = event.timestamp,
                policyVersion = event.policyVersion,
                approvalStatus = event.approvalStatus
            )

            if (recalculated != event.eventHash) {
                return@withContext Resource.Success(
                    ForwardChainIntegrity(
                        rootMessageId = rootMessageId,
                        isVerified = false,
                        chainDepth = events.size,
                        events = events,
                        brokenAtEventId = event.eventId,
                        validationMessage = "Tampered payload at step ${index + 1} (${event.eventId}). Hash signature mismatch."
                    )
                )
            }

            expectedPrevHash = event.eventHash
        }

        Resource.Success(
            ForwardChainIntegrity(
                rootMessageId = rootMessageId,
                isVerified = true,
                chainDepth = events.size,
                events = events,
                validationMessage = "All ${events.size} forwarding hops cryptographically verified with unbroken SHA-256 hash chain."
            )
        )
    }

    override suspend fun respondToForwardRequest(
        eventId: String,
        approved: Boolean
    ): Resource<Unit> = withContext(dispatchers.io) {
        val eventEntity = forwardEventDao.getEventById(eventId)
            ?: return@withContext Resource.Error(AppError.NotFound("Forward event not found: $eventId"))

        val newStatus = if (approved) ForwardApprovalStatus.APPROVED else ForwardApprovalStatus.REJECTED
        forwardEventDao.updateStatus(eventId, newStatus.name)

        // If approved, unlock the forwarded message
        val msg = messageDao.getMessageById(eventEntity.secureMessageId)
        if (msg != null) {
            val updatedMsg = msg.copy(
                isAccessGranted = approved,
                deliveryStatus = if (approved) MessageDeliveryStatus.DELIVERED.name else MessageDeliveryStatus.FAILED.name
            )
            messageDao.insertOrReplace(updatedMsg)
        }

        Resource.Success(Unit)
    }

    override suspend fun getForwardHopCount(rootMessageId: String): Int = withContext(dispatchers.io) {
        forwardEventDao.getApprovedHopCount(rootMessageId)
    }
}
