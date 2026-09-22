package com.cryptora.securechat.domain.model

import com.cryptora.securechat.core.common.Constants

data class Note(
    val id: String,
    val title: String,
    val body: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerId: String = "me",
    val encryptedContentBase64: String = "",
    val encryptionMetadata: EncryptionMetadata = EncryptionMetadata(
        initializationVectorBase64 = "",
        keyAlias = Constants.MASTER_KEY_ALIAS
    ),
    val isShared: Boolean = false,
    val sharedWithUsernames: List<String> = emptyList()
)
