package com.cryptora.securechat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptora.securechat.core.database.entity.ForwardEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardEventDao {

    @Query("SELECT * FROM forward_events WHERE rootMessageId = :rootMessageId ORDER BY timestamp ASC")
    fun getEventsForRootFlow(rootMessageId: String): Flow<List<ForwardEventEntity>>

    @Query("SELECT * FROM forward_events WHERE rootMessageId = :rootMessageId ORDER BY timestamp ASC")
    suspend fun getEventsForRoot(rootMessageId: String): List<ForwardEventEntity>

    @Query("SELECT * FROM forward_events WHERE secureMessageId = :secureMessageId ORDER BY timestamp ASC")
    suspend fun getEventsForMessage(secureMessageId: String): List<ForwardEventEntity>

    @Query("SELECT * FROM forward_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): ForwardEventEntity?

    @Query("SELECT * FROM forward_events WHERE rootMessageId = :rootMessageId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEventForRoot(rootMessageId: String): ForwardEventEntity?

    @Query("SELECT COUNT(*) FROM forward_events WHERE rootMessageId = :rootMessageId AND approvalStatus = 'APPROVED'")
    suspend fun getApprovedHopCount(rootMessageId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ForwardEventEntity)

    @Query("UPDATE forward_events SET approvalStatus = :status WHERE eventId = :eventId")
    suspend fun updateStatus(eventId: String, status: String)

    @Query("DELETE FROM forward_events WHERE rootMessageId = :rootMessageId")
    suspend fun deleteEventsForRoot(rootMessageId: String)
}
