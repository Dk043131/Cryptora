package com.cryptora.securechat.core.network.otp

import com.cryptora.securechat.core.activity.CurrentActivityHolder
import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production Real SMS OTP Provider using Firebase Phone Authentication.
 * Dispatches real carrier SMS verification codes globally at zero cost (10,000 free/mo).
 * Handles Play Integrity / reCAPTCHA verification, auto-retrieval, and authentic credential validation.
 */
@Singleton
class FirebasePhoneOtpProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val activityHolder: CurrentActivityHolder
) : OtpProvider {

    private val resendTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()
    private val autoCredentials = ConcurrentHashMap<String, PhoneAuthCredential>()

    private val _autoDetectedSmsCode = MutableStateFlow<String?>(null)
    val autoDetectedSmsCode: StateFlow<String?> = _autoDetectedSmsCode.asStateFlow()

    override suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String> {
        val cleanNumber = mobileNumber.filter { it.isDigit() }
        val cleanCountry = countryCode.trim()

        if (cleanNumber.length < 7) {
            return Resource.Error(AppError.Validation("Invalid mobile number format"))
        }

        // Format to strict E.164 format (+[countryCode][number])
        val formattedCountry = if (cleanCountry.startsWith("+")) cleanCountry else "+$cleanCountry"
        val fullPhoneNumber = "$formattedCountry$cleanNumber"

        val activity = activityHolder.getCurrentActivity()
            ?: return Resource.Error(
                AppError.Network("Activity not currently attached. Please reopen the application.")
            )

        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val code = credential.smsCode
                    if (!code.isNullOrBlank()) {
                        _autoDetectedSmsCode.value = code
                    }

                    val autoSessionId = "auto_${System.currentTimeMillis()}"
                    autoCredentials[autoSessionId] = credential

                    if (continuation.isActive) {
                        continuation.resume(Resource.Success(autoSessionId))
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val userFriendlyError = when {
                        e.message?.contains("app is not authorized", ignoreCase = true) == true ->
                            "Phone Auth configuration error: Ensure SHA-1 fingerprint is added in Firebase Console."
                        e.message?.contains("quota", ignoreCase = true) == true ->
                            "SMS quota exceeded. Please try again later."
                        e.message?.contains("invalid", ignoreCase = true) == true ->
                            "Invalid phone number format: $fullPhoneNumber"
                        else ->
                            e.localizedMessage ?: "Failed to dispatch SMS verification code."
                    }

                    if (continuation.isActive) {
                        continuation.resume(Resource.Error(AppError.Network(userFriendlyError)))
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    resendTokens[verificationId] = token
                    if (continuation.isActive) {
                        continuation.resume(Resource.Success(verificationId))
                    }
                }
            }

            val existingToken = resendTokens.values.firstOrNull()

            val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(fullPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (existingToken != null) {
                builder.setForceResendingToken(existingToken)
            }

            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    override suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean> {
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 6) {
            return Resource.Error(AppError.Validation("Verification code must be 6 digits"))
        }

        return suspendCancellableCoroutine { continuation ->
            // Check if already automatically verified by Google Play Services SMS Retriever
            if (sessionId.startsWith("auto_")) {
                val cachedCredential = autoCredentials.remove(sessionId)
                if (cachedCredential != null) {
                    firebaseAuth.signInWithCredential(cachedCredential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                continuation.resume(Resource.Success(true))
                            } else {
                                continuation.resume(
                                    Resource.Error(AppError.Validation(task.exception?.localizedMessage ?: "Auto-verification failed"))
                                )
                            }
                        }
                    return@suspendCancellableCoroutine
                }
            }

            // Verify with Firebase servers using real SMS code
            try {
                val credential = PhoneAuthProvider.getCredential(sessionId, cleanOtp)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            resendTokens.remove(sessionId)
                            continuation.resume(Resource.Success(true))
                        } else {
                            val msg = task.exception?.localizedMessage ?: "Invalid verification code. Please check SMS and try again."
                            continuation.resume(Resource.Error(AppError.Validation(msg)))
                        }
                    }
            } catch (e: Exception) {
                continuation.resume(
                    Resource.Error(AppError.Validation("Invalid verification code session: ${e.localizedMessage}"))
                )
            }
        }
    }

    fun clearAutoDetectedCode() {
        _autoDetectedSmsCode.value = null
    }
}
