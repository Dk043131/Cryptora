package com.cryptora.securechat.domain.usecase.notes

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.repository.NoteRepository
import javax.inject.Inject

class SendNoteSecurely @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        noteId: String,
        conversationId: String,
        recipientId: String,
        policy: com.cryptora.securechat.domain.model.SecureMessagePolicy = com.cryptora.securechat.domain.model.SecureMessagePolicy()
    ): Resource<Message> {
        return noteRepository.sendNoteSecurely(noteId, conversationId, recipientId, policy)
    }
}
