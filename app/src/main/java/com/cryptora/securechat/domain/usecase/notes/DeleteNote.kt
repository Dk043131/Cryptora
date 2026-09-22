package com.cryptora.securechat.domain.usecase.notes

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNote @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String): Resource<Unit> {
        return noteRepository.deleteNote(noteId)
    }
}
