package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun getNoteById(noteId: String): Resource<Note?>
    suspend fun createNote(title: String, body: String): Resource<Note>
    suspend fun updateNote(noteId: String, title: String, body: String, isPinned: Boolean): Resource<Note>
    suspend fun togglePinNote(noteId: String): Resource<Note>
    suspend fun deleteNote(noteId: String): Resource<Unit>
    suspend fun searchNotes(query: String): Resource<List<Note>>
    suspend fun sendNoteSecurely(
        noteId: String,
        conversationId: String,
        recipientId: String,
        policy: com.cryptora.securechat.domain.model.SecureMessagePolicy = com.cryptora.securechat.domain.model.SecureMessagePolicy()
    ): Resource<Message>

    // Legacy compatibility methods
    suspend fun saveEncryptedNote(note: Note): Resource<Note>
    suspend fun shareNoteSecurely(noteId: String, recipientUserId: String): Resource<Unit>
}
