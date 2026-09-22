package com.cryptora.securechat.presentation.home

import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.core.navigation.Screen
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.core.notification.CryptoraNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationType
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.repository.AuthRepository
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.NoteRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import com.cryptora.securechat.domain.usecase.auth.LogoutUserUseCase
import com.cryptora.securechat.presentation.common.BaseViewModel
import com.cryptora.securechat.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab {
    CHATS,
    SEARCH,
    PROFILE,
    SETTINGS
}

data class HomeDashboardData(
    val currentUser: User?,
    val conversations: List<Conversation>,
    val pendingAccessRequestsCount: Int,
    val totalNotesCount: Int,
    val isHardwareKeystoreActive: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
    private val secureContentRepository: SecureContentRepository,
    val inAppNotificationManager: InAppNotificationManager,
    private val notificationManager: CryptoraNotificationManager,
    navigator: AppNavigator,
    dispatchers: DispatcherProvider
) : BaseViewModel(dispatchers, navigator) {

    private val _selectedTab = MutableStateFlow(HomeTab.CHATS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<HomeDashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeDashboardData>> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch(dispatchers.default) {
            secureContentRepository.observeAccessNotifications().collect { event ->
                when (event) {
                    is AccessNotificationEvent.RequestReceived -> {
                        inAppNotificationManager.notifyAccessRequestReceived(event.requesterUsername, event.contentTitle)
                        notificationManager.showAccessRequestNotification(event.requesterUsername, event.contentTitle, event.requestId)
                    }
                    is AccessNotificationEvent.RequestApproved -> {
                        inAppNotificationManager.showNotification("🔓 Access Granted", event.message, InAppNotificationType.SUCCESS)
                        notificationManager.showAccessApprovedNotification(event.contentTitle, event.requestId)
                    }
                    is AccessNotificationEvent.RequestRejected -> {
                        inAppNotificationManager.showNotification("🚫 Access Declined", event.message, InAppNotificationType.WARNING)
                        notificationManager.showAccessRejectedNotification(event.contentTitle, event.requestId)
                    }
                    is AccessNotificationEvent.AccessExpired -> {
                        inAppNotificationManager.notifyExpired()
                        notificationManager.showExpiredNotification(event.contentTitle, event.secureMessageId)
                    }
                    is AccessNotificationEvent.AccessRevoked -> {
                        inAppNotificationManager.showNotification("🛑 Access Revoked", event.message, InAppNotificationType.DANGER)
                        notificationManager.showAccessRevokedNotification(event.contentTitle)
                    }
                }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun loadDashboardData() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = UiState.Loading
            try {
                combine(
                    authRepository.getAuthenticatedUser(),
                    chatRepository.getConversations(),
                    secureContentRepository.getPendingAccessRequests(),
                    noteRepository.getNotes()
                ) { user, convs, requests, notes ->
                    HomeDashboardData(
                        currentUser = user,
                        conversations = convs,
                        pendingAccessRequestsCount = requests.size,
                        totalNotesCount = notes.size,
                        isHardwareKeystoreActive = true
                    )
                }.collect { dashboardData ->
                    _uiState.value = UiState.Success(dashboardData)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to initialize secure dashboard: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch(dispatchers.io) {
            logoutUserUseCase()
            navigator.popUpTo(Screen.Home.route, inclusive = true)
            navigator.navigateTo(Screen.Auth.route)
        }
    }

    fun onConversationClicked(conversationId: String) {
        navigateTo(Screen.Chat.createRoute(conversationId))
    }

    fun onAccessRequestsClicked() {
        navigateTo(Screen.AccessRequests.route)
    }

    fun onNotesClicked() {
        navigateTo(Screen.NoteEditor.createRoute("new"))
    }
}
