package com.cryptora.securechat.di

import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.StandardDispatcherProvider
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.core.navigation.AppNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppNavigator(impl: AppNavigatorImpl): AppNavigator

    companion object {
        @Provides
        @Singleton
        fun provideDispatcherProvider(): DispatcherProvider {
            return StandardDispatcherProvider()
        }
    }
}
