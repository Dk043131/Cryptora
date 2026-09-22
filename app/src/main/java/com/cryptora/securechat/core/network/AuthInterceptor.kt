package com.cryptora.securechat.core.network

import com.cryptora.securechat.core.security.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {

    companion object {
        const val KEY_AUTH_TOKEN = "auth_session_token"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = secureStorage.getString(KEY_AUTH_TOKEN)

        val authenticatedRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("X-Client-Platform", "Android")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("X-Client-Platform", "Android")
                .build()
        }

        return chain.proceed(authenticatedRequest)
    }
}
