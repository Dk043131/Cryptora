package com.cryptora.securechat.presentation.search

import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.core.navigation.Screen
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.model.SearchFilterTab
import com.cryptora.securechat.domain.model.UnifiedSearchResult
import com.cryptora.securechat.domain.model.UserProfile
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.usecase.notes.SearchNotes
import com.cryptora.securechat.domain.usecase.search.SearchConversationsUseCase
import com.cryptora.securechat.domain.usecase.search.SearchLocalMessagesUseCase
import com.cryptora.securechat.domain.usecase.user.SearchUsersUseCase
import com.cryptora.securechat.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val result: UnifiedSearchResult) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val searchLocalMessagesUseCase: SearchLocalMessagesUseCase,
    private val searchNotesUseCase: SearchNotes,
    private val searchConversationsUseCase: SearchConversationsUseCase,
    private val chatRepository: ChatRepository,
    navigator: AppNavigator,
    dispatchers: DispatcherProvider
) : BaseViewModel(dispatchers, navigator) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(SearchFilterTab.ALL)
    val selectedTab: StateFlow<SearchFilterTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        observeDebouncedSearch()
    }

    private fun observeDebouncedSearch() {
        viewModelScope.launch(dispatchers.default) {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    fun clearQuery() {
        _searchQuery.value = ""
        _uiState.value = SearchUiState.Idle
    }

    fun selectTab(tab: SearchFilterTab) {
        _selectedTab.value = tab
    }

    private suspend fun executeSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            if (clean.isEmpty()) {
                _uiState.value = SearchUiState.Idle
            } else {
                _uiState.value = SearchUiState.Error("Type at least 2 characters to search")
            }
            return
        }

        _uiState.value = SearchUiState.Loading

        val cleanUsername = clean.removePrefix("@")

        // Execute searches concurrently across domains
        val usersDeferred = viewModelScope.async(dispatchers.io) {
            searchUsersUseCase(cleanUsername).getOrNull() ?: emptyList()
        }
        val messagesDeferred = viewModelScope.async(dispatchers.io) {
            searchLocalMessagesUseCase(clean).getOrNull() ?: emptyList()
        }
        val notesDeferred = viewModelScope.async(dispatchers.io) {
            searchNotesUseCase(clean).getOrNull() ?: emptyList()
        }
        val convsDeferred = viewModelScope.async(dispatchers.io) {
            searchConversationsUseCase(clean).getOrNull() ?: emptyList()
        }

        val users = usersDeferred.await()
        val messages = messagesDeferred.await()
        val notes = notesDeferred.await()
        val convs = convsDeferred.await()

        val unifiedResult = UnifiedSearchResult(
            query = clean,
            users = users,
            messages = messages,
            notes = notes,
            conversations = convs
        )

        if (unifiedResult.isEmpty) {
            _uiState.value = SearchUiState.Empty(clean)
        } else {
            _uiState.value = SearchUiState.Success(unifiedResult)
        }
    }

    fun startChat(user: UserProfile) {
        viewModelScope.launch(dispatchers.io) {
            val convResult = chatRepository.getOrCreateConversation(user.id)
            convResult.onSuccess { conversation ->
                navigator.navigateTo(Screen.Chat.createRoute(conversation.id))
            }
        }
    }

    fun openConversation(conversationId: String) {
        navigator.navigateTo(Screen.Chat.createRoute(conversationId))
    }
}
