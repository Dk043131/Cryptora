package com.cryptora.securechat.data.message

import android.net.Uri
import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.AttachmentDao
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.entity.AttachmentEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.core.network.NetworkResult
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.data.attachment.AttachmentManager
import com.cryptora.securechat.data.remote.SecureContentApi
import com.cryptora.securechat.data.remote.ServerPolicyMetadata
import com.cryptora.securechat.domain.model.AccessMode
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val conversationDao: ConversationDao,
    private val attachmentManager: AttachmentManager,
    private val cryptoManager: CryptoManager,
    private val secureContentApi: SecureContentApi,
    private val timeManager: AuthoritativeTimeManager,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : MessageRepository {

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesFlow(conversationId).map { entities ->
            val serverTime = timeManager.getAuthoritativeTime()
            entities.map { entity ->
                val attachment = if (entity.hasAttachment) {
                    attachmentDao.getAttachmentByMessageId(entity.id)?.toDomain()
                } else null

                val isExpired = entity.expiresAt != null && serverTime >= entity.expiresAt
                val text = if (isExpired) {
                    "[Access Expired]"
                } else {
                    entity.decryptedTextCache ?: run {
                        try {
                            cryptoManager.decryptString(entity.encryptedContentBase64, entity.keyAlias)
                        } catch (e: Exception) {
                            "[Encrypted Message]"
                        }
                    }
                }

                entity.toDomain(attachment, text)
            }
        }
    }

    override suspend fun getMessagesPaged(
        conversationId: String,
        limit: Int,
        offset: Int
    ): Resource<List<Message>> = withContext(dispatchers.io) {
        try {
            val entities = messageDao.getMessagesPaged(conversationId, limit, offset)
            val serverTime = timeManager.getAuthoritativeTime()
            val domainMessages = entities.map { entity ->
                val attachment = if (entity.hasAttachment) {
                    attachmentDao.getAttachmentByMessageId(entity.id)?.toDomain()
                } else null

                val isExpired = entity.expiresAt != null && serverTime >= entity.expiresAt
                val text = if (isExpired) {
                    "[Access Expired]"
                } else {
                    entity.decryptedTextCache ?: run {
                        try {
                            cryptoManager.decryptString(entity.encryptedContentBase64, entity.keyAlias)
                        } catch (e: Exception) {
                            "[Encrypted Message]"
                        }
                    }
                }

                entity.toDomain(attachment, text)
            }
            Resource.Success(domainMessages)
        } catch (e: Exception) {
            Resource.Error(AppError.Storage("Failed to fetch messages: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        recipientId: String,
        text: String
    ): Resource<Message> = sendTextMessage(conversationId, recipientId, text, SecureMessagePolicy())

    override suspend fun sendTextMessage(
        conversationId: String,
        recipientId: String,
        text: String,
        policy: SecureMessagePolicy
    ): Resource<Message> = withContext(dispatchers.io) {
        try {
            val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
            val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val encryptedBase64 = cryptoManager.encryptString(text, Constants.MASTER_KEY_ALIAS)

            // Register policy with server authoritative metadata
            val serverPolicy = ServerPolicyMetadata(
                messageId = messageId,
                accessMode = policy.accessMode.name,
                expiresAt = policy.expiresAt,
                forwardingPolicy = policy.forwardingPolicy.name,
                approvalRequired = policy.approvalRequired,
                ownerId = currentUserId,
                createdAt = policy.createdAt,
                policyVersion = policy.policyVersion
            )
            secureContentApi.registerPolicy(serverPolicy)

            val entity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientId,
                encryptedContentBase64 = encryptedBase64,
                keyAlias = Constants.MASTER_KEY_ALIAS,
                ivBase64 = "",
                messageType = MessageType.TEXT.name,
                deliveryStatus = MessageDeliveryStatus.SENT.name,
                isOutgoing = true,
                decryptedTextCache = text,
                timestamp = System.currentTimeMillis(),
                hasAttachment = false,
                accessMode = policy.accessMode.name,
                expiresAt = policy.expiresAt,
                forwardingPolicy = policy.forwardingPolicy.name,
                approvalRequired = policy.approvalRequired,
                policyOwnerId = currentUserId,
                policyCreatedAt = policy.createdAt,
                policyVersion = policy.policyVersion,
                isAccessGranted = policy.isAccessGranted
            )

            messageDao.insertOrReplace(entity)

            // Update conversation last message
            updateConversationPreview(conversationId, text, entity.timestamp, MessageDeliveryStatus.SENT)

            Resource.Success(entity.toDomain(attachment = null, decryptedText = text))
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Failed to send message: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun sendAttachmentMessage(
        conversationId: String,
        recipientId: String,
        uri: Uri,
        messageType: MessageType
    ): Resource<Message> = sendAttachmentMessage(conversationId, recipientId, uri, messageType, SecureMessagePolicy())

    override suspend fun sendAttachmentMessage(
        conversationId: String,
        recipientId: String,
        uri: Uri,
        messageType: MessageType,
        policy: SecureMessagePolicy
    ): Resource<Message> = withContext(dispatchers.io) {
        try {
            val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
            val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

            // 1. Process & stream-encrypt attachment
            val attachmentResult = attachmentManager.processAndEncryptAttachment(uri, messageId)
            val attachment = when (attachmentResult) {
                is Resource.Success -> attachmentResult.data
                is Resource.Error -> return@withContext Resource.Error(attachmentResult.error, attachmentResult.cause)
                is Resource.Loading -> return@withContext Resource.Error(AppError.Unknown("Unexpected loading state"))
            }

            // 2. Persist attachment entity
            attachmentDao.insertOrReplace(attachment.toEntity())

            // 3. Register server authoritative policy
            val serverPolicy = ServerPolicyMetadata(
                messageId = messageId,
                accessMode = policy.accessMode.name,
                expiresAt = policy.expiresAt,
                forwardingPolicy = policy.forwardingPolicy.name,
                approvalRequired = policy.approvalRequired,
                ownerId = currentUserId,
                createdAt = policy.createdAt,
                policyVersion = policy.policyVersion
            )
            secureContentApi.registerPolicy(serverPolicy)

            // 4. Encrypt placeholder summary
            val summaryText = if (messageType == MessageType.IMAGE) "[Image]" else "[File: ${attachment.fileName}]"
            val encryptedBase64 = cryptoManager.encryptString(summaryText, Constants.MASTER_KEY_ALIAS)

            val entity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientId,
                encryptedContentBase64 = encryptedBase64,
                keyAlias = Constants.MASTER_KEY_ALIAS,
                ivBase64 = "",
                messageType = messageType.name,
                deliveryStatus = MessageDeliveryStatus.SENT.name,
                isOutgoing = true,
                decryptedTextCache = summaryText,
                timestamp = System.currentTimeMillis(),
                hasAttachment = true,
                accessMode = policy.accessMode.name,
                expiresAt = policy.expiresAt,
                forwardingPolicy = policy.forwardingPolicy.name,
                approvalRequired = policy.approvalRequired,
                policyOwnerId = currentUserId,
                policyCreatedAt = policy.createdAt,
                policyVersion = policy.policyVersion,
                isAccessGranted = policy.isAccessGranted
            )

            messageDao.insertOrReplace(entity)

            // 5. Update conversation preview
            updateConversationPreview(conversationId, summaryText, entity.timestamp, MessageDeliveryStatus.SENT)

            Resource.Success(entity.toDomain(attachment = attachment, decryptedText = summaryText))
        } catch (e: Exception) {
            Resource.Error(AppError.Storage("Failed to send attachment: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun retryMessage(messageId: String): Resource<Message> = withContext(dispatchers.io) {
        val existing = messageDao.getMessageById(messageId)
            ?: return@withContext Resource.Error(AppError.NotFound("Message $messageId not found"))

        val updated = existing.copy(
            deliveryStatus = MessageDeliveryStatus.SENT.name,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertOrReplace(updated)

        val attachment = if (updated.hasAttachment) {
            attachmentDao.getAttachmentByMessageId(updated.id)?.toDomain()
        } else null

        val text = updated.decryptedTextCache ?: run {
            try {
                cryptoManager.decryptString(updated.encryptedContentBase64, updated.keyAlias)
            } catch (e: Exception) {
                "[Encrypted Message]"
            }
        }

        updateConversationPreview(updated.conversationId, text, updated.timestamp, MessageDeliveryStatus.SENT)

        Resource.Success(updated.toDomain(attachment, text))
    }

    override suspend fun markMessageAsRead(messageId: String): Resource<Unit> = withContext(dispatchers.io) {
        messageDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.READ.name)
        Resource.Success(Unit)
    }

    override suspend fun decryptMessage(message: Message): Resource<String> = withContext(dispatchers.default) {
        val authoritativeTime = timeManager.getAuthoritativeTime()
        if (message.policy.isExpired(authoritativeTime)) {
            // Shred cache
            messageDao.updateDecryptedCache(message.id, "")
            return@withContext Resource.Error(AppError.Expired("Message access has expired on the authoritative server"))
        }

        if (message.policy.isLocked) {
            return@withContext Resource.Error(AppError.Unauthorized("Access not granted. Sender approval required."))
        }

        message.decryptedTextCache?.let {
            if (it != "[Access Expired]" && it.isNotBlank()) return@withContext Resource.Success(it)
        }

        try {
            val decrypted = cryptoManager.decryptString(
                message.encryptedContentBase64,
                message.encryptionMetadata.keyAlias
            )
            messageDao.updateDecryptedCache(message.id, decrypted)
            Resource.Success(decrypted)
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Decryption failed: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun decryptAttachment(attachment: Attachment): Resource<File> = withContext(dispatchers.io) {
        val serverTime = timeManager.getAuthoritativeTime()
        val messageEntity = messageDao.getMessageById(attachment.messageId)

        // Expiry check
        if (messageEntity != null && messageEntity.expiresAt != null && serverTime >= messageEntity.expiresAt) {
            attachmentManager.deleteDecryptedPreview(attachment)
            return@withContext Resource.Error(AppError.Unauthorized("Cannot decrypt attachment: Message access has expired."))
        }

        // Access Grant check for Request Access mode
        if (messageEntity != null && messageEntity.accessMode == AccessMode.REQUEST_ACCESS.name && !messageEntity.isAccessGranted) {
            val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
            if (currentUserId != messageEntity.senderId) {
                return@withContext Resource.Error(AppError.Unauthorized("Cannot access attachment: Access has not been granted by the owner."))
            }
        }

        attachmentManager.decryptAttachmentToCache(attachment)
    }

    override suspend fun revokeMessage(messageId: String): Resource<Unit> = withContext(dispatchers.io) {
        val currentUserId = authRepository.getAuthenticatedUser().firstOrNull()?.id ?: "me"
        when (val netResult = secureContentApi.revokePolicy(messageId, currentUserId)) {
            is NetworkResult.Error -> return@withContext Resource.Error(AppError.Unauthorized(netResult.message))
            is NetworkResult.Exception -> return@withContext Resource.Error(AppError.Unknown("Revocation network error", netResult.throwable))
            is NetworkResult.Success -> Unit
        }

        messageDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED.name)
        messageDao.updateDecryptedCache(messageId, "")
        val att = attachmentDao.getAttachmentByMessageId(messageId)?.toDomain()
        if (att != null) {
            attachmentManager.deleteDecryptedPreview(att)
        }
        Resource.Success(Unit)
    }

    override suspend fun purgeExpiredMessages(): Resource<Int> = withContext(dispatchers.io) {
        val serverTime = timeManager.getAuthoritativeTime()
        val expiredEntities = messageDao.getExpiredCachedMessages(serverTime)
        for (entity in expiredEntities) {
            if (entity.hasAttachment) {
                attachmentDao.getAttachmentByMessageId(entity.id)?.let {
                    attachmentManager.deleteDecryptedPreview(it.toDomain())
                }
            }
        }
        val shreddedCount = messageDao.shredExpiredDecryptedCaches(serverTime)
        Resource.Success(shreddedCount)
    }

    override suspend fun sendEncryptedMessage(message: Message): Resource<Message> = withContext(dispatchers.io) {
        val entity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            senderId = message.senderId,
            recipientId = message.recipientId,
            encryptedContentBase64 = message.encryptedContentBase64,
            keyAlias = message.encryptionMetadata.keyAlias,
            ivBase64 = message.encryptionMetadata.initializationVectorBase64,
            messageType = message.messageType.name,
            deliveryStatus = message.deliveryStatus.name,
            isOutgoing = message.isOutgoing,
            decryptedTextCache = message.decryptedTextCache,
            timestamp = message.timestamp,
            hasAttachment = message.attachment != null,
            accessMode = message.policy.accessMode.name,
            expiresAt = message.policy.expiresAt,
            forwardingPolicy = message.policy.forwardingPolicy.name,
            approvalRequired = message.policy.approvalRequired,
            policyOwnerId = message.policy.ownerId,
            policyCreatedAt = message.policy.createdAt,
            isAccessGranted = message.policy.isAccessGranted,
            rootMessageId = message.rootMessageId,
            forwardCount = message.forwardCount,
            isForwarded = message.isForwarded,
            originalSenderUsername = message.originalSenderUsername
        )
        messageDao.insertOrReplace(entity)
        message.attachment?.let {
            attachmentDao.insertOrReplace(it.toEntity())
        }
        Resource.Success(message)
    }

    override suspend fun searchLocalMessages(query: String): Resource<List<Message>> = withContext(dispatchers.io) {
        val clean = query.trim()
        if (clean.isBlank()) {
            return@withContext Resource.Success(emptyList())
        }
        val currentTime = timeManager.getAuthoritativeTime()
        val entities = messageDao.searchLocalMessages(clean, currentTime)
        val messages = entities.map { entity ->
            val attachment = if (entity.hasAttachment) {
                attachmentDao.getAttachmentByMessageId(entity.id)?.toDomain()
            } else null
            val decrypted = entity.decryptedTextCache ?: ""
            entity.toDomain(attachment, decrypted)
        }
        Resource.Success(messages)
    }

    override suspend fun deleteLocalMessage(messageId: String): Resource<Unit> = withContext(dispatchers.io) {
        messageDao.deleteMessage(messageId)
        attachmentDao.deleteAttachmentByMessageId(messageId)
        Resource.Success(Unit)
    }

    private suspend fun updateConversationPreview(
        conversationId: String,
        lastMessageText: String,
        timestamp: Long,
        deliveryStatus: MessageDeliveryStatus
    ) {
        val existing = conversationDao.getConversationById(conversationId)
        if (existing != null) {
            conversationDao.insertOrUpdate(
                existing.copy(
                    lastMessageText = lastMessageText,
                    lastMessageTimestamp = timestamp,
                    lastMessageDeliveryStatus = deliveryStatus.name,
                    updatedAt = timestamp
                )
            )
        }
    }

    private fun MessageEntity.toDomain(attachment: Attachment?, decryptedText: String): Message {
        val type = try { MessageType.valueOf(messageType) } catch (e: Exception) { MessageType.TEXT }
        val status = try { MessageDeliveryStatus.valueOf(deliveryStatus) } catch (e: Exception) { MessageDeliveryStatus.SENT }

        val legacyStatus = when (status) {
            MessageDeliveryStatus.READ -> MessageStatus.DECRYPTED_SEEN
            MessageDeliveryStatus.DELIVERED -> MessageStatus.DELIVERED
            MessageDeliveryStatus.FAILED -> MessageStatus.REVOKED
            else -> MessageStatus.ENCRYPTED_SENT
        }

        val mode = try { AccessMode.valueOf(accessMode) } catch (e: Exception) { AccessMode.IMMEDIATE_ACCESS }
        val fwd = try { ForwardingPolicy.valueOf(forwardingPolicy) } catch (e: Exception) { ForwardingPolicy.FORWARDING_DISABLED }

        val secPolicy = SecureMessagePolicy(
            accessMode = mode,
            expiresAt = expiresAt,
            forwardingPolicy = fwd,
            approvalRequired = approvalRequired,
            ownerId = policyOwnerId.ifBlank { senderId },
            createdAt = if (policyCreatedAt > 0L) policyCreatedAt else timestamp,
            policyVersion = policyVersion,
            isAccessGranted = isAccessGranted
        )

        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            encryptedContentBase64 = encryptedContentBase64,
            encryptionMetadata = EncryptionMetadata(
                keyAlias = keyAlias,
                initializationVectorBase64 = ivBase64
            ),
            policy = secPolicy,
            status = legacyStatus,
            messageType = type,
            deliveryStatus = status,
            isOutgoing = isOutgoing,
            decryptedTextCache = decryptedText,
            forwardCount = forwardCount,
            isForwarded = isForwarded,
            rootMessageId = rootMessageId,
            originalSenderUsername = originalSenderUsername,
            timestamp = timestamp,
            attachment = attachment
        )
    }

    private fun AttachmentEntity.toDomain(): Attachment {
        return Attachment(
            attachmentId = attachmentId,
            messageId = messageId,
            encryptedFileReference = encryptedFileReference,
            fileName = fileName,
            mimeType = mimeType,
            size = size,
            encryptedSize = encryptedSize,
            createdAt = createdAt
        )
    }

    private fun Attachment.toEntity(): AttachmentEntity {
        return AttachmentEntity(
            attachmentId = attachmentId,
            messageId = messageId,
            encryptedFileReference = encryptedFileReference,
            fileName = fileName,
            mimeType = mimeType,
            size = size,
            encryptedSize = encryptedSize,
            createdAt = createdAt
        )
    }
}
