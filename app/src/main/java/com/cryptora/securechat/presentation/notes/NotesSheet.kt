package com.cryptora.securechat.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraButton
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraOutlinedButton
import com.cryptora.securechat.core.designsystem.CryptoraSearchBar
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(
    viewModel: NotesViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CryptoraColors.DeepNavyBackground,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f))
                            .border(1.dp, CryptoraColors.ElectricCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notes Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CryptoraColors.ElectricCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AES-256",
                                    color = CryptoraColors.ElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Hardware encrypted at rest",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CryptoraColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = CryptoraColors.BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            CryptoraSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search notes locally...",
                onClear = { viewModel.onSearchQueryChange("") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "+ New Note" Action Button
            CryptoraButton(
                text = "New Note",
                onClick = viewModel::onStartCreateNote,
                icon = Icons.Default.Add
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status feedback banner
            uiState.statusMessage?.let { status ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.EmeraldSafe.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.EmeraldSafe.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = status,
                        color = CryptoraColors.EmeraldSafe,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Notes List
            if (uiState.filteredNotes.isEmpty()) {
                if (uiState.searchQuery.isNotBlank()) {
                    SearchEmptyState(query = uiState.searchQuery)
                } else {
                    VaultEmptyState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { viewModel.onOpenNote(note) },
                            onTogglePin = { viewModel.onTogglePin(note.id) },
                            onDelete = { viewModel.onRequestDelete(note) }
                        )
                    }
                }
            }
        }
    }

    // Note Editor Modal/Dialog
    if (uiState.isEditorOpen) {
        NoteEditorDialog(
            isCreatingNew = uiState.isCreatingNew,
            title = uiState.editTitle,
            body = uiState.editBody,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            onSave = viewModel::onSaveNote,
            onSendSecurely = {
                uiState.activeNote?.let { viewModel.onRequestSendSecurely(it) }
            },
            canSendSecurely = !uiState.isCreatingNew && uiState.activeNote != null,
            onDismiss = viewModel::onCloseEditor
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog && uiState.noteToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelDelete,
            title = {
                Text(
                    text = "Shred Note?",
                    color = CryptoraColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${uiState.noteToDelete?.title}\"? The encrypted content and key material will be shredded.",
                    color = CryptoraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoraColors.CrimsonDanger)
                ) {
                    Text("Delete Note", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDelete) {
                    Text("Cancel", color = CryptoraColors.TextSecondary)
                }
            },
            containerColor = CryptoraColors.SurfaceNavy
        )
    }

    // Send Note Securely Dialog
    if (uiState.showSendSecurelyDialog && uiState.noteToSendSecurely != null) {
        SendNoteSecurelyDialog(
            note = uiState.noteToSendSecurely!!,
            conversations = uiState.availableConversations,
            onSelectConversation = viewModel::onConfirmSendSecurely,
            onDismiss = viewModel::onCancelSendSecurely
        )
    }

    // Flagship Timelock & Access Options for Note
    if (uiState.showPolicyOptionsDialog && uiState.selectedConversationForSend != null && uiState.noteToSendSecurely != null) {
        val selectedConv = uiState.selectedConversationForSend!!
        val note = uiState.noteToSendSecurely!!
        com.cryptora.securechat.presentation.chat.SendSecureOptionsDialog(
            recipientName = selectedConv.participantUser.fullName.ifBlank { selectedConv.participantUser.username },
            recipientUsername = selectedConv.participantUser.username,
            contentPreview = "📝 ${note.title}\n${note.body.take(100)}",
            onConfirm = viewModel::onConfirmSendWithPolicy,
            onDismiss = viewModel::onCancelPolicyOptions
        )
    }
}

@Composable
private fun NoteCardItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = CryptoraColors.SurfaceNavy,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.isPinned) CryptoraColors.ElectricCyan.copy(alpha = 0.6f) else CryptoraColors.BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (note.isPinned) CryptoraColors.ElectricCyan.copy(alpha = 0.2f)
                        else CryptoraColors.DeepNavyBackground
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = if (note.isPinned) CryptoraColors.ElectricCyan else CryptoraColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (note.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = CryptoraColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Updated ${formatNoteDate(note.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) CryptoraColors.ElectricCyan else CryptoraColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CryptoraColors.CrimsonDanger.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    isCreatingNew: Boolean,
    title: String,
    body: String,
    isLoading: Boolean,
    errorMessage: String?,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onSendSecurely: () -> Unit,
    canSendSecurely: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CryptoraColors.SurfaceNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCreatingNew) "New Private Note" else "Edit Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CryptoraColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = CryptoraColors.CrimsonDanger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title", color = CryptoraColors.TextSecondary) },
                    placeholder = { Text("e.g. Project Credentials", color = CryptoraColors.TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoraColors.ElectricCyan,
                        unfocusedBorderColor = CryptoraColors.BorderSubtle,
                        focusedTextColor = CryptoraColors.TextPrimary,
                        unfocusedTextColor = CryptoraColors.TextPrimary,
                        cursorColor = CryptoraColors.ElectricCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("Encrypted Content", color = CryptoraColors.TextSecondary) },
                    placeholder = { Text("Write confidential notes, tokens, or recovery keys...", color = CryptoraColors.TextMuted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoraColors.ElectricCyan,
                        unfocusedBorderColor = CryptoraColors.BorderSubtle,
                        focusedTextColor = CryptoraColors.TextPrimary,
                        unfocusedTextColor = CryptoraColors.TextPrimary,
                        cursorColor = CryptoraColors.ElectricCyan
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canSendSecurely) {
                        CryptoraOutlinedButton(
                            text = "Send Securely",
                            onClick = onSendSecurely,
                            icon = Icons.Default.Lock,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CryptoraButton(
                        text = "Save Note",
                        onClick = onSave,
                        enabled = !isLoading && title.isNotBlank(),
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SendNoteSecurelyDialog(
    note: Note,
    conversations: List<Conversation>,
    onSelectConversation: (Conversation) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CryptoraColors.SurfaceNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Note Securely",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CryptoraColors.TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CryptoraColors.TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a conversation to send \"${note.title}\" directly via end-to-end encryption without copying plaintext.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (conversations.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CryptoraColors.DeepNavyBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No active conversations found. Start a chat first to send notes.",
                            color = CryptoraColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            Surface(
                                onClick = { onSelectConversation(conv) },
                                shape = RoundedCornerShape(8.dp),
                                color = CryptoraColors.DeepNavyBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = conv.participantUser.username.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = CryptoraColors.ElectricCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = conv.participantUser.fullName.ifBlank { conv.participantUser.username },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CryptoraColors.TextPrimary
                                        )
                                        Text(
                                            text = "@${conv.participantUser.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CryptoraColors.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = CryptoraColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun VaultEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Notes Vault is Empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CryptoraColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Store project credentials, meeting notes, API keys, or personal ideas encrypted locally with AES-256.",
            style = MaterialTheme.typography.bodySmall,
            color = CryptoraColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = CryptoraColors.TextMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No notes matching \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoraColors.TextSecondary
        )
    }
}

private fun formatNoteDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
