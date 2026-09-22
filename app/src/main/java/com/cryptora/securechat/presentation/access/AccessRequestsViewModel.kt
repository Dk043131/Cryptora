package com.cryptora.securechat.presentation.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.AccessRequestStatus
import com.cryptora.securechat.domain.usecase.secure.GetAllAccessRequestsUseCase
import com.cryptora.securechat.domain.usecase.secure.GetPendingAccessRequestsUseCase
import com.cryptora.securechat.domain.usecase.secure.RespondToAccessRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccessRequestsUiState(
    val requests: List<AccessRequest> = emptyList(),
    val selectedFilter: AccessRequestFilter = AccessRequestFilter.ALL,
    val selectedRequestForApproval: AccessRequest? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

enum class AccessRequestFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    EXPIRED("Expired")
}

@HiltViewModel
class AccessRequestsViewModel @Inject constructor(
    private val getAllAccessRequestsUseCase: GetAllAccessRequestsUseCase,
    private val respondToAccessRequestUseCase: RespondToAccessRequestUseCase,
    private val navigator: AppNavigator,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessRequestsUiState())
    val uiState: StateFlow<AccessRequestsUiState> = _uiState.asStateFlow()

    init {
        observeRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch(dispatchers.io) {
            getAllAccessRequestsUseCase().collect { allRequests ->
                _uiState.update { it.copy(requests = allRequests) }
            }
        }
    }

    fun setFilter(filter: AccessRequestFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun openApprovalDialog(request: AccessRequest) {
        _uiState.update { it.copy(selectedRequestForApproval = request) }
    }

    fun dismissApprovalDialog() {
        _uiState.update { it.copy(selectedRequestForApproval = null) }
    }

    fun onApproveWithDuration(requestId: String, finalDurationMillis: Long) {
        dismissApprovalDialog()
        respondToRequest(requestId, approved = true, finalDurationMillis = finalDurationMillis)
    }

    fun onRejectRequest(requestId: String) {
        dismissApprovalDialog()
        respondToRequest(requestId, approved = false, finalDurationMillis = null)
    }

    private fun respondToRequest(requestId: String, approved: Boolean, finalDurationMillis: Long?) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, feedbackMessage = null) }
            when (val result = respondToAccessRequestUseCase(requestId, approved, finalDurationMillis)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackMessage = if (approved) {
                                val mins = (finalDurationMillis ?: 0L) / 60000L
                                "Access granted for $mins minutes. Recipient can now decrypt."
                            } else {
                                "Access request rejected."
                            }
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

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    fun onBack() {
        navigator.navigateUp()
    }
}
