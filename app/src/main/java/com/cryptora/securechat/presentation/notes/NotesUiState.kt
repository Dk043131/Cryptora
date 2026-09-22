package com.cryptora.securechat.presentation.notes

import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.Note

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val activeNote: Note? = null,
    val isEditorOpen: Boolean = false,
    val isCreatingNew: Boolean = false,
    val editTitle: String = "",
    val editBody: String = "",
    val noteToDelete: Note? = null,
    val showDeleteDialog: Boolean = false,
    val noteToSendSecurely: Note? = null,
    val showSendSecurelyDialog: Boolean = false,
    val availableConversations: List<Conversation> = emptyList(),
    val selectedConversationForSend: Conversation? = null,
    val showPolicyOptionsDialog: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
