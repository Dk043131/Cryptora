package com.cryptora.securechat.core.network.otp

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevOtpProvider @Inject constructor() : OtpProvider {

    companion object {
        const val DEV_TEST_OTP = "123456"
        private const val OTP_EXPIRY_MILLIS = 5 * 60 * 1000L // 5 minutes
    }

    private data class OtpSession(
        val mobileNumber: String,
        val generatedOtp: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val activeSessions = ConcurrentHashMap<String, OtpSession>()
    private val secureRandom = SecureRandom()

    override suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String> {
        delay(350) // Simulate fast network dispatch

        if (mobileNumber.length < 7) {
            return Resource.Error(AppError.Validation("Invalid mobile number format"))
        }

        val generatedOtp = (100000 + secureRandom.nextInt(900000)).toString()
        val sessionId = "otp_dev_sess_${System.currentTimeMillis()}_${secureRandom.nextInt(1000)}"

        activeSessions[sessionId] = OtpSession(
            mobileNumber = "$countryCode$mobileNumber",
            generatedOtp = generatedOtp
        )

        // Zero sensitive data leakage in production logs, only returns reference sessionId
        return Resource.Success(sessionId)
    }

    override suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean> {
        delay(250) // Simulate verification handshake

        val cleanOtp = otp.trim()
        val session = activeSessions[sessionId]
            ?: return if (com.cryptora.securechat.BuildConfig.DEBUG && cleanOtp == DEV_TEST_OTP) {
                // Allow universal dev bypass code only in debug builds for local automated testing
                Resource.Success(true)
            } else {
                Resource.Error(AppError.Expired("OTP session expired or not found. Please request a new code."))
            }

        if (System.currentTimeMillis() - session.createdAt > OTP_EXPIRY_MILLIS) {
            activeSessions.remove(sessionId)
            return Resource.Error(AppError.Expired("OTP has expired. Please tap Resend."))
        }

        val isValid = cleanOtp == session.generatedOtp || (com.cryptora.securechat.BuildConfig.DEBUG && cleanOtp == DEV_TEST_OTP)
        return if (isValid) {
            activeSessions.remove(sessionId)
            Resource.Success(true)
        } else {
            Resource.Error(AppError.Validation("Incorrect OTP code. Try again."))
        }
    }
}
