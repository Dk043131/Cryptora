package com.cryptora.securechat.presentation.forwarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.usecase.forwarding.VerifyForwardChainIntegrityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForwardHistoryUiState(
    val messageId: String = "",
    val isLoading: Boolean = false,
    val integrity: ForwardChainIntegrity? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ForwardHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyForwardChainIntegrityUseCase: VerifyForwardChainIntegrityUseCase,
    private val navigator: AppNavigator
) : ViewModel() {

    private val messageId: String = savedStateHandle.get<String>("messageId").orEmpty()

    private val _uiState = MutableStateFlow(ForwardHistoryUiState(messageId = messageId))
    val uiState: StateFlow<ForwardHistoryUiState> = _uiState.asStateFlow()

    init {
        loadForwardHistory()
    }

    fun loadForwardHistory() {
        if (messageId.isBlank()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = verifyForwardChainIntegrityUseCase(messageId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            integrity = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onBack() {
        navigator.navigateUp()
    }
}
