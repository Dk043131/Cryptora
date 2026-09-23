package com.cryptora.securechat.di

import com.cryptora.securechat.BuildConfig
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.LOG_NETWORK_CALLS) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                // Keep headers only; do not log encrypted payload bodies to prevent metadata leakage
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideCryptoraApiService(retrofit: Retrofit): com.cryptora.securechat.data.remote.CryptoraApiService {
        return retrofit.create(com.cryptora.securechat.data.remote.CryptoraApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSecureContentApi(impl: com.cryptora.securechat.data.remote.SecureContentApiImpl): com.cryptora.securechat.data.remote.SecureContentApi = impl
}
