package com.cryptora.securechat.di

import com.cryptora.securechat.data.auth.AuthRepositoryImpl
import com.cryptora.securechat.data.chat.ChatRepositoryImpl
import com.cryptora.securechat.data.repository.ForwardingRepositoryImpl
import com.cryptora.securechat.data.message.MessageRepositoryImpl
import com.cryptora.securechat.data.notes.NoteRepositoryImpl
import com.cryptora.securechat.data.repository.SecureContentRepositoryImpl
import com.cryptora.securechat.data.user.UserRepositoryImpl
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.ForwardingRepository
import com.cryptora.securechat.domain.repository.MessageRepository
import com.cryptora.securechat.domain.repository.NoteRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import com.cryptora.securechat.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindForwardingRepository(impl: ForwardingRepositoryImpl): ForwardingRepository

    @Binds
    @Singleton
    abstract fun bindSecureContentRepository(impl: SecureContentRepositoryImpl): SecureContentRepository
}
