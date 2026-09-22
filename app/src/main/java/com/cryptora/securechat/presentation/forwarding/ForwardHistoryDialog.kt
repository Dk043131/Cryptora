package com.cryptora.securechat.presentation.forwarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraOutlinedButton
import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardChainIntegrity
import com.cryptora.securechat.domain.model.ForwardEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForwardHistoryDialog(
    originalOwnerUsername: String,
    integrity: ForwardChainIntegrity?,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
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
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = CryptoraColors.ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Forward History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Text(
                                text = "Cryptographic Hash Chain Timeline",
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

                // Integrity Banner
                if (integrity != null) {
                    val isVerified = integrity.isVerified
                    val bannerBg = if (isVerified) CryptoraColors.EmeraldSafe.copy(alpha = 0.15f) else CryptoraColors.CrimsonDanger.copy(alpha = 0.15f)
                    val bannerBorder = if (isVerified) CryptoraColors.EmeraldSafe else CryptoraColors.CrimsonDanger
                    val bannerIcon = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bannerBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = bannerIcon,
                                contentDescription = null,
                                tint = bannerBorder,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isVerified) "🛡️ Cryptographically Verified" else "⚠️ Chain Tampered",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = bannerBorder
                                )
                                Text(
                                    text = integrity.validationMessage,
                                    fontSize = 11.sp,
                                    color = CryptoraColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Original Sender Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.DeepNavyBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CryptoraColors.ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Original Content Owner",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryptoraColors.TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "@${originalOwnerUsername.ifBlank { "unknown" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.ElectricCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CryptoraColors.ElectricCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (integrity?.events.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No forwarding hops yet. This message is directly at genesis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoraColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(integrity!!.events, key = { _, item -> item.eventId }) { index, event ->
                            ForwardEventTimelineCard(
                                hopIndex = index + 1,
                                event = event
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CryptoraOutlinedButton(
                    text = "Close",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ForwardEventTimelineCard(
    hopIndex: Int,
    event: ForwardEvent
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CryptoraColors.DeepNavyBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$hopIndex.",
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${event.fromUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CryptoraColors.TextMuted,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp)
                    )
                    Text(
                        text = "@${event.toUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan
                    )
                }

                ForwardStatusBadge(status = event.approvalStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatEventTime(event.timestamp),
                    fontSize = 10.sp,
                    color = CryptoraColors.TextMuted
                )
                Text(
                    text = "Hash: ${event.eventHash.take(8)}...${event.eventHash.takeLast(6)}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CryptoraColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ForwardStatusBadge(status: ForwardApprovalStatus) {
    val (color, text) = when (status) {
        ForwardApprovalStatus.APPROVED -> Pair(CryptoraColors.EmeraldSafe, "APPROVED")
        ForwardApprovalStatus.PENDING -> Pair(CryptoraColors.AmberWarning, "PENDING")
        ForwardApprovalStatus.REJECTED -> Pair(CryptoraColors.CrimsonDanger, "REJECTED")
        ForwardApprovalStatus.EXPIRED -> Pair(CryptoraColors.TextMuted, "EXPIRED")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatEventTime(timestamp: Long): String {
    return SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
