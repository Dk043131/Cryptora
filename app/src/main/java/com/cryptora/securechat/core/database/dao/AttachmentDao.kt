package com.cryptora.securechat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptora.securechat.core.database.entity.AttachmentEntity

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE messageId = :messageId LIMIT 1")
    suspend fun getAttachmentByMessageId(messageId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentByMessageId(messageId: String)
}
