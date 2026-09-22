package com.cryptora.securechat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptora.securechat.core.database.dao.AccessGrantDao
import com.cryptora.securechat.core.database.dao.AccessRequestDao
import com.cryptora.securechat.core.database.dao.AttachmentDao
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.dao.ForwardEventDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.dao.NoteDao
import com.cryptora.securechat.core.database.entity.AccessGrantEntity
import com.cryptora.securechat.core.database.entity.AccessRequestEntity
import com.cryptora.securechat.core.database.entity.AttachmentEntity
import com.cryptora.securechat.core.database.entity.ConversationEntity
import com.cryptora.securechat.core.database.entity.ForwardEventEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.database.entity.NoteEntity

@Database(
    entities = [
        SyncMetadataEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        NoteEntity::class,
        AccessRequestEntity::class,
        AccessGrantEntity::class,
        ForwardEventEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class CryptoraDatabase : RoomDatabase() {
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteDao(): NoteDao
    abstract fun accessRequestDao(): AccessRequestDao
    abstract fun accessGrantDao(): AccessGrantDao
    abstract fun forwardEventDao(): ForwardEventDao
}
