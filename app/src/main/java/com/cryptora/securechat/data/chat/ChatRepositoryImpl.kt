package com.cryptora.securechat.data.chat

import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.entity.ConversationEntity
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider
) : ChatRepository {

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getConversationsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(conversationId: String): Resource<Conversation?> = withContext(dispatchers.io) {
        val entity = conversationDao.getConversationById(conversationId)
        Resource.Success(entity?.toDomain())
    }

    override suspend fun getOrCreateConversation(participantUserId: String): Resource<Conversation> = withContext(dispatchers.io) {
        // Check if conversation already exists by participant or ID
        val existing = conversationDao.getConversationByParticipant(participantUserId)
            ?: conversationDao.getConversationById(participantUserId)

        if (existing != null) {
            return@withContext Resource.Success(existing.toDomain())
        }

        // Fetch user profile if available to populate name/username
        var username = participantUserId
        var fullName = participantUserId
        var avatarUrl: String? = null

        when (val userResult = userRepository.getUserProfile(participantUserId)) {
            is Resource.Success -> {
                username = userResult.data.username
                fullName = userResult.data.fullName.ifBlank { username }
                avatarUrl = userResult.data.avatarUrl
            }
            else -> {
                // If not found by ID directly, check if it's already a formatted username
                val clean = participantUserId.removePrefix("@")
                when (val byUsername = userRepository.searchUserByUsername(clean)) {
                    is Resource.Success -> {
                        byUsername.data?.let {
                            username = it.username
                            fullName = it.fullName.ifBlank { it.username }
                            avatarUrl = it.avatarUrl
                        }
                    }
                    else -> Unit
                }
            }
        }

        val convId = "conv_${System.currentTimeMillis()}_$participantUserId"
        val entity = ConversationEntity(
            id = convId,
            participantUserId = participantUserId,
            participantUsername = username,
            participantFullName = fullName,
            lastMessageText = null,
            lastMessageTimestamp = null,
            lastMessageDeliveryStatus = null,
            unreadCount = 0,
            isSecretSession = true,
            updatedAt = System.currentTimeMillis()
        )

        conversationDao.insertOrUpdate(entity)
        Resource.Success(entity.toDomain(avatarUrl))
    }

    override suspend fun deleteConversation(conversationId: String): Resource<Unit> = withContext(dispatchers.io) {
        conversationDao.deleteConversation(conversationId)
        Resource.Success(Unit)
    }

    override suspend fun searchConversations(query: String): Resource<List<Conversation>> = withContext(dispatchers.io) {
        val clean = query.trim()
        if (clean.isBlank()) {
            return@withContext Resource.Success(emptyList())
        }
        val entities = conversationDao.searchConversations(clean)
        val domainList = entities.map { it.toDomain() }
        Resource.Success(domainList)
    }

    private fun ConversationEntity.toDomain(avatarUrl: String? = null): Conversation {
        val lastMsg = if (!lastMessageText.isNullOrBlank()) {
            val status = try {
                lastMessageDeliveryStatus?.let { MessageDeliveryStatus.valueOf(it) } ?: MessageDeliveryStatus.SENT
            } catch (e: Exception) {
                MessageDeliveryStatus.SENT
            }
            Message(
                id = "last_${id}",
                conversationId = id,
                senderId = "",
                recipientId = participantUserId,
                encryptedContentBase64 = "",
                encryptionMetadata = EncryptionMetadata(
                    initializationVectorBase64 = "",
                    keyAlias = com.cryptora.securechat.core.common.Constants.MASTER_KEY_ALIAS
                ),
                policy = SecureMessagePolicy(),
                deliveryStatus = status,
                decryptedTextCache = lastMessageText,
                timestamp = lastMessageTimestamp ?: updatedAt
            )
        } else {
            null
        }

        return Conversation(
            id = id,
            participantUser = User(
                id = participantUserId,
                username = participantUsername,
                mobileNumber = "",
                publicKey = "pk_$participantUserId",
                fullName = participantFullName,
                avatarUrl = avatarUrl
            ),
            lastMessage = lastMsg,
            unreadCount = unreadCount,
            isSecretSession = isSecretSession,
            updatedAt = updatedAt
        )
    }
}
