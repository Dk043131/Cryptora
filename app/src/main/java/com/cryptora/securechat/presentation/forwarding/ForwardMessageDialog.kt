package com.cryptora.securechat.presentation.forwarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForwardMessageDialog(
    message: Message,
    conversations: List<Conversation>,
    onForwardToConversation: (targetConversationId: String, targetUserId: String, targetUsername: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val policy = message.policy.forwardingPolicy
    val isForwardingDisabled = policy == ForwardingPolicy.FORWARDING_DISABLED
    val requiresApproval = policy == ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL

    val filteredConversations = conversations.filter {
        it.participantUser.username.contains(searchQuery, ignoreCase = true) ||
                it.participantUser.fullName.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CryptoraColors.SurfaceNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = CryptoraColors.ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Forward Secure Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Text(
                                text = "Tamper-evident cryptographic chain",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryptoraColors.ElectricCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CryptoraColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CryptoraColors.BorderSubtle)
                Spacer(modifier = Modifier.height(12.dp))

                // Forwarding Policy Status Banner
                val (policyBannerColor, policyIcon, policyTitle, policyDesc) = when (policy) {
                    ForwardingPolicy.FORWARDING_DISABLED -> Quad(
                        CryptoraColors.CrimsonDanger,
                        Icons.Default.NotInterested,
                        "Forwarding Prohibited",
                        "The original sender (@${message.originalSenderUsername ?: message.senderId}) has strictly disabled forwarding for this content."
                    )
                    ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL -> Quad(
                        CryptoraColors.AmberWarning,
                        Icons.Default.Lock,
                        "Owner Approval Required",
                        "Forwarding will trigger an authorization request to original owner (@${message.originalSenderUsername ?: message.senderId}). Recipient cannot decrypt until approved."
                    )
                    ForwardingPolicy.FORWARDING_ALLOWED -> Quad(
                        CryptoraColors.EmeraldSafe,
                        Icons.Default.LockOpen,
                        "Forwarding Permitted",
                        "Forwarding is allowed. Message will inherit original expiry (${formatExpiryTime(message.policy.expiresAt)})."
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = policyBannerColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, policyBannerColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = policyIcon,
                            contentDescription = null,
                            tint = policyBannerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = policyTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = policyBannerColor
                            )
                            Text(
                                text = policyDesc,
                                fontSize = 11.sp,
                                color = CryptoraColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Recipient
                Text(
                    text = "SELECT RECIPIENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CryptoraColors.ElectricCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contact or username...", color = CryptoraColors.TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoraColors.ElectricCyan,
                        unfocusedBorderColor = CryptoraColors.BorderSubtle,
                        focusedTextColor = CryptoraColors.TextPrimary,
                        unfocusedTextColor = CryptoraColors.TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val isSelected = selectedConversation?.id == conv.id
                        Surface(
                            onClick = { selectedConversation = conv },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CryptoraColors.ElectricCyan.copy(alpha = 0.15f) else CryptoraColors.DeepNavyBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conv.participantUser.fullName.take(1).uppercase().ifBlank { "U" },
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
                                        color = CryptoraColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle)
                    ) {
                        Text("Cancel", color = CryptoraColors.TextSecondary)
                    }

                    Button(
                        onClick = {
                            selectedConversation?.let { conv ->
                                onForwardToConversation(
                                    conv.id,
                                    conv.participantUser.id,
                                    conv.participantUser.username
                                )
                            }
                        },
                        enabled = !isForwardingDisabled && selectedConversation != null,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CryptoraColors.ElectricCyan,
                            disabledContainerColor = CryptoraColors.BorderSubtle,
                            contentColor = CryptoraColors.DeepNavyBackground,
                            disabledContentColor = CryptoraColors.TextMuted
                        )
                    ) {
                        Text(
                            text = if (requiresApproval) "Request Forward" else "Forward Message",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatExpiryTime(expiresAt: Long?): String {
    if (expiresAt == null) return "No Expiry"
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expiresAt))
}
