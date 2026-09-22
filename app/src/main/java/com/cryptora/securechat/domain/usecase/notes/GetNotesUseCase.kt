package com.cryptora.securechat.domain.usecase.notes

import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return noteRepository.getNotes()
    }
}
