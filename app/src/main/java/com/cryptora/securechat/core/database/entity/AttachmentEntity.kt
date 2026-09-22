package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    indices = [
        Index("messageId")
    ]
)
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val messageId: String,
    val encryptedFileReference: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val encryptedSize: Long,
    val createdAt: Long = System.currentTimeMillis()
)
