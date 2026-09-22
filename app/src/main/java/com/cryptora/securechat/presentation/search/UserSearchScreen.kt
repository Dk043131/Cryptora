package com.cryptora.securechat.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraAvatar
import com.cryptora.securechat.core.designsystem.CryptoraCard
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraEmptyState
import com.cryptora.securechat.core.designsystem.CryptoraErrorView
import com.cryptora.securechat.core.designsystem.CryptoraLoadingIndicator
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.Note
import com.cryptora.securechat.domain.model.SearchFilterTab
import com.cryptora.securechat.domain.model.UnifiedSearchResult
import com.cryptora.securechat.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserSearchScreen(
    viewModel: UserSearchViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CryptoraColors.DeepNavyBackground)
            .padding(horizontal = CryptoraDimens.PaddingDefault)
    ) {
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChanged(it) },
            placeholder = { Text("Search users, messages, notes, chats...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = CryptoraColors.ElectricCyan
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearQuery() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = CryptoraColors.TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Unified search input field" },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CryptoraColors.SurfaceNavy,
                unfocusedContainerColor = CryptoraColors.SurfaceNavy,
                focusedBorderColor = CryptoraColors.ElectricCyan,
                unfocusedBorderColor = CryptoraColors.BorderSubtle,
                focusedTextColor = CryptoraColors.TextPrimary,
                unfocusedTextColor = CryptoraColors.TextPrimary,
                focusedPlaceholderColor = CryptoraColors.TextSecondary,
                unfocusedPlaceholderColor = CryptoraColors.TextSecondary
            ),
            shape = RoundedCornerShape(CryptoraDimens.CornerMedium)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilterTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectTab(tab) },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CryptoraColors.ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CryptoraColors.ElectricCyan,
                        containerColor = CryptoraColors.SurfaceNavy,
                        labelColor = CryptoraColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle
                    )
                )
            }
        }

        // Privacy indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CryptoraColors.ElectricCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Private messages & notes searched strictly on-device",
                color = CryptoraColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    CryptoraEmptyState(
                        title = "Search Cryptora",
                        description = "Find users by @username or search your local private chats, notes, and messages."
                    )
                }
                is SearchUiState.Loading -> {
                    CryptoraLoadingIndicator(message = "Searching verified users and local database...")
                }
                is SearchUiState.Success -> {
                    SearchResultsList(
                        result = state.result,
                        selectedTab = selectedTab,
                        onStartChat = { viewModel.startChat(it) },
                        onOpenConversation = { viewModel.openConversation(it) }
                    )
                }
                is SearchUiState.Empty -> {
                    CryptoraEmptyState(
                        title = "No Matches Found",
                        description = "No users, messages, notes, or chats found matching \"${state.query}\"."
                    )
                }
                is SearchUiState.Error -> {
                    CryptoraErrorView(
                        message = state.message,
                        onRetry = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    result: UnifiedSearchResult,
    selectedTab: SearchFilterTab,
    onStartChat: (UserProfile) -> Unit,
    onOpenConversation: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Users Section
        if ((selectedTab == SearchFilterTab.ALL || selectedTab == SearchFilterTab.USERS) && result.users.isNotEmpty()) {
            item {
                SearchSectionHeader(title = "Users", count = result.users.size, icon = Icons.Default.Person)
            }
            items(result.users, key = { "user_${it.id}" }) { user ->
                UserSearchResultCard(
                    user = user,
                    onStartChatClick = { onStartChat(user) }
                )
            }
        }

        // Conversations Section
        if ((selectedTab == SearchFilterTab.ALL || selectedTab == SearchFilterTab.CHATS) && result.conversations.isNotEmpty()) {
            item {
                SearchSectionHeader(title = "Conversations", count = result.conversations.size, icon = Icons.AutoMirrored.Filled.Chat)
            }
            items(result.conversations, key = { "conv_${it.id}" }) { conv ->
                ConversationSearchResultCard(
                    conversation = conv,
                    onClick = { onOpenConversation(conv.id) }
                )
            }
        }

        // Messages Section
        if ((selectedTab == SearchFilterTab.ALL || selectedTab == SearchFilterTab.MESSAGES) && result.messages.isNotEmpty()) {
            item {
                SearchSectionHeader(title = "Local Messages", count = result.messages.size, icon = Icons.AutoMirrored.Filled.Message)
            }
            items(result.messages, key = { "msg_${it.id}" }) { message ->
                MessageSearchResultCard(
                    message = message,
                    onClick = { onOpenConversation(message.conversationId) }
                )
            }
        }

        // Notes Section
        if ((selectedTab == SearchFilterTab.ALL || selectedTab == SearchFilterTab.NOTES) && result.notes.isNotEmpty()) {
            item {
                SearchSectionHeader(title = "Local Notes", count = result.notes.size, icon = Icons.Default.Description)
            }
            items(result.notes, key = { "note_${it.id}" }) { note ->
                NoteSearchResultCard(note = note)
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CryptoraColors.ElectricCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CryptoraColors.TextPrimary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CryptoraColors.ElectricCyan.copy(alpha = 0.2f)
        ) {
            Text(
                text = count.toString(),
                color = CryptoraColors.ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun UserSearchResultCard(
    user: UserProfile,
    onStartChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CryptoraCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = CryptoraColors.BorderSubtle
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CryptoraAvatar(
                name = user.fullName.ifBlank { user.username },
                size = 46.dp,
                isKeyVerified = user.isVerified
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoraColors.TextPrimary
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified User",
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.ElectricCyan,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onStartChatClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CryptoraColors.ElectricCyan,
                    contentColor = CryptoraColors.DeepNavyBackground
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Chat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ConversationSearchResultCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = CryptoraColors.SurfaceNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CryptoraColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.participantUser.fullName.ifBlank { "@${conversation.participantUser.username}" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoraColors.TextPrimary
                )
                Text(
                    text = conversation.lastMessage?.decryptedTextCache ?: "Encrypted conversation",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary,
                    maxLines = 1,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MessageSearchResultCard(
    message: Message,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = CryptoraColors.SurfaceNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (message.isOutgoing) "Sent by you" else "Received message",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoraColors.ElectricCyan
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = CryptoraColors.TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.decryptedTextCache ?: "[Encrypted payload]",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextPrimary,
                    maxLines = 2,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NoteSearchResultCard(
    note: Note
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CryptoraColors.SurfaceNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = CryptoraColors.AmberWarning,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoraColors.TextPrimary
                )
                if (note.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = CryptoraColors.TextSecondary,
                        maxLines = 2,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
