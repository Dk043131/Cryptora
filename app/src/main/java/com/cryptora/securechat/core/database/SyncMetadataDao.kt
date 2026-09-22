package com.cryptora.securechat.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getSyncMetadata(key: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncMetadata(entity: SyncMetadataEntity)
}
