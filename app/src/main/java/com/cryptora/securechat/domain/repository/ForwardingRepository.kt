package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.model.ForwardEvent
import kotlinx.coroutines.flow.Flow

interface ForwardingRepository {

    suspend fun forwardMessage(
        messageId: String,
        targetConversationId: String,
        targetUserId: String,
        targetUsername: String
    ): Resource<ForwardEvent>

    fun getForwardChainFlow(rootMessageId: String): Flow<List<ForwardEvent>>

    suspend fun getForwardChain(rootMessageId: String): Resource<List<ForwardEvent>>

    suspend fun verifyChainIntegrity(rootMessageId: String): Resource<ForwardChainIntegrity>

    suspend fun respondToForwardRequest(
        eventId: String,
        approved: Boolean
    ): Resource<Unit>

    suspend fun getForwardHopCount(rootMessageId: String): Int
}
