package com.cryptora.securechat.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.repository.ChatRepository
import com.cryptora.securechat.domain.repository.SecureContentRepository
import com.cryptora.securechat.domain.usecase.chat.DecryptAttachmentUseCase
import com.cryptora.securechat.domain.usecase.chat.GetConversationByIdUseCase
import com.cryptora.securechat.domain.usecase.chat.GetMessagesUseCase
import com.cryptora.securechat.domain.usecase.chat.MarkMessageReadUseCase
import com.cryptora.securechat.domain.usecase.chat.RetryMessageUseCase
import com.cryptora.securechat.domain.usecase.chat.SendAttachmentMessageUseCase
import com.cryptora.securechat.domain.usecase.chat.SendTextMessageUseCase
import com.cryptora.securechat.domain.usecase.forwarding.ForwardSecureMessageUseCase
import com.cryptora.securechat.domain.usecase.forwarding.VerifyForwardChainIntegrityUseCase
import com.cryptora.securechat.core.notification.CryptoraNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationType
import com.cryptora.securechat.domain.usecase.message.DeleteLocalMessageUseCase
import com.cryptora.securechat.domain.usecase.secure.CancelAccessRequestUseCase
import com.cryptora.securechat.domain.usecase.secure.GetAllAccessRequestsUseCase
import com.cryptora.securechat.domain.usecase.secure.RequestAccessUseCase
import com.cryptora.securechat.domain.usecase.secure.RespondToAccessRequestUseCase
import com.cryptora.securechat.domain.usecase.secure.SendSecureContentUseCase
import com.cryptora.securechat.domain.usecase.secure.ValidateAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendAttachmentMessageUseCase: SendAttachmentMessageUseCase,
    private val retryMessageUseCase: RetryMessageUseCase,
    private val markMessageReadUseCase: MarkMessageReadUseCase,
    private val decryptAttachmentUseCase: DecryptAttachmentUseCase,
    private val getConversationByIdUseCase: GetConversationByIdUseCase,
    private val sendSecureContentUseCase: SendSecureContentUseCase,
    private val validateAccessUseCase: ValidateAccessUseCase,
    private val requestAccessUseCase: RequestAccessUseCase,
    private val respondToAccessRequestUseCase: RespondToAccessRequestUseCase,
    private val cancelAccessRequestUseCase: CancelAccessRequestUseCase,
    private val getAllAccessRequestsUseCase: GetAllAccessRequestsUseCase,
    private val forwardSecureMessageUseCase: ForwardSecureMessageUseCase,
    private val verifyForwardChainIntegrityUseCase: VerifyForwardChainIntegrityUseCase,
    private val deleteLocalMessageUseCase: DeleteLocalMessageUseCase,
    val inAppNotificationManager: InAppNotificationManager,
    private val notificationManager: CryptoraNotificationManager,
    private val chatRepository: ChatRepository,
    private val secureContentRepository: SecureContentRepository,
    private val navigator: AppNavigator,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()

    private val _uiState = MutableStateFlow(ChatUiState(conversationId = conversationId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversationDetails()
        observeMessages()
        observeAccessRequests()
        observeAccessNotifications()
        startAuthoritativeTimer()
    }

    private fun startAuthoritativeTimer() {
        viewModelScope.launch(dispatchers.default) {
            while (isActive) {
                val serverTime = secureContentRepository.getAuthoritativeServerTime()
                _uiState.update { it.copy(authoritativeTimeMillis = serverTime) }
                delay(1000)
            }
        }
    }

    private fun loadConversationDetails() {
        if (conversationId.isBlank()) return

        viewModelScope.launch(dispatchers.io) {
            when (val result = getConversationByIdUseCase(conversationId)) {
                is Resource.Success -> {
                    result.data?.let { conv ->
                        _uiState.update {
                            it.copy(
                                conversation = conv,
                                participantName = conv.participantUser.fullName.ifBlank { conv.participantUser.username },
                                participantUsername = conv.participantUser.username,
                                participantAvatarUrl = conv.participantUser.avatarUrl
                            )
                        }
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            participantName = if (conversationId.startsWith("conv_")) "Secure Contact" else conversationId,
                            participantUsername = conversationId
                        )
                    }
                }
            }
        }
    }

    private fun observeMessages() {
        if (conversationId.isBlank()) return

        viewModelScope.launch(dispatchers.io) {
            getMessagesUseCase(conversationId).collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }
    }

    private fun observeAccessRequests() {
        viewModelScope.launch(dispatchers.io) {
            getAllAccessRequestsUseCase().collect { allRequests ->
                val map = allRequests.associateBy { it.secureMessageId }
                _uiState.update { it.copy(accessRequestsMap = map) }
            }
        }
    }

    private fun observeAccessNotifications() {
        viewModelScope.launch(dispatchers.io) {
            secureContentRepository.observeAccessNotifications().collect { event ->
                _uiState.update { it.copy(activeNotificationEvent = event) }
            }
        }
    }

    fun onDismissNotificationBanner() {
        _uiState.update { it.copy(activeNotificationEvent = null) }
    }

    fun onNotificationActionClick() {
        val event = _uiState.value.activeNotificationEvent ?: return
        if (event is AccessNotificationEvent.RequestReceived) {
            val req = _uiState.value.accessRequestsMap[event.requestId]
                ?: AccessRequest(
                    requestId = event.requestId,
                    secureMessageId = "",
                    requesterId = "",
                    requesterUsername = event.requesterUsername,
                    ownerId = "",
                    contentTitle = event.contentTitle,
                    requestedDuration = event.requestedDuration
                )
            _uiState.update { it.copy(selectedRequestForApproval = req, activeNotificationEvent = null) }
        } else {
            _uiState.update { it.copy(activeNotificationEvent = null) }
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(inputMessage = text) }
    }

    fun onToggleSecureMode() {
        _uiState.update { it.copy(isSecureTimedModeEnabled = !it.isSecureTimedModeEnabled) }
    }

    fun onAttachmentSelected(uri: Uri, name: String, type: MessageType) {
        _uiState.update {
            it.copy(
                pendingAttachmentUri = uri,
                pendingAttachmentName = name,
                pendingAttachmentType = type
            )
        }
    }

    fun onClearAttachment() {
        _uiState.update {
            it.copy(
                pendingAttachmentUri = null,
                pendingAttachmentName = null,
                pendingAttachmentType = null
            )
        }
    }

    fun onSendMessage() {
        val state = _uiState.value
        val pendingUri = state.pendingAttachmentUri
        val pendingType = state.pendingAttachmentType
        val text = state.inputMessage.trim()

        val recipientId = state.conversation?.participantUser?.id ?: conversationId

        if (pendingUri != null && pendingType != null) {
            viewModelScope.launch(dispatchers.io) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                when (val result = sendAttachmentMessageUseCase(conversationId, recipientId, pendingUri, pendingType)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingAttachmentUri = null,
                                pendingAttachmentName = null,
                                pendingAttachmentType = null
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
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            }
            return
        }

        if (text.isBlank()) return

        _uiState.update { it.copy(inputMessage = "") }

        viewModelScope.launch(dispatchers.io) {
            when (val result = sendTextMessageUseCase(conversationId, recipientId, text)) {
                is Resource.Success -> {
                    inAppNotificationManager.notifyMessageSent(isSecure = false)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
                else -> Unit
            }
        }
    }

    fun onRetryMessage(messageId: String) {
        viewModelScope.launch(dispatchers.io) {
            retryMessageUseCase(messageId)
        }
    }

    fun onAttachmentClick(attachment: Attachment) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = decryptAttachmentUseCase(attachment)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            previewAttachmentFile = result.data,
                            isPreviewingAttachment = true
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
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onClosePreview() {
        _uiState.update {
            it.copy(
                previewAttachmentFile = null,
                isPreviewingAttachment = false
            )
        }
    }

    fun onOpenSecureOptions() {
        _uiState.update { it.copy(showSecureOptionsDialog = true) }
    }

    fun onCloseSecureOptions() {
        _uiState.update { it.copy(showSecureOptionsDialog = false) }
    }

    fun onSendSecureMessage(policy: SecureMessagePolicy) {
        val state = _uiState.value
        val pendingUri = state.pendingAttachmentUri
        val pendingType = state.pendingAttachmentType
        val text = state.inputMessage.trim()
        val recipientId = state.conversation?.participantUser?.id ?: conversationId

        _uiState.update { it.copy(showSecureOptionsDialog = false, isLoading = true, errorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            val result = if (pendingUri != null && pendingType != null) {
                sendSecureContentUseCase(
                    conversationId = conversationId,
                    recipientId = recipientId,
                    content = text,
                    messageType = pendingType,
                    attachmentUri = pendingUri,
                    policy = policy
                )
            } else {
                sendSecureContentUseCase(
                    conversationId = conversationId,
                    recipientId = recipientId,
                    content = text,
                    messageType = MessageType.TEXT,
                    policy = policy
                )
            }

            when (result) {
                is Resource.Success -> {
                    inAppNotificationManager.notifyMessageSent(isSecure = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputMessage = "",
                            pendingAttachmentUri = null,
                            pendingAttachmentName = null,
                            pendingAttachmentType = null
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
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Access Request UI Handlers
    fun onOpenRequestAccessDialog(message: Message) {
        _uiState.update { it.copy(selectedMessageForRequestAccess = message) }
    }

    fun onDismissRequestAccessDialog() {
        _uiState.update { it.copy(selectedMessageForRequestAccess = null) }
    }

    fun onSubmitAccessRequest(messageId: String, requestedDuration: Long, contentTitle: String) {
        _uiState.update { it.copy(selectedMessageForRequestAccess = null, isLoading = true, errorMessage = null) }
        viewModelScope.launch(dispatchers.io) {
            when (val result = requestAccessUseCase(messageId, requestedDuration, contentTitle)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestSuccessMessage = "Access request sent to message owner."
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
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCancelAccessRequest(requestId: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = cancelAccessRequestUseCase(requestId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestSuccessMessage = "Access request cancelled."
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
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onOpenApprovalDialog(request: AccessRequest) {
        _uiState.update { it.copy(selectedRequestForApproval = request) }
    }

    fun onDismissApprovalDialog() {
        _uiState.update { it.copy(selectedRequestForApproval = null) }
    }

    fun onApproveAccessRequest(requestId: String, finalDurationMillis: Long) {
        _uiState.update { it.copy(selectedRequestForApproval = null, isLoading = true, errorMessage = null) }
        viewModelScope.launch(dispatchers.io) {
            when (val result = respondToAccessRequestUseCase(requestId, approved = true, finalDurationMillis = finalDurationMillis)) {
                is Resource.Success -> {
                    val mins = finalDurationMillis / 60000L
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestSuccessMessage = "Access granted for $mins minutes."
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

    fun onRejectAccessRequest(requestId: String) {
        _uiState.update { it.copy(selectedRequestForApproval = null, isLoading = true, errorMessage = null) }
        viewModelScope.launch(dispatchers.io) {
            when (val result = respondToAccessRequestUseCase(requestId, approved = false, finalDurationMillis = null)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestSuccessMessage = "Access request rejected."
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

    // Forwarding Handlers
    fun onOpenForwardDialog(message: Message) {
        viewModelScope.launch(dispatchers.io) {
            val convs = chatRepository.getConversations().firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    selectedMessageForForward = message,
                    availableConversations = convs.filter { c -> c.id != conversationId }
                )
            }
        }
    }

    fun onDismissForwardDialog() {
        _uiState.update { it.copy(selectedMessageForForward = null) }
    }

    fun onForwardMessage(targetConversationId: String, targetUserId: String, targetUsername: String) {
        val message = _uiState.value.selectedMessageForForward ?: return
        _uiState.update { it.copy(selectedMessageForForward = null, isLoading = true, errorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = forwardSecureMessageUseCase(message.id, targetConversationId, targetUserId, targetUsername)) {
                is Resource.Success -> {
                    inAppNotificationManager.notifyForwardSuccess(targetUsername)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            forwardSuccessMessage = "Message forwarded to @$targetUsername."
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
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onOpenForwardHistory(message: Message) {
        val rootId = message.rootMessageId.ifBlank { message.id }
        _uiState.update {
            it.copy(
                selectedMessageForForwardHistory = message,
                isLoadingForwardChain = true,
                forwardChainIntegrity = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            when (val result = verifyForwardChainIntegrityUseCase(rootId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingForwardChain = false,
                            forwardChainIntegrity = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingForwardChain = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoadingForwardChain = false) }
            }
        }
    }

    fun onDismissForwardHistory() {
        _uiState.update {
            it.copy(
                selectedMessageForForwardHistory = null,
                forwardChainIntegrity = null,
                isLoadingForwardChain = false
            )
        }
    }

    fun dismissForwardSuccessMessage() {
        _uiState.update { it.copy(forwardSuccessMessage = null) }
    }

    fun onValidateAccess(messageId: String) {
        viewModelScope.launch(dispatchers.io) {
            validateAccessUseCase(messageId)
        }
    }

    fun dismissAccessSuccessMessage() {
        _uiState.update { it.copy(accessRequestSuccessMessage = null) }
    }

    fun onMessageLongClick(message: Message) {
        _uiState.update { it.copy(selectedMessageForActions = message) }
    }

    fun onDismissMessageActions() {
        _uiState.update { it.copy(selectedMessageForActions = null) }
    }

    fun onPreviewAttachment(attachment: Attachment) {
        _uiState.update { it.copy(previewingAttachment = attachment) }
    }

    fun onDismissAttachmentPreview() {
        _uiState.update { it.copy(previewingAttachment = null, isPreviewingAttachment = false) }
    }

    fun onCopyMessageText(message: Message, context: android.content.Context) {
        val text = message.decryptedTextCache
        if (!text.isNullOrBlank()) {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Cryptora Message", text)
            clipboard?.setPrimaryClip(clip)
            inAppNotificationManager.showNotification(
                title = "✓ Copied to clipboard",
                type = InAppNotificationType.SUCCESS,
                durationMillis = 2000L
            )
        }
        _uiState.update { it.copy(selectedMessageForActions = null) }
    }

    fun onDeleteLocalMessage(message: Message) {
        viewModelScope.launch(dispatchers.io) {
            deleteLocalMessageUseCase(message.id)
            _uiState.update { it.copy(selectedMessageForActions = null) }
            inAppNotificationManager.showNotification(
                title = "🗑️ Message deleted locally",
                type = InAppNotificationType.INFO,
                durationMillis = 2500L
            )
        }
    }

    fun onBackClicked() {
        navigator.navigateUp()
    }
}
