package com.cryptora.securechat.di

import com.cryptora.securechat.core.network.otp.FirebasePhoneOtpProvider
import com.cryptora.securechat.core.network.otp.OtpProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideOtpProvider(
        firebasePhoneOtpProvider: FirebasePhoneOtpProvider
    ): OtpProvider {
        return firebasePhoneOtpProvider
    }
}
