package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(sessionId: String, otp: String): Resource<Boolean> {
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 6 || !cleanOtp.all { it.isDigit() }) {
            return Resource.Error(AppError.Validation("Please enter a valid 6-digit OTP"))
        }
        if (sessionId.isBlank()) {
            return Resource.Error(AppError.Validation("Invalid OTP verification session"))
        }
        return authRepository.verifyOtp(sessionId, cleanOtp)
    }
}
