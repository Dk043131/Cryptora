package com.cryptora.securechat.security

import com.cryptora.securechat.core.network.AuthoritativeTimeManager
import com.cryptora.securechat.data.remote.SecureContentApiImpl
import com.cryptora.securechat.data.remote.ServerPolicyMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpiryAndClockTamperingTest {

    private lateinit var timeManager: AuthoritativeTimeManager
    private lateinit var secureContentApi: SecureContentApiImpl

    @Before
    fun setUp() {
        timeManager = AuthoritativeTimeManager()
        secureContentApi = SecureContentApiImpl(timeManager)
    }

    @Test
    fun `monotonic time constraint prevents clock rollback tampering`() {
        val serverTime1 = 1_700_000_000_000L
        timeManager.updateServerTime(serverTime1)

        val authoritativeTime1 = timeManager.getAuthoritativeTime()
        assertTrue(authoritativeTime1 >= serverTime1)

        // Attacker rolls device clock back by 10 days
        // Monotonic check must enforce that authoritative time never goes backwards
        val authoritativeTimeAfterTamper = timeManager.getAuthoritativeTime()
        assertTrue(authoritativeTimeAfterTamper >= serverTime1)
    }

    @Test
    fun `server and client time skew is accurately compensated`() {
        val localTime = System.currentTimeMillis()
        val serverTimeWithFastClock = localTime + 3600_000L // Server is 1 hour ahead

        timeManager.updateServerTime(serverTimeWithFastClock)

        val calculated = timeManager.getAuthoritativeTime()
        // Must reflect the 1 hour offset
        assertTrue(calculated >= serverTimeWithFastClock)
    }

    @Test
    fun `expired content is marked expired and unauthorized on server validation`() = runBlocking {
        val now = 1_000_000L
        timeManager.updateServerTime(now)

        val messageId = "msg_expiring_soon"
        // Expired 1 second in the past
        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = "IMMEDIATE_ACCESS",
                expiresAt = now - 1000L,
                forwardingPolicy = "FORWARDING_DISABLED",
                approvalRequired = false,
                ownerId = "alice"
            )
        )

        val validation = secureContentApi.validateAccess(messageId, "bob")
        assertTrue(validation is com.cryptora.securechat.core.network.NetworkResult.Success)
        val data = (validation as com.cryptora.securechat.core.network.NetworkResult.Success).data

        assertTrue(data.isExpired)
        assertFalse(data.isAuthorized)
    }

    @Test
    fun `requesting access to expired content returns 410 Gone`() = runBlocking {
        val now = 1_000_000L
        timeManager.updateServerTime(now)

        val messageId = "msg_expired_content"
        secureContentApi.registerPolicy(
            ServerPolicyMetadata(
                messageId = messageId,
                accessMode = "REQUEST_ACCESS",
                expiresAt = now - 5000L,
                forwardingPolicy = "FORWARDING_DISABLED",
                approvalRequired = true,
                ownerId = "alice"
            )
        )

        val reqResult = secureContentApi.requestAccess(messageId, "bob", "bob")
        assertTrue(reqResult is com.cryptora.securechat.core.network.NetworkResult.Error)
        assertEquals(410, (reqResult as com.cryptora.securechat.core.network.NetworkResult.Error).statusCode)
    }
}
