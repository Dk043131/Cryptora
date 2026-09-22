package com.cryptora.securechat.di

import android.content.Context
import androidx.room.Room
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.database.CryptoraDatabase
import com.cryptora.securechat.core.database.SyncMetadataDao
import com.cryptora.securechat.core.database.dao.AttachmentDao
import com.cryptora.securechat.core.database.dao.ConversationDao
import com.cryptora.securechat.core.database.dao.MessageDao
import com.cryptora.securechat.core.database.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `attachments` (
                    `attachmentId` TEXT NOT NULL,
                    `messageId` TEXT NOT NULL,
                    `encryptedFileReference` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `size` INTEGER NOT NULL,
                    `encryptedSize` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`attachmentId`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `notes` (
                    `id` TEXT NOT NULL,
                    `encryptedTitleBase64` TEXT NOT NULL,
                    `encryptedBodyBase64` TEXT NOT NULL,
                    `keyAlias` TEXT NOT NULL,
                    `isPinned` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `access_requests` (
                    `requestId` TEXT NOT NULL,
                    `messageId` TEXT NOT NULL,
                    `conversationId` TEXT NOT NULL,
                    `requesterId` TEXT NOT NULL,
                    `requesterUsername` TEXT NOT NULL,
                    `contentTitle` TEXT NOT NULL,
                    `requestedDuration` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `requestedAt` INTEGER NOT NULL,
                    `respondedAt` INTEGER,
                    `grantedDuration` INTEGER,
                    `expiresAt` INTEGER,
                    PRIMARY KEY(`requestId`)
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `access_grants` (
                    `grantId` TEXT NOT NULL,
                    `requestId` TEXT NOT NULL,
                    `secureMessageId` TEXT NOT NULL,
                    `granteeId` TEXT NOT NULL,
                    `grantedDuration` INTEGER NOT NULL,
                    `grantedAt` INTEGER NOT NULL,
                    `expiresAt` INTEGER NOT NULL,
                    `isRevoked` INTEGER NOT NULL,
                    PRIMARY KEY(`grantId`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `policyVersion` INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `isAccessGranted` INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `rootMessageId` TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `forwardCount` INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `isForwarded` INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE `messages` ADD COLUMN `originalSenderUsername` TEXT") } catch (_: Exception) {}
        }
    }

    val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `forward_events` (
                    `eventId` TEXT NOT NULL,
                    `rootMessageId` TEXT NOT NULL,
                    `secureMessageId` TEXT NOT NULL,
                    `parentEventId` TEXT,
                    `fromUserId` TEXT NOT NULL,
                    `fromUsername` TEXT NOT NULL,
                    `toUserId` TEXT NOT NULL,
                    `toUsername` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `policyVersion` INTEGER NOT NULL,
                    `approvalStatus` TEXT NOT NULL,
                    `previousEventHash` TEXT NOT NULL,
                    `eventHash` TEXT NOT NULL,
                    `signatureMetadata` TEXT NOT NULL,
                    PRIMARY KEY(`eventId`)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_events_rootMessageId` ON `forward_events` (`rootMessageId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_events_secureMessageId` ON `forward_events` (`secureMessageId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_events_timestamp` ON `forward_events` (`timestamp`)")
        }
    }

    @Provides
    @Singleton
    fun provideCryptoraDatabase(@ApplicationContext context: Context): CryptoraDatabase {
        return Room.databaseBuilder(
            context,
            CryptoraDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: CryptoraDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: CryptoraDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: CryptoraDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideAttachmentDao(database: CryptoraDatabase): AttachmentDao {
        return database.attachmentDao()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: CryptoraDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideAccessRequestDao(database: CryptoraDatabase): com.cryptora.securechat.core.database.dao.AccessRequestDao {
        return database.accessRequestDao()
    }

    @Provides
    @Singleton
    fun provideAccessGrantDao(database: CryptoraDatabase): com.cryptora.securechat.core.database.dao.AccessGrantDao {
        return database.accessGrantDao()
    }

    @Provides
    @Singleton
    fun provideForwardEventDao(database: CryptoraDatabase): com.cryptora.securechat.core.database.dao.ForwardEventDao {
        return database.forwardEventDao()
    }
}
