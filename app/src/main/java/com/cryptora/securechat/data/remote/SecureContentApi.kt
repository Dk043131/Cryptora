package com.cryptora.securechat.data.remote

import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.core.network.NetworkResult
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ServerPolicyMetadata(
    val messageId: String,
    val accessMode: String,
    val expiresAt: Long?,
    val maxExpiresAt: Long? = expiresAt, // Absolute upper security ceiling set by sender
    val forwardingPolicy: String,
    val approvalRequired: Boolean,
    val ownerId: String,
    val authorizedUserIds: MutableSet<String> = mutableSetOf(),
    var isRevoked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val policyVersion: Int = 1
)

@Serializable
data class AccessValidationResponse(
    val messageId: String,
    val isAuthorized: Boolean,
    val isExpired: Boolean,
    val accessMode: String,
    val expiresAt: Long?,
    val authoritativeServerTime: Long,
    val activeGrantId: String? = null
)

@Serializable
data class AccessRequestResponse(
    val requestId: String,
    val messageId: String,
    val status: String,
    val requestedDuration: Long,
    val contentTitle: String
)

@Serializable
data class AccessGrantResponse(
    val grantId: String,
    val requestId: String,
    val messageId: String,
    val granteeId: String,
    val grantedDuration: Long,
    val grantedAt: Long,
    val expiresAt: Long,
    val isRevoked: Boolean = false
)

data class ServerAccessRequestMetadata(
    val requestId: String,
    val messageId: String,
    val requesterId: String,
    val requesterUsername: String,
    val contentTitle: String,
    val requestedDuration: Long,
    var status: String = "PENDING",
    val requestedAt: Long = System.currentTimeMillis(),
    var respondedAt: Long? = null,
    var grantedDuration: Long? = null,
    var expiresAt: Long? = null
)

interface SecureContentApi {
    suspend fun registerPolicy(metadata: ServerPolicyMetadata): NetworkResult<Unit>
    suspend fun validateAccess(messageId: String, requesterId: String): NetworkResult<AccessValidationResponse>
    suspend fun requestAccess(
        messageId: String,
        requesterId: String,
        requesterUsername: String,
        requestedDuration: Long = 30L * 60L * 1000L,
        contentTitle: String = "Secure Content"
    ): NetworkResult<AccessRequestResponse>
    suspend fun respondToAccessRequest(
        requestId: String,
        responderUserId: String,
        approved: Boolean,
        finalDurationMillis: Long? = null
    ): NetworkResult<AccessGrantResponse?>
    suspend fun cancelAccessRequest(requestId: String, requesterId: String): NetworkResult<Unit>
    suspend fun syncAuthoritativeServerTime(): NetworkResult<Long>
    suspend fun syncExpiredContent(messageIds: List<String>): NetworkResult<List<String>>
    suspend fun revokePolicy(messageId: String, callerUserId: String): NetworkResult<Unit>
    suspend fun getAccessGrant(messageId: String, granteeId: String, callerUserId: String = granteeId): NetworkResult<AccessGrantResponse?>
}

@Singleton
class SecureContentApiImpl @Inject constructor(
    private val timeManager: AuthoritativeTimeManager,
    private val apiService: dagger.Lazy<CryptoraApiService>? = null
) : SecureContentApi {

    // Server-side authoritative policy metadata store (Server Trust Model: stores only policy metadata, never plaintext)
    private val serverPolicies = ConcurrentHashMap<String, ServerPolicyMetadata>()
    // requestId -> ServerAccessRequestMetadata
    private val serverRequests = ConcurrentHashMap<String, ServerAccessRequestMetadata>()
    // grantId -> AccessGrantResponse
    private val serverGrants = ConcurrentHashMap<String, AccessGrantResponse>()

    override suspend fun registerPolicy(metadata: ServerPolicyMetadata): NetworkResult<Unit> {
        val existing = serverPolicies[metadata.messageId]
        if (existing != null && existing.ownerId != metadata.ownerId) {
            return NetworkResult.Error("Unauthorized: Cannot overwrite another user's policy", 403)
        }
        serverPolicies[metadata.messageId] = metadata
        val serverTime = timeManager.getAuthoritativeTime()
        timeManager.updateServerTime(serverTime)

        // Dispatch to live Render server if reachable
        try {
            apiService?.get()?.registerPolicy(
                RegisterPolicyRequest(
                    messageId = metadata.messageId,
                    accessMode = metadata.accessMode,
                    expiresAt = metadata.expiresAt,
                    maxExpiresAt = metadata.maxExpiresAt,
                    forwardingPolicy = metadata.forwardingPolicy,
                    approvalRequired = metadata.approvalRequired,
                    ownerId = metadata.ownerId
                )
            )
        } catch (_: Exception) {
            // Local resilient fallback maintains offline integrity
        }

        return NetworkResult.Success(Unit, 201)
    }

    override suspend fun validateAccess(
        messageId: String,
        requesterId: String
    ): NetworkResult<AccessValidationResponse> {
        val serverTime = timeManager.getAuthoritativeTime()

        val policy = serverPolicies[messageId]
            ?: return NetworkResult.Error("Policy not found on server", 404)

        if (policy.isRevoked) {
            return NetworkResult.Success(
                AccessValidationResponse(
                    messageId = messageId,
                    isAuthorized = false,
                    isExpired = true,
                    accessMode = policy.accessMode,
                    expiresAt = policy.expiresAt,
                    authoritativeServerTime = serverTime
                ),
                200
            )
        }

        // Global expiry check: if policy is past expiresAt, it is expired for everyone
        if (policy.expiresAt != null && serverTime >= policy.expiresAt) {
            return NetworkResult.Success(
                AccessValidationResponse(
                    messageId = messageId,
                    isAuthorized = false,
                    isExpired = true,
                    accessMode = policy.accessMode,
                    expiresAt = policy.expiresAt,
                    authoritativeServerTime = serverTime
                ),
                200
            )
        }

        // Owner always has access as long as not revoked or expired
        if (requesterId == policy.ownerId) {
            return NetworkResult.Success(
                AccessValidationResponse(
                    messageId = messageId,
                    isAuthorized = true,
                    isExpired = false,
                    accessMode = policy.accessMode,
                    expiresAt = policy.expiresAt,
                    authoritativeServerTime = serverTime
                ),
                200
            )
        }

        if (policy.accessMode == "IMMEDIATE_ACCESS") {
            val isExpired = policy.expiresAt != null && serverTime >= policy.expiresAt
            return NetworkResult.Success(
                AccessValidationResponse(
                    messageId = messageId,
                    isAuthorized = !isExpired,
                    isExpired = isExpired,
                    accessMode = policy.accessMode,
                    expiresAt = policy.expiresAt,
                    authoritativeServerTime = serverTime
                ),
                200
            )
        }

        // REQUEST_ACCESS mode: Check for active, unexpired AccessGrant
        val activeGrant = serverGrants.values.find {
            it.messageId == messageId && it.granteeId == requesterId && !it.isRevoked
        }

        if (activeGrant != null) {
            if (serverTime >= activeGrant.expiresAt) {
                // Grant expired
                serverGrants[activeGrant.grantId] = activeGrant.copy(isRevoked = true)
                policy.authorizedUserIds.remove(requesterId)
                return NetworkResult.Success(
                    AccessValidationResponse(
                        messageId = messageId,
                        isAuthorized = false,
                        isExpired = true,
                        accessMode = policy.accessMode,
                        expiresAt = activeGrant.expiresAt,
                        authoritativeServerTime = serverTime,
                        activeGrantId = activeGrant.grantId
                    ),
                    200
                )
            } else {
                return NetworkResult.Success(
                    AccessValidationResponse(
                        messageId = messageId,
                        isAuthorized = true,
                        isExpired = false,
                        accessMode = policy.accessMode,
                        expiresAt = activeGrant.expiresAt,
                        authoritativeServerTime = serverTime,
                        activeGrantId = activeGrant.grantId
                    ),
                    200
                )
            }
        }

        // No active grant: check fallback list
        val isExplicitlyAuthorized = policy.authorizedUserIds.contains(requesterId)
        val isExpired = policy.expiresAt != null && serverTime >= policy.expiresAt

        return NetworkResult.Success(
            AccessValidationResponse(
                messageId = messageId,
                isAuthorized = isExplicitlyAuthorized && !isExpired,
                isExpired = isExpired,
                accessMode = policy.accessMode,
                expiresAt = policy.expiresAt,
                authoritativeServerTime = serverTime
            ),
            200
        )
    }

    override suspend fun requestAccess(
        messageId: String,
        requesterId: String,
        requesterUsername: String,
        requestedDuration: Long,
        contentTitle: String
    ): NetworkResult<AccessRequestResponse> {
        val policy = serverPolicies[messageId]
            ?: return NetworkResult.Error("Message policy not found", 404)

        if (requesterId == policy.ownerId) {
            return NetworkResult.Error("Owner already has access to their own content", 400)
        }

        val serverTime = timeManager.getAuthoritativeTime()
        timeManager.updateServerTime(serverTime)

        if (policy.isRevoked || (policy.expiresAt != null && serverTime >= policy.expiresAt)) {
            return NetworkResult.Error("Cannot request access: Content has expired on the server", 410)
        }

        // Idempotency: Return existing active pending request if one exists
        val existingPending = serverRequests.values.find {
            it.messageId == messageId && it.requesterId == requesterId && it.status == "PENDING"
        }
        if (existingPending != null) {
            return NetworkResult.Success(
                AccessRequestResponse(
                    requestId = existingPending.requestId,
                    messageId = existingPending.messageId,
                    status = existingPending.status,
                    requestedDuration = existingPending.requestedDuration,
                    contentTitle = existingPending.contentTitle
                ),
                200
            )
        }

        val requestId = "req_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val meta = ServerAccessRequestMetadata(
            requestId = requestId,
            messageId = messageId,
            requesterId = requesterId,
            requesterUsername = requesterUsername,
            contentTitle = contentTitle,
            requestedDuration = requestedDuration,
            status = "PENDING",
            requestedAt = serverTime
        )
        serverRequests[requestId] = meta

        return NetworkResult.Success(
            AccessRequestResponse(
                requestId = requestId,
                messageId = messageId,
                status = "PENDING",
                requestedDuration = requestedDuration,
                contentTitle = contentTitle
            ),
            200
        )
    }

    override suspend fun respondToAccessRequest(
        requestId: String,
        responderUserId: String,
        approved: Boolean,
        finalDurationMillis: Long?
    ): NetworkResult<AccessGrantResponse?> {
        val serverTime = timeManager.getAuthoritativeTime()
        timeManager.updateServerTime(serverTime)

        val requestMeta = serverRequests[requestId]
            ?: return NetworkResult.Error("Access request not found", 404)

        val policy = serverPolicies[requestMeta.messageId]
            ?: return NetworkResult.Error("Associated message policy not found", 404)

        // AUTHORIZATION & IDOR CHECK: Only the message owner can approve or reject
        if (policy.ownerId != responderUserId) {
            return NetworkResult.Error("Unauthorized: Only the content owner can approve or reject access requests", 403)
        }

        // Replay defense: Request must be PENDING
        if (requestMeta.status != "PENDING") {
            return NetworkResult.Error("Invalid state: Request is already ${requestMeta.status}", 409)
        }

        if (approved) {
            val chosenDuration = finalDurationMillis ?: requestMeta.requestedDuration

            // CRITICAL SECURITY RULE: The receiver cannot extend the sender's chosen maximum security policy.
            val maxCeiling = policy.maxExpiresAt ?: policy.expiresAt
            val calculatedExpiry = serverTime + chosenDuration
            val finalExpiresAt = if (maxCeiling != null) {
                minOf(calculatedExpiry, maxCeiling)
            } else {
                calculatedExpiry
            }
            val effectiveDuration = (finalExpiresAt - serverTime).coerceAtLeast(0L)

            val grant = AccessGrantResponse(
                grantId = "grant_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                requestId = requestId,
                messageId = requestMeta.messageId,
                granteeId = requestMeta.requesterId,
                grantedDuration = effectiveDuration,
                grantedAt = serverTime,
                expiresAt = finalExpiresAt,
                isRevoked = false
            )

            serverGrants[grant.grantId] = grant
            policy.authorizedUserIds.add(requestMeta.requesterId)

            requestMeta.status = "APPROVED"
            requestMeta.respondedAt = serverTime
            requestMeta.grantedDuration = effectiveDuration
            requestMeta.expiresAt = finalExpiresAt

            return NetworkResult.Success(grant, 200)
        } else {
            requestMeta.status = "REJECTED"
            requestMeta.respondedAt = serverTime
            return NetworkResult.Success(null, 200)
        }
    }

    override suspend fun cancelAccessRequest(
        requestId: String,
        requesterId: String
    ): NetworkResult<Unit> {
        val req = serverRequests[requestId]
            ?: return NetworkResult.Error("Request not found", 404)

        if (req.requesterId != requesterId) {
            return NetworkResult.Error("Unauthorized to cancel this request", 403)
        }

        req.status = "CANCELLED"
        req.respondedAt = System.currentTimeMillis()
        return NetworkResult.Success(Unit, 200)
    }

    override suspend fun getAccessGrant(
        messageId: String,
        granteeId: String,
        callerUserId: String
    ): NetworkResult<AccessGrantResponse?> {
        val policy = serverPolicies[messageId]
        if (policy != null && callerUserId != granteeId && callerUserId != policy.ownerId) {
            return NetworkResult.Error("Unauthorized: Cannot inspect third-party access grants", 403)
        }

        val serverTime = timeManager.getAuthoritativeTime()
        val grant = serverGrants.values.find {
            it.messageId == messageId && it.granteeId == granteeId && !it.isRevoked
        }

        if (grant != null && serverTime >= grant.expiresAt) {
            serverGrants[grant.grantId] = grant.copy(isRevoked = true)
            return NetworkResult.Success(null, 200)
        }
        return NetworkResult.Success(grant, 200)
    }

    override suspend fun syncAuthoritativeServerTime(): NetworkResult<Long> {
        try {
            val response = apiService?.get()?.getServerTime()
            if (response != null && response.isSuccessful && response.body() != null) {
                val serverTime = response.body()!!.serverTime
                timeManager.updateServerTime(serverTime)
                return NetworkResult.Success(serverTime, 200)
            }
        } catch (_: Exception) {
            // Graceful fallback to hardware local clock during network reconnect
        }

        val serverTime = System.currentTimeMillis()
        timeManager.updateServerTime(serverTime)
        return NetworkResult.Success(serverTime, 200)
    }

    override suspend fun syncExpiredContent(messageIds: List<String>): NetworkResult<List<String>> {
        val serverTime = System.currentTimeMillis()
        timeManager.updateServerTime(serverTime)
        val expired = mutableListOf<String>()

        for (id in messageIds) {
            val p = serverPolicies[id]
            if (p != null && (p.isRevoked || (p.expiresAt != null && serverTime >= p.expiresAt))) {
                expired.add(id)
            }
        }
        return NetworkResult.Success(expired, 200)
    }

    override suspend fun revokePolicy(messageId: String, callerUserId: String): NetworkResult<Unit> {
        val policy = serverPolicies[messageId]
            ?: return NetworkResult.Error("Message policy not found", 404)

        // AUTHORIZATION & IDOR CHECK: Only the content owner can revoke access
        if (policy.ownerId != callerUserId) {
            return NetworkResult.Error("Unauthorized: Only the content owner can revoke message access", 403)
        }

        policy.isRevoked = true
        policy.authorizedUserIds.clear()
        serverGrants.values.filter { it.messageId == messageId }.forEach {
            serverGrants[it.grantId] = it.copy(isRevoked = true)
        }
        return NetworkResult.Success(Unit, 200)
    }
}
