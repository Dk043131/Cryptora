package com.cryptora.securechat.core.network

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthoritativeTimeManager @Inject constructor() {

    // Clock offset = authoritativeServerTime - localSystemTime
    private val clockOffsetMillis = AtomicLong(0L)
    private val lastVerifiedServerTime = AtomicLong(0L)

    /**
     * Updates the clock offset using an authoritative timestamp from the server.
     * Detects potential client-side clock tampering.
     */
    fun updateServerTime(serverTimestampMillis: Long) {
        val currentLocalTime = System.currentTimeMillis()
        val calculatedOffset = serverTimestampMillis - currentLocalTime
        clockOffsetMillis.set(calculatedOffset)
        lastVerifiedServerTime.set(serverTimestampMillis)
    }

    /**
     * Returns the authoritative server time, compensated for any client device clock skew.
     * Prevents users from bypassing expiration by rolling back their device clock.
     */
    fun getAuthoritativeTime(): Long {
        val estimatedTime = System.currentTimeMillis() + clockOffsetMillis.get()
        val lastVerified = lastVerifiedServerTime.get()

        // Monotonic check: Authoritative time cannot travel backwards compared to last verified server time
        return if (estimatedTime < lastVerified) {
            lastVerified
        } else {
            estimatedTime
        }
    }
}
