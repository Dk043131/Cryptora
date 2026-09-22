package com.cryptora.securechat.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val lastSyncTimestamp: Long,
    val syncStatus: String
)
