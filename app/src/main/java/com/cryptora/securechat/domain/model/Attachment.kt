package com.cryptora.securechat.domain.model

data class Attachment(
    val attachmentId: String,
    val messageId: String,
    val encryptedFileReference: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val encryptedSize: Long,
    val createdAt: Long = System.currentTimeMillis()
)
