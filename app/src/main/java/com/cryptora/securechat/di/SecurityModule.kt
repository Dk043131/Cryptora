package com.cryptora.securechat.di

import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.CryptoManagerImpl
import com.cryptora.securechat.core.security.DeviceIdentityManager
import com.cryptora.securechat.core.security.DeviceIdentityManagerImpl
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.KeyManagerImpl
import com.cryptora.securechat.core.security.SecureStorage
import com.cryptora.securechat.core.security.SecureStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeyManager(impl: KeyManagerImpl): KeyManager

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: CryptoManagerImpl): CryptoManager

    @Binds
    @Singleton
    abstract fun bindDeviceIdentityManager(impl: DeviceIdentityManagerImpl): DeviceIdentityManager

    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: SecureStorageImpl): SecureStorage
}
