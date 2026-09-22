package com.cryptora.securechat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptora.securechat.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String)

    @Query("UPDATE messages SET decryptedTextCache = :decryptedText WHERE id = :messageId")
    suspend fun updateDecryptedCache(messageId: String, decryptedText: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND deliveryStatus = 'FAILED'")
    suspend fun getFailedMessages(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE expiresAt IS NOT NULL AND expiresAt <= :currentTime AND decryptedTextCache != ''")
    suspend fun getExpiredCachedMessages(currentTime: Long): List<MessageEntity>

    @Query("UPDATE messages SET decryptedTextCache = '' WHERE expiresAt IS NOT NULL AND expiresAt <= :currentTime")
    suspend fun shredExpiredDecryptedCaches(currentTime: Long): Int

    @Query("SELECT * FROM messages WHERE decryptedTextCache LIKE '%' || :query || '%' AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY timestamp DESC")
    suspend fun searchLocalMessages(query: String, currentTime: Long): List<MessageEntity>
}
