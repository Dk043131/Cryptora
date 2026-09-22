package com.cryptora.securechat.di

import com.cryptora.securechat.BuildConfig
import com.cryptora.securechat.core.network.otp.DevOtpProvider
import com.cryptora.securechat.core.network.otp.OtpProvider
import com.cryptora.securechat.core.network.otp.ProductionSmsProvider
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
    fun provideOtpProvider(
        devOtpProvider: DevOtpProvider,
        productionSmsProvider: ProductionSmsProvider
    ): OtpProvider {
        return if (BuildConfig.DEBUG) {
            devOtpProvider
        } else {
            productionSmsProvider
        }
    }
}
