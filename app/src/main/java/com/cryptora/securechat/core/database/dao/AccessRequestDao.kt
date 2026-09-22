package com.cryptora.securechat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptora.securechat.core.database.entity.AccessRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessRequestDao {

    @Query("SELECT * FROM access_requests WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPendingRequestsFlow(): Flow<List<AccessRequestEntity>>

    @Query("SELECT * FROM access_requests ORDER BY requestedAt DESC")
    fun getAllRequestsFlow(): Flow<List<AccessRequestEntity>>

    @Query("SELECT * FROM access_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): AccessRequestEntity?

    @Query("SELECT * FROM access_requests WHERE messageId = :messageId LIMIT 1")
    suspend fun getRequestByMessageId(messageId: String): AccessRequestEntity?

    @Query("SELECT * FROM access_requests WHERE messageId = :messageId ORDER BY requestedAt DESC LIMIT 1")
    fun getLatestRequestFlowForMessage(messageId: String): Flow<AccessRequestEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(request: AccessRequestEntity)

    @Query("UPDATE access_requests SET status = :status, respondedAt = :respondedAt WHERE requestId = :requestId")
    suspend fun updateStatus(requestId: String, status: String, respondedAt: Long = System.currentTimeMillis())

    @Query("UPDATE access_requests SET status = :status, grantedDuration = :grantedDuration, expiresAt = :expiresAt, respondedAt = :respondedAt WHERE requestId = :requestId")
    suspend fun updateStatusWithGrant(
        requestId: String,
        status: String,
        grantedDuration: Long?,
        expiresAt: Long?,
        respondedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM access_requests WHERE requestId = :requestId")
    suspend fun deleteRequest(requestId: String)
}
