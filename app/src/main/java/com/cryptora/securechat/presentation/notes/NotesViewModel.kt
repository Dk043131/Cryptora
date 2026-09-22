package com.cryptora.securechat.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.NoteRepository
import com.cryptora.securechat.domain.usecase.notes.CreateNote
import com.cryptora.securechat.domain.usecase.notes.DeleteNote
import com.cryptora.securechat.domain.usecase.notes.GetNotesUseCase
import com.cryptora.securechat.domain.usecase.notes.SendNoteSecurely
import com.cryptora.securechat.domain.usecase.notes.UpdateNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNote,
    private val updateNoteUseCase: UpdateNote,
    private val deleteNoteUseCase: DeleteNote,
    private val sendNoteSecurelyUseCase: SendNoteSecurely,
    private val noteRepository: NoteRepository,
    private val chatRepository: ChatRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        observeNotes()
        observeConversations()
    }

    private fun observeNotes() {
        viewModelScope.launch(dispatchers.io) {
            getNotesUseCase().collect { allNotes ->
                _uiState.update { state ->
                    val query = state.searchQuery.trim()
                    val filtered = if (query.isBlank()) {
                        allNotes
                    } else {
                        allNotes.filter {
                            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
                        }
                    }
                    state.copy(notes = allNotes, filteredNotes = filtered)
                }
            }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch(dispatchers.io) {
            chatRepository.getConversations().collect { convList ->
                _uiState.update { it.copy(availableConversations = convList) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val trimmed = query.trim()
            val filtered = if (trimmed.isBlank()) {
                state.notes
            } else {
                state.notes.filter {
                    it.title.contains(trimmed, ignoreCase = true) || it.body.contains(trimmed, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredNotes = filtered)
        }
    }

    fun onStartCreateNote() {
        _uiState.update {
            it.copy(
                isEditorOpen = true,
                isCreatingNew = true,
                activeNote = null,
                editTitle = "",
                editBody = "",
                errorMessage = null
            )
        }
    }

    fun onOpenNote(note: Note) {
        _uiState.update {
            it.copy(
                isEditorOpen = true,
                isCreatingNew = false,
                activeNote = note,
                editTitle = note.title,
                editBody = note.body,
                errorMessage = null
            )
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun onBodyChange(body: String) {
        _uiState.update { it.copy(editBody = body) }
    }

    fun onSaveNote() {
        val state = _uiState.value
        val title = state.editTitle.trim()
        val body = state.editBody.trim()

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Note title cannot be empty") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.isCreatingNew || state.activeNote == null) {
                createNoteUseCase(title, body)
            } else {
                updateNoteUseCase(
                    noteId = state.activeNote.id,
                    title = title,
                    body = body,
                    isPinned = state.activeNote.isPinned
                )
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditorOpen = false,
                            activeNote = null,
                            editTitle = "",
                            editBody = "",
                            statusMessage = "Note saved securely (AES-256)"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTogglePin(noteId: String) {
        viewModelScope.launch(dispatchers.io) {
            noteRepository.togglePinNote(noteId)
        }
    }

    fun onRequestDelete(note: Note) {
        _uiState.update {
            it.copy(noteToDelete = note, showDeleteDialog = true)
        }
    }

    fun onConfirmDelete() {
        val note = _uiState.value.noteToDelete ?: return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true) }
            deleteNoteUseCase(note.id)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showDeleteDialog = false,
                    noteToDelete = null,
                    isEditorOpen = if (it.activeNote?.id == note.id) false else it.isEditorOpen,
                    statusMessage = "Note shredded and deleted"
                )
            }
        }
    }

    fun onCancelDelete() {
        _uiState.update {
            it.copy(showDeleteDialog = false, noteToDelete = null)
        }
    }

    fun onRequestSendSecurely(note: Note) {
        _uiState.update {
            it.copy(noteToSendSecurely = note, showSendSecurelyDialog = true)
        }
    }

    fun onSelectConversationForSend(conversation: Conversation) {
        _uiState.update {
            it.copy(
                selectedConversationForSend = conversation,
                showSendSecurelyDialog = false,
                showPolicyOptionsDialog = true
            )
        }
    }

    fun onConfirmSendWithPolicy(policy: com.cryptora.securechat.domain.model.SecureMessagePolicy) {
        val note = _uiState.value.noteToSendSecurely ?: return
        val conversation = _uiState.value.selectedConversationForSend ?: return

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, showPolicyOptionsDialog = false) }
            val result = sendNoteSecurelyUseCase(
                noteId = note.id,
                conversationId = conversation.id,
                recipientId = conversation.participantUser.id,
                policy = policy
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            noteToSendSecurely = null,
                            selectedConversationForSend = null,
                            isEditorOpen = false,
                            statusMessage = "Note securely sent to @${conversation.participantUser.username}"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to send note: ${result.error.message}"
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCancelPolicyOptions() {
        _uiState.update {
            it.copy(
                showPolicyOptionsDialog = false,
                selectedConversationForSend = null,
                noteToSendSecurely = null
            )
        }
    }

    fun onConfirmSendSecurely(conversation: Conversation) {
        onSelectConversationForSend(conversation)
    }

    fun onCancelSendSecurely() {
        _uiState.update {
            it.copy(showSendSecurelyDialog = false, noteToSendSecurely = null, selectedConversationForSend = null)
        }
    }

    fun onCloseEditor() {
        _uiState.update {
            it.copy(
                isEditorOpen = false,
                activeNote = null,
                editTitle = "",
                editBody = "",
                errorMessage = null
            )
        }
    }

    fun clearStatus() {
        _uiState.update {
            it.copy(statusMessage = null, errorMessage = null)
        }
    }
}
