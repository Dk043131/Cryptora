package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.AccessGrant
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import kotlinx.coroutines.flow.Flow

data class AccessValidationResult(
    val messageId: String,
    val isAuthorized: Boolean,
    val isExpired: Boolean,
    val remainingTimeMillis: Long,
    val accessMode: String,
    val activeGrant: AccessGrant? = null
)

interface SecureContentRepository {
    suspend fun validateMessageAccess(messageId: String): Resource<AccessValidationResult>
    suspend fun requestAccess(
        messageId: String,
        requestedDuration: Long = 30L * 60L * 1000L,
        contentTitle: String = "Secure Content"
    ): Resource<AccessRequest>
    suspend fun respondToAccessRequest(
        requestId: String,
        approved: Boolean,
        finalDurationMillis: Long? = null
    ): Resource<AccessGrant?>
    suspend fun cancelAccessRequest(requestId: String): Resource<Unit>
    fun getPendingAccessRequests(): Flow<List<AccessRequest>>
    fun getAllAccessRequests(): Flow<List<AccessRequest>>
    fun getAccessRequestForMessage(messageId: String): Flow<AccessRequest?>
    suspend fun getActiveGrant(messageId: String): Resource<AccessGrant?>
    suspend fun syncAuthoritativeExpiry(): Resource<Int>
    suspend fun revokeMessageAccess(messageId: String): Resource<Unit>
    suspend fun updateMessagePolicy(messageId: String, policy: SecureMessagePolicy): Resource<Unit>
    fun getAuthoritativeServerTime(): Long
    fun observeAccessNotifications(): Flow<AccessNotificationEvent>
    suspend fun emitNotification(event: AccessNotificationEvent)
}
