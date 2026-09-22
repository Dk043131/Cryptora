package com.cryptora.securechat.security

import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.core.network.NetworkResult
import com.cryptora.securechat.data.remote.SecureContentApi
import com.cryptora.securechat.data.remote.SecureContentApiImpl
import com.cryptora.securechat.data.remote.ServerPolicyMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthorizationAndIdorTest {

    private lateinit var timeManager: AuthoritativeTimeManager
    private lateinit var secureContentApi: SecureContentApi

    private val ownerId = "usr_alice_owner"
    private val requesterId = "usr_bob_recipient"
    private val attackerId = "usr_eve_attacker"
    private val messageId = "msg_confidential_001"

    @Before
    fun setUp() {
        timeManager = AuthoritativeTimeManager()
        timeManager.updateServerTime(1000000L)
        secureContentApi = SecureContentApiImpl(timeManager)

        // Register secure message policy owned by Alice
        runBlocking {
            secureContentApi.registerPolicy(
                ServerPolicyMetadata(
                    messageId = messageId,
                    accessMode = "REQUEST_ACCESS",
                    expiresAt = 2000000L,
                    maxExpiresAt = 2000000L,
                    forwardingPolicy = "FORWARDING_DISABLED",
                    approvalRequired = true,
                    ownerId = ownerId
                )
            )
        }
    }

    @Test
    fun `attacker cannot approve access request owned by another user (IDOR prevention)`() = runBlocking {
        // Bob requests access
        val reqResult = secureContentApi.requestAccess(
            messageId = messageId,
            requesterId = requesterId,
            requesterUsername = "bob"
        )
        assertTrue(reqResult is NetworkResult.Success)
        val requestId = (reqResult as NetworkResult.Success).data.requestId

        // Attacker Eve attempts to approve Bob's request
        val attackerApproval = secureContentApi.respondToAccessRequest(
            requestId = requestId,
            responderUserId = attackerId,
            approved = true
        )

        // Must be rejected with 403 Forbidden
        assertTrue(attackerApproval is NetworkResult.Error)
        assertEquals(403, (attackerApproval as NetworkResult.Error).statusCode)

        // Verify Bob is still not authorized
        val validation = secureContentApi.validateAccess(messageId, requesterId)
        assertTrue(validation is NetworkResult.Success)
        assertFalse((validation as NetworkResult.Success).data.isAuthorized)

        // Legitimate owner Alice approves
        val ownerApproval = secureContentApi.respondToAccessRequest(
            requestId = requestId,
            responderUserId = ownerId,
            approved = true
        )
        assertTrue(ownerApproval is NetworkResult.Success)

        // Now Bob is authorized
        val validationAfter = secureContentApi.validateAccess(messageId, requesterId)
        assertTrue(validationAfter is NetworkResult.Success)
        assertTrue((validationAfter as NetworkResult.Success).data.isAuthorized)
    }

    @Test
    fun `attacker cannot revoke message policy owned by another user`() = runBlocking {
        // Attacker Eve attempts to revoke Alice's message policy
        val attackerRevoke = secureContentApi.revokePolicy(messageId, callerUserId = attackerId)
        assertTrue(attackerRevoke is NetworkResult.Error)
        assertEquals(403, (attackerRevoke as NetworkResult.Error).statusCode)

        // Legitimate owner Alice revokes
        val ownerRevoke = secureContentApi.revokePolicy(messageId, callerUserId = ownerId)
        assertTrue(ownerRevoke is NetworkResult.Success)

        // Verify content is now marked revoked/expired for all users
        val validation = secureContentApi.validateAccess(messageId, ownerId)
        assertTrue(validation is NetworkResult.Success)
        assertTrue((validation as NetworkResult.Success).data.isExpired)
    }

    @Test
    fun `attacker cannot snoop on third party access grants`() = runBlocking {
        val reqResult = secureContentApi.requestAccess(
            messageId = messageId,
            requesterId = requesterId,
            requesterUsername = "bob"
        )
        val requestId = (reqResult as NetworkResult.Success).data.requestId

        // Owner approves
        secureContentApi.respondToAccessRequest(requestId, ownerId, approved = true)

        // Attacker Eve tries to fetch Bob's grant
        val snoopResult = secureContentApi.getAccessGrant(messageId, granteeId = requesterId, callerUserId = attackerId)
        assertTrue(snoopResult is NetworkResult.Error)
        assertEquals(403, (snoopResult as NetworkResult.Error).statusCode)

        // Grantee Bob can fetch his own grant
        val bobGrant = secureContentApi.getAccessGrant(messageId, granteeId = requesterId, callerUserId = requesterId)
        assertTrue(bobGrant is NetworkResult.Success)
        assertNotNull((bobGrant as NetworkResult.Success).data)

        // Owner Alice can also inspect the grant
        val ownerGrant = secureContentApi.getAccessGrant(messageId, granteeId = requesterId, callerUserId = ownerId)
        assertTrue(ownerGrant is NetworkResult.Success)
        assertNotNull((ownerGrant as NetworkResult.Success).data)
    }

    @Test
    fun `attacker cannot overwrite existing message policy with new owner (Policy Hijacking Defense)`() = runBlocking {
        // Attacker Eve attempts to register the same messageId claiming ownership
        val hijackResult = secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = "IMMEDIATE_ACCESS",
                expiresAt = 9999999L,
                forwardingPolicy = "FORWARDING_ALLOWED",
                approvalRequired = false,
                ownerId = attackerId
            )
        )
        assertTrue(hijackResult is NetworkResult.Error)
        assertEquals(403, (hijackResult as NetworkResult.Error).statusCode)
    }

    @Test
    fun `access request cannot be replayed or approved multiple times`() = runBlocking {
        val reqResult = secureContentApi.requestAccess(
            messageId = messageId,
            requesterId = requesterId,
            requesterUsername = "bob"
        )
        val requestId = (reqResult as NetworkResult.Success).data.requestId

        // First approval succeeds
        val firstApproval = secureContentApi.respondToAccessRequest(requestId, ownerId, approved = true)
        assertTrue(firstApproval is NetworkResult.Success)

        // Replay attempt must be rejected
        val replayApproval = secureContentApi.respondToAccessRequest(requestId, ownerId, approved = true)
        assertTrue(replayApproval is NetworkResult.Error)
        assertEquals(409, (replayApproval as NetworkResult.Error).statusCode)
    }

    @Test
    fun `duplicate access requests for same message return existing pending request idempotently`() = runBlocking {
        val first = secureContentApi.requestAccess(messageId, requesterId, "bob")
        assertTrue(first is NetworkResult.Success)

        val second = secureContentApi.requestAccess(messageId, requesterId, "bob")
        assertTrue(second is NetworkResult.Success)

        // Must return the exact same requestId to prevent spamming
        assertEquals(
            (first as NetworkResult.Success).data.requestId,
            (second as NetworkResult.Success).data.requestId
        )
    }
}
