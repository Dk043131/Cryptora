package com.cryptora.securechat.domain.usecase.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(mobileNumber: String, countryCode: String): Resource<String> {
        val cleanNumber = mobileNumber.trim().replace(Regex("[^0-9]"), "")
        if (cleanNumber.length < 7 || cleanNumber.length > 15) {
            return Resource.Error(AppError.Validation("Please enter a valid mobile number (7–15 digits)"))
        }
        return authRepository.sendOtp(cleanNumber, countryCode.trim())
    }
}
