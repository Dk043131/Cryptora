package com.cryptora.securechat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index("isPinned"),
        Index("updatedAt")
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val encryptedTitleBase64: String,
    val encryptedBodyBase64: String,
    val keyAlias: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
