package com.cryptora.securechat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptora.securechat.core.database.entity.AccessGrantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessGrantDao {

    @Query("SELECT * FROM access_grants WHERE secureMessageId = :messageId AND granteeId = :granteeId AND isRevoked = 0 ORDER BY expiresAt DESC LIMIT 1")
    suspend fun getActiveGrant(messageId: String, granteeId: String): AccessGrantEntity?

    @Query("SELECT * FROM access_grants WHERE secureMessageId = :messageId AND isRevoked = 0 ORDER BY expiresAt DESC LIMIT 1")
    suspend fun getGrantByMessageId(messageId: String): AccessGrantEntity?

    @Query("SELECT * FROM access_grants WHERE grantId = :grantId LIMIT 1")
    suspend fun getGrantById(grantId: String): AccessGrantEntity?

    @Query("SELECT * FROM access_grants WHERE isRevoked = 0")
    fun getAllActiveGrantsFlow(): Flow<List<AccessGrantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(grant: AccessGrantEntity)

    @Query("UPDATE access_grants SET isRevoked = 1 WHERE grantId = :grantId")
    suspend fun revokeGrant(grantId: String)

    @Query("UPDATE access_grants SET isRevoked = 1 WHERE secureMessageId = :messageId")
    suspend fun revokeGrantsForMessage(messageId: String)

    @Query("DELETE FROM access_grants WHERE expiresAt <= :currentTime")
    suspend fun purgeExpiredGrants(currentTime: Long): Int
}
