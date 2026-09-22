package com.cryptora.securechat.domain.usecase.notes

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.repository.NoteRepository
import javax.inject.Inject

class UpdateNote @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        noteId: String,
        title: String,
        body: String,
        isPinned: Boolean
    ): Resource<Note> {
        return noteRepository.updateNote(noteId, title, body, isPinned)
    }
}
