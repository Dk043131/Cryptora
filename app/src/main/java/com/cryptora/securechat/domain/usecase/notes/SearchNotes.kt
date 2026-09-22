package com.cryptora.securechat.domain.usecase.notes

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.repository.NoteRepository
import javax.inject.Inject

class SearchNotes @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(query: String): Resource<List<Note>> {
        return noteRepository.searchNotes(query)
    }
}
