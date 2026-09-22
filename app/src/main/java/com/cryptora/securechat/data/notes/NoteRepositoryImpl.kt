package com.cryptora.securechat.data.notes

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.NoteDao
import com.cryptora.securechat.core.database.entity.NoteEntity
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.repository.MessageRepository
import com.cryptora.securechat.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val cryptoManager: CryptoManager,
    private val messageRepository: MessageRepository,
    private val dispatchers: DispatcherProvider
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getNotesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(noteId: String): Resource<Note?> = withContext(dispatchers.io) {
        val entity = noteDao.getNoteById(noteId)
        Resource.Success(entity?.toDomain())
    }

    override suspend fun createNote(title: String, body: String): Resource<Note> = withContext(dispatchers.io) {
        try {
            val noteId = "note_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val encTitle = cryptoManager.encryptString(title, Constants.MASTER_KEY_ALIAS)
            val encBody = cryptoManager.encryptString(body, Constants.MASTER_KEY_ALIAS)

            val now = System.currentTimeMillis()
            val entity = NoteEntity(
                id = noteId,
                encryptedTitleBase64 = encTitle,
                encryptedBodyBase64 = encBody,
                keyAlias = Constants.MASTER_KEY_ALIAS,
                isPinned = false,
                createdAt = now,
                updatedAt = now
            )

            noteDao.insertOrUpdate(entity)

            Resource.Success(
                Note(
                    id = noteId,
                    title = title,
                    body = body,
                    isPinned = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Failed to encrypt and save note: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun updateNote(
        noteId: String,
        title: String,
        body: String,
        isPinned: Boolean
    ): Resource<Note> = withContext(dispatchers.io) {
        try {
            val existing = noteDao.getNoteById(noteId)
                ?: return@withContext Resource.Error(AppError.NotFound("Note not found"))

            val encTitle = cryptoManager.encryptString(title, existing.keyAlias)
            val encBody = cryptoManager.encryptString(body, existing.keyAlias)
            val now = System.currentTimeMillis()

            val updated = existing.copy(
                encryptedTitleBase64 = encTitle,
                encryptedBodyBase64 = encBody,
                isPinned = isPinned,
                updatedAt = now
            )

            noteDao.insertOrUpdate(updated)

            Resource.Success(
                Note(
                    id = noteId,
                    title = title,
                    body = body,
                    isPinned = isPinned,
                    createdAt = existing.createdAt,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Failed to update note: ${e.localizedMessage}"), e)
        }
    }

    override suspend fun togglePinNote(noteId: String): Resource<Note> = withContext(dispatchers.io) {
        val existing = noteDao.getNoteById(noteId)
            ?: return@withContext Resource.Error(AppError.NotFound("Note not found"))

        val newPinStatus = !existing.isPinned
        val now = System.currentTimeMillis()
        noteDao.updatePinStatus(noteId, newPinStatus, now)

        val updated = existing.copy(isPinned = newPinStatus, updatedAt = now)
        Resource.Success(updated.toDomain())
    }

    override suspend fun deleteNote(noteId: String): Resource<Unit> = withContext(dispatchers.io) {
        noteDao.deleteNote(noteId)
        Resource.Success(Unit)
    }

    override suspend fun searchNotes(query: String): Resource<List<Note>> = withContext(dispatchers.io) {
        val allNotes = getNotes().firstOrNull() ?: emptyList()
        if (query.isBlank()) {
            return@withContext Resource.Success(allNotes)
        }

        val filtered = allNotes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
        Resource.Success(filtered)
    }

    override suspend fun sendNoteSecurely(
        noteId: String,
        conversationId: String,
        recipientId: String,
        policy: com.cryptora.securechat.domain.model.SecureMessagePolicy
    ): Resource<Message> = withContext(dispatchers.io) {
        val existing = noteDao.getNoteById(noteId)
            ?: return@withContext Resource.Error(AppError.NotFound("Note $noteId not found"))

        val note = existing.toDomain()
        val formattedPayload = buildString {
            append("📝 **${note.title}**")
            if (note.body.isNotBlank()) {
                append("\n\n")
                append(note.body)
            }
        }

        // Forward through existing MessageRepository using end-to-end chat encryption & policy
        messageRepository.sendTextMessage(conversationId, recipientId, formattedPayload, policy)
    }

    override suspend fun saveEncryptedNote(note: Note): Resource<Note> = withContext(dispatchers.io) {
        createNote(note.title, note.body)
    }

    override suspend fun shareNoteSecurely(noteId: String, recipientUserId: String): Resource<Unit> = withContext(dispatchers.io) {
        Resource.Success(Unit)
    }

    private fun NoteEntity.toDomain(): Note {
        val decryptedTitle = try {
            cryptoManager.decryptString(encryptedTitleBase64, keyAlias)
        } catch (e: Exception) {
            "[Encrypted Title]"
        }

        val decryptedBody = try {
            cryptoManager.decryptString(encryptedBodyBase64, keyAlias)
        } catch (e: Exception) {
            "[Encrypted Content]"
        }

        return Note(
            id = id,
            title = decryptedTitle,
            body = decryptedBody,
            isPinned = isPinned,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
