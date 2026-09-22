package com.cryptora.securechat.core.network.otp

import com.cryptora.securechat.core.common.Resource

interface OtpProvider {
    /**
     * Sends OTP to given mobile number and country code.
     * Returns a unique sessionId / reference token for verification.
     */
    suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String>

    /**
     * Verifies the provided 6-digit OTP against the sessionId.
     */
    suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean>
}
