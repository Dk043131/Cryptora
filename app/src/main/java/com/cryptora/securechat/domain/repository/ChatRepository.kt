package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(conversationId: String): Resource<Conversation?>
    suspend fun getOrCreateConversation(participantUserId: String): Resource<Conversation>
    suspend fun deleteConversation(conversationId: String): Resource<Unit>
    suspend fun searchConversations(query: String): Resource<List<Conversation>>
}
