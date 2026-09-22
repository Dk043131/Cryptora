package com.cryptora.securechat.presentation.chat

import android.net.Uri
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageType
import java.io.File

data class ChatUiState(
    val conversationId: String = "",
    val conversation: Conversation? = null,
    val participantName: String = "",
    val participantUsername: String = "",
    val participantAvatarUrl: String? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputMessage: String = "",
    val isSecureTimedModeEnabled: Boolean = false,
    val pendingAttachmentUri: Uri? = null,
    val pendingAttachmentName: String? = null,
    val pendingAttachmentType: MessageType? = null,
    val previewAttachmentFile: File? = null,
    val isPreviewingAttachment: Boolean = false,
    val showSecureOptionsDialog: Boolean = false,
    val selectedMessageForRequestAccess: Message? = null,
    val selectedRequestForApproval: AccessRequest? = null,
    val activeNotificationEvent: AccessNotificationEvent? = null,
    val accessRequestsMap: Map<String, AccessRequest> = emptyMap(),
    val accessRequestSuccessMessage: String? = null,
    val selectedMessageForForward: Message? = null,
    val selectedMessageForForwardHistory: Message? = null,
    val forwardChainIntegrity: ForwardChainIntegrity? = null,
    val isLoadingForwardChain: Boolean = false,
    val availableConversations: List<Conversation> = emptyList(),
    val forwardSuccessMessage: String? = null,
    val authoritativeTimeMillis: Long = System.currentTimeMillis(),
    val selectedMessageForActions: Message? = null,
    val previewingAttachment: Attachment? = null,
    val errorMessage: String? = null
)
