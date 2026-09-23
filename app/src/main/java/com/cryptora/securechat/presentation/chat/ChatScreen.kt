package com.cryptora.securechat.presentation.chat

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraExpiryBadge
import com.cryptora.securechat.domain.model.AccessNotificationEvent
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.AccessRequestStatus
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.presentation.access.AccessNotificationBanner
import com.cryptora.securechat.presentation.access.ApproveAccessRequestDialog
import com.cryptora.securechat.presentation.access.RequestAccessDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.cryptora.securechat.presentation.chat.components.AttachmentPreviewDialog
import com.cryptora.securechat.presentation.chat.components.MessageActionsBottomSheet
import com.cryptora.securechat.presentation.components.InAppNotificationHost
import com.cryptora.securechat.presentation.forwarding.ForwardHistoryDialog
import com.cryptora.securechat.presentation.forwarding.ForwardMessageDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showAttachmentPickerSheet by remember { mutableStateOf(false) }

    // Content pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "image_${System.currentTimeMillis()}.jpg"
            viewModel.onAttachmentSelected(it, fileName, MessageType.IMAGE)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "file_${System.currentTimeMillis()}"
            viewModel.onAttachmentSelected(it, fileName, MessageType.FILE)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        containerColor = CryptoraColors.DeepNavyBackground,
        topBar = {
            ChatTopAppBar(
                participantName = uiState.participantName,
                participantUsername = uiState.participantUsername,
                onBackClicked = viewModel::onBackClicked
            )
        },
        bottomBar = {
            ChatInputBar(
                inputMessage = uiState.inputMessage,
                isSecureTimedMode = uiState.isSecureTimedModeEnabled,
                pendingAttachmentName = uiState.pendingAttachmentName,
                pendingAttachmentType = uiState.pendingAttachmentType,
                isLoading = uiState.isLoading,
                onMessageChange = viewModel::onMessageChange,
                onSend = viewModel::onSendMessage,
                onAttachClick = { showAttachmentPickerSheet = true },
                onToggleSecureMode = viewModel::onOpenSecureOptions,
                onClearAttachment = viewModel::onClearAttachment
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyChatConversation(participantName = uiState.participantName)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val accessReq = uiState.accessRequestsMap[message.id]
                        ChatMessageItem(
                            message = message,
                            accessRequest = accessReq,
                            authoritativeTimeMillis = uiState.authoritativeTimeMillis,
                            onRequestAccess = { viewModel.onOpenRequestAccessDialog(message) },
                            onCancelRequest = { accessReq?.let { viewModel.onCancelAccessRequest(it.requestId) } },
                            onRetry = { viewModel.onRetryMessage(message.id) },
                            onAttachmentClick = { message.attachment?.let { viewModel.onPreviewAttachment(it) } },
                            onOpenForwardHistory = { viewModel.onOpenForwardHistory(message) },
                            onForwardClick = { viewModel.onOpenForwardDialog(message) },
                            onLongClick = { viewModel.onMessageLongClick(message) }
                        )
                    }
                }
            }

            // Centralized In-App Notification Host Overlay
            InAppNotificationHost(
                notificationManager = viewModel.inAppNotificationManager,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Realtime In-App Notification Banner for access requests/grants/revocations
            AccessNotificationBanner(
                event = uiState.activeNotificationEvent,
                onDismiss = viewModel::onDismissNotificationBanner,
                onActionClick = viewModel::onNotificationActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            // Forward success feedback banner
            uiState.forwardSuccessMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .clickable { viewModel.dismissForwardSuccessMessage() },
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.ElectricCyan.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CryptoraColors.DeepNavyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = CryptoraColors.DeepNavyBackground,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Access Request feedback banner
            uiState.accessRequestSuccessMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .clickable { viewModel.dismissAccessSuccessMessage() },
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.EmeraldSafe.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error banner if any
            uiState.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.CrimsonDanger.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // Send Secure Options Dialog (Flagship Time-Based Access)
    if (uiState.showSecureOptionsDialog) {
        SendSecureOptionsDialog(
            recipientName = uiState.participantName,
            recipientUsername = uiState.participantUsername,
            contentPreview = uiState.pendingAttachmentName
                ?: uiState.inputMessage.ifBlank { "Encrypted text payload" },
            onConfirm = viewModel::onSendSecureMessage,
            onDismiss = viewModel::onCloseSecureOptions
        )
    }

    // Attachment Source BottomSheet
    if (showAttachmentPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentPickerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = CryptoraColors.NavyCardBackground
        ) {
            AttachmentPickerContent(
                onPickImage = {
                    showAttachmentPickerSheet = false
                    imagePickerLauncher.launch("image/*")
                },
                onPickFile = {
                    showAttachmentPickerSheet = false
                    filePickerLauncher.launch("*/*")
                }
            )
        }
    }

    // Decrypted attachment preview dialog
    uiState.previewingAttachment?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            onDismiss = viewModel::onDismissAttachmentPreview
        )
    }

    // Message Actions Context Menu (Triggered on Long-Press)
    uiState.selectedMessageForActions?.let { msg ->
        MessageActionsBottomSheet(
            message = msg,
            onDismiss = viewModel::onDismissMessageActions,
            onCopy = { viewModel.onCopyMessageText(msg, context) },
            onForward = {
                viewModel.onDismissMessageActions()
                viewModel.onOpenForwardDialog(msg)
            },
            onViewHistory = {
                viewModel.onDismissMessageActions()
                viewModel.onOpenForwardHistory(msg)
            },
            onRetry = {
                viewModel.onDismissMessageActions()
                viewModel.onRetryMessage(msg.id)
            },
            onDelete = { viewModel.onDeleteLocalMessage(msg) }
        )
    }

    // Receiver: Request Access Dialog
    uiState.selectedMessageForRequestAccess?.let { msg ->
        val title = msg.attachment?.fileName
            ?: if (msg.decryptedTextCache.isNullOrBlank() || msg.decryptedTextCache.startsWith("[")) "Secure Message" else msg.decryptedTextCache.take(30)
        RequestAccessDialog(
            contentTitle = title,
            senderUsername = uiState.participantUsername,
            onConfirm = { duration ->
                viewModel.onSubmitAccessRequest(msg.id, duration, title)
            },
            onDismiss = viewModel::onDismissRequestAccessDialog
        )
    }

    // Sender: Approve Access Request Dialog
    uiState.selectedRequestForApproval?.let { req ->
        ApproveAccessRequestDialog(
            request = req,
            onApprove = { duration ->
                viewModel.onApproveAccessRequest(req.requestId, duration)
            },
            onReject = {
                viewModel.onRejectAccessRequest(req.requestId)
            },
            onDismiss = viewModel::onDismissApprovalDialog
        )
    }

    // Forward Message Dialog
    uiState.selectedMessageForForward?.let { msg ->
        ForwardMessageDialog(
            message = msg,
            conversations = uiState.availableConversations,
            onForwardToConversation = { convId, userId, username ->
                viewModel.onForwardMessage(convId, userId, username)
            },
            onDismiss = viewModel::onDismissForwardDialog
        )
    }

    // Forward History Dialog
    uiState.selectedMessageForForwardHistory?.let { msg ->
        ForwardHistoryDialog(
            originalOwnerUsername = msg.originalSenderUsername ?: msg.senderId,
            integrity = uiState.forwardChainIntegrity,
            isLoading = uiState.isLoadingForwardChain,
            onDismiss = viewModel::onDismissForwardHistory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    participantName: String,
    participantUsername: String,
    onBackClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Participant avatar placeholder
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f))
                        .border(1.dp, CryptoraColors.ElectricCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participantName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = participantName.ifBlank { "Encrypted Contact" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CryptoraColors.EmeraldSafe)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (participantUsername.isNotBlank()) "@$participantUsername • AES-256" else "End-to-End Encrypted",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.TextSecondary
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CryptoraColors.TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CryptoraColors.SurfaceNavy
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: Message,
    accessRequest: AccessRequest?,
    authoritativeTimeMillis: Long,
    onRequestAccess: () -> Unit,
    onCancelRequest: () -> Unit,
    onRetry: () -> Unit,
    onAttachmentClick: () -> Unit,
    onOpenForwardHistory: () -> Unit,
    onForwardClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    val isExpired = message.isExpiredOrRevoked || (message.policy.expiresAt != null && authoritativeTimeMillis >= message.policy.expiresAt)
    val isLocked = message.policy.isLocked && !isExpired
    val hasActiveCountdown = !isExpired && !isLocked && message.policy.expiresAt != null
    val remainingMillis = if (hasActiveCountdown) (message.policy.expiresAt!! - authoritativeTimeMillis).coerceAtLeast(0L) else 0L

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
        }

        val bubbleBackground = when {
            isExpired -> CryptoraColors.CrimsonDanger.copy(alpha = 0.12f)
            isLocked -> CryptoraColors.SurfaceNavy
            isOutgoing -> CryptoraColors.ElectricCyan.copy(alpha = 0.22f)
            else -> CryptoraColors.NavyCardBackground
        }

        val bubbleBorder = when {
            isExpired -> CryptoraColors.CrimsonDanger.copy(alpha = 0.5f)
            isLocked -> CryptoraColors.AmberWarning.copy(alpha = 0.5f)
            isOutgoing -> CryptoraColors.ElectricCyan.copy(alpha = 0.45f)
            else -> CryptoraColors.BorderSubtle
        }

        Box(
            modifier = Modifier
                .widthIn(min = 72.dp, max = 295.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = { /* Message tapped */ },
                    onLongClick = onLongClick
                )
                .background(bubbleBackground)
                .border(1.dp, bubbleBorder, bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (message.isForwarded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (!message.originalSenderUsername.isNullOrBlank()) {
                                "Forwarded from @${message.originalSenderUsername}"
                            } else {
                                "Forwarded message"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.ElectricCyan,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                if (isLocked) {
                    // State 1: Locked
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CryptoraColors.AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔒 Secure Content",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CryptoraColors.AmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        if (isOutgoing) {
                            Text(
                                text = "Access requires your approval when recipient requests.",
                                fontSize = 12.sp,
                                color = CryptoraColors.TextSecondary
                            )
                            if (accessRequest?.status == AccessRequestStatus.PENDING) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CryptoraColors.AmberWarning.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, CryptoraColors.AmberWarning)
                                ) {
                                    Text(
                                        text = "Pending request from @${accessRequest.requesterUsername}",
                                        color = CryptoraColors.AmberWarning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // Receiver view: handle pending, rejected, or initial not granted state
                            when (accessRequest?.status) {
                                AccessRequestStatus.PENDING -> {
                                    Text(
                                        text = "⏳ Access Requested (Pending approval)",
                                        fontSize = 12.sp,
                                        color = CryptoraColors.AmberWarning,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onCancelRequest,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, CryptoraColors.CrimsonDanger.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "Cancel Request",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CryptoraColors.CrimsonDanger
                                        )
                                    }
                                }
                                AccessRequestStatus.REJECTED -> {
                                    Text(
                                        text = "🚫 Access Request Rejected",
                                        fontSize = 12.sp,
                                        color = CryptoraColors.CrimsonDanger,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Owner declined access to this message.",
                                        fontSize = 11.sp,
                                        color = CryptoraColors.TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onRequestAccess,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CryptoraColors.ElectricCyan),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "Request Again",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CryptoraColors.DeepNavyBackground
                                        )
                                    }
                                }
                                else -> {
                                    // Not granted yet / cancelled
                                    Text(
                                        text = "Access not granted",
                                        fontSize = 12.sp,
                                        color = CryptoraColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onRequestAccess,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CryptoraColors.ElectricCyan),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "Request Access",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CryptoraColors.DeepNavyBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isExpired) {
                    // State 3: Expired
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CryptoraColors.CrimsonDanger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🔒 Access Expired",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CryptoraColors.CrimsonDanger
                            )
                            Text(
                                text = "Authoritative time elapsed. Keys permanently shredded.",
                                fontSize = 10.sp,
                                color = CryptoraColors.TextMuted
                            )
                        }
                    }
                } else {
                    // State 2: Active (with Timelock countdown or standard immediate access)
                    if (hasActiveCountdown) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CryptoraColors.DeepNavyBackground.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🔐 Secure Content",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CryptoraColors.ElectricCyan
                                )
                                CryptoraExpiryBadge(remainingMillis = remainingMillis)
                            }
                        }
                    }

                    // Attachment Section
                    message.attachment?.let { attachment ->
                        AttachmentBubbleCard(
                            attachment = attachment,
                            messageType = message.messageType,
                            onClick = onAttachmentClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Text Content
                    val displayContent = message.decryptedTextCache
                        ?: if (message.messageType == MessageType.TEXT) "[Encrypted Message]" else ""

                    if (displayContent.isNotBlank() && !(message.attachment != null && displayContent.startsWith("["))) {
                        Text(
                            text = displayContent,
                            color = CryptoraColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Compact Metadata Footer aligned to the bottom-end
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isForwarded) {
                            Text(
                                text = "🔗 ${maxOf(1, message.forwardCount)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.ElectricCyan,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { onOpenForwardHistory() }
                            )
                        }

                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.TextMuted,
                            fontSize = 10.sp
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryStatusIndicator(
                                status = message.deliveryStatus,
                                onRetry = onRetry
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentBubbleCard(
    attachment: Attachment,
    messageType: MessageType,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = CryptoraColors.DeepNavyBackground.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (messageType == MessageType.IMAGE) Icons.Default.Image else Icons.Default.Description,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    color = CryptoraColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatFileSize(attachment.size)} • Encrypted (AES)",
                    color = CryptoraColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DeliveryStatusIndicator(
    status: MessageDeliveryStatus,
    onRetry: () -> Unit
) {
    when (status) {
        MessageDeliveryStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                color = CryptoraColors.TextMuted,
                strokeWidth = 1.2.dp
            )
        }
        MessageDeliveryStatus.SENT -> {
            // Sent indicator: Single Check ✓
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Sent",
                tint = CryptoraColors.TextMuted,
                modifier = Modifier.size(13.dp)
            )
        }
        MessageDeliveryStatus.DELIVERED -> {
            // Delivered indicator: Double Check ✓✓ (Muted)
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = CryptoraColors.TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
        MessageDeliveryStatus.READ -> {
            // Read indicator: Double Check ✓✓ (Electric Cyan)
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(14.dp)
            )
        }
        MessageDeliveryStatus.FAILED -> {
            // Failed indicator: ⚠️ with retry
            Row(
                modifier = Modifier.clickable { onRetry() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Failed",
                    tint = CryptoraColors.CrimsonDanger,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = CryptoraColors.CrimsonDanger,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputMessage: String,
    isSecureTimedMode: Boolean,
    pendingAttachmentName: String?,
    pendingAttachmentType: MessageType?,
    isLoading: Boolean,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onToggleSecureMode: () -> Unit,
    onClearAttachment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CryptoraColors.SurfaceNavy)
            .border(
                width = 1.dp,
                color = if (isSecureTimedMode) CryptoraColors.ElectricCyan.copy(alpha = 0.6f) else CryptoraColors.BorderSubtle
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Secure-mode indicator banner
        if (isSecureTimedMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Encrypted Hardware Channel Active",
                    color = CryptoraColors.ElectricCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }
        }

        // Pending Attachment Chip
        if (pendingAttachmentName != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CryptoraColors.DeepNavyBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (pendingAttachmentType == MessageType.IMAGE) Icons.Default.Image else Icons.Default.Description,
                        contentDescription = null,
                        tint = CryptoraColors.ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pendingAttachmentName,
                        color = CryptoraColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearAttachment,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear attachment",
                            tint = CryptoraColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Button (+)
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach File",
                    tint = CryptoraColors.ElectricCyan
                )
            }

            // Secure mode toggle button (Architecture ready for timed access!)
            IconButton(
                onClick = onToggleSecureMode,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Secure Policy",
                    tint = if (isSecureTimedMode) CryptoraColors.ElectricCyan else CryptoraColors.TextMuted
                )
            }

            // Message text input
            OutlinedTextField(
                value = inputMessage,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        text = if (isSecureTimedMode) "Secure encrypted message..." else "Type a message...",
                        color = CryptoraColors.TextMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CryptoraColors.ElectricCyan,
                    unfocusedBorderColor = CryptoraColors.BorderSubtle,
                    focusedTextColor = CryptoraColors.TextPrimary,
                    unfocusedTextColor = CryptoraColors.TextPrimary,
                    cursorColor = CryptoraColors.ElectricCyan
                )
            )

            // Send Button
            IconButton(
                onClick = onSend,
                enabled = !isLoading && (inputMessage.isNotBlank() || pendingAttachmentName != null),
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputMessage.isNotBlank() || pendingAttachmentName != null) CryptoraColors.ElectricCyan else CryptoraColors.SurfaceElevated
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CryptoraColors.DeepNavyBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputMessage.isNotBlank() || pendingAttachmentName != null) CryptoraColors.DeepNavyBackground else CryptoraColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPickerContent(
    onPickImage: () -> Unit,
    onPickFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Attach Encrypted Media",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CryptoraColors.TextPrimary
        )
        Text(
            text = "Files are chunked & encrypted client-side using AES-256 before upload (Max 25MB).",
            style = MaterialTheme.typography.bodySmall,
            color = CryptoraColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PickerOptionItem(
                icon = Icons.Default.Image,
                label = "Photo / Image",
                onClick = onPickImage
            )
            PickerOptionItem(
                icon = Icons.Default.Description,
                label = "Document / File",
                onClick = onPickFile
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PickerOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f))
                .border(1.dp, CryptoraColors.ElectricCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = CryptoraColors.TextPrimary
        )
    }
}

@Composable
private fun EmptyChatConversation(participantName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CryptoraColors.ElectricCyan.copy(alpha = 0.1f))
                    .border(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "End-to-End Encrypted Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CryptoraColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Messages and attachments with ${participantName.ifBlank { "this contact" }} are securely encrypted on your device using AES-256.",
                style = MaterialTheme.typography.bodySmall,
                color = CryptoraColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

private fun formatRemainingDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

