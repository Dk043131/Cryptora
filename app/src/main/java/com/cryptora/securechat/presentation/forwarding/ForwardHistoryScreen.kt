package com.cryptora.securechat.presentation.forwarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraCard
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraEmptyState
import com.cryptora.securechat.core.designsystem.CryptoraErrorView
import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardHistoryScreen(
    viewModel: ForwardHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CryptoraColors.DeepNavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Forward Hash Chain",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CryptoraColors.TextPrimary
                        )
                        Text(
                            text = "Tamper-Evident Audit Trail",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.ElectricCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CryptoraColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadForwardHistory) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CryptoraColors.ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CryptoraColors.SurfaceNavy
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = CryptoraColors.ElectricCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
                            Text(
                                text = "Verifying cryptographic chain signatures...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CryptoraColors.TextSecondary
                            )
                        }
                    }
                }
                uiState.errorMessage != null -> {
                    CryptoraErrorView(
                        message = uiState.errorMessage ?: "Failed to verify forward chain.",
                        onRetry = viewModel::loadForwardHistory
                    )
                }
                else -> {
                    val integrity = uiState.integrity
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(CryptoraDimens.PaddingDefault),
                        verticalArrangement = Arrangement.spacedBy(CryptoraDimens.PaddingDefault)
                    ) {
                        // Integrity Status Banner
                        item {
                            val isVerified = integrity?.isVerified == true
                            val bannerBorder = if (isVerified) CryptoraColors.EmeraldSafe else CryptoraColors.CrimsonDanger
                            val bannerBg = if (isVerified) CryptoraColors.EmeraldSafe.copy(alpha = 0.12f) else CryptoraColors.CrimsonDanger.copy(alpha = 0.12f)
                            val bannerIcon = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning

                            Surface(
                                shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
                                color = bannerBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = bannerIcon,
                                        contentDescription = null,
                                        tint = bannerBorder,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isVerified) "Cryptographic Chain Verified" else "Chain Integrity Violation Detected",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = bannerBorder
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = integrity?.validationMessage ?: "Hash verification in progress.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CryptoraColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Message Header Info
                        item {
                            CryptoraCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = CryptoraColors.ElectricCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Root Payload ID",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CryptoraColors.TextSecondary
                                        )
                                        Text(
                                            text = uiState.messageId.take(16) + if (uiState.messageId.length > 16) "..." else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = CryptoraColors.ElectricCyan
                                        )
                                    }
                                }
                            }
                        }

                        // Timeline Events
                        if (integrity == null || integrity.events.isEmpty()) {
                            item {
                                CryptoraEmptyState(
                                    title = "No Forward Hops",
                                    description = "This secure payload is at genesis and has not been forwarded to any other recipient."
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "Chronological Forward Events (${integrity.events.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CryptoraColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            itemsIndexed(integrity.events, key = { _, item -> item.eventId }) { index, event ->
                                TimelineEventCard(
                                    hopNumber = index + 1,
                                    event = event,
                                    isLast = index == integrity.events.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    hopNumber: Int,
    event: ForwardEvent,
    isLast: Boolean
) {
    CryptoraCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$hopNumber",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CryptoraColors.ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "@${event.fromUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CryptoraColors.TextMuted,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp)
                    )
                    Text(
                        text = "@${event.toUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan
                    )
                }

                StatusBadge(status = event.approvalStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CryptoraColors.BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Timestamp",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoraColors.TextMuted
                    )
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = CryptoraColors.TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SHA-256 Hash",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoraColors.TextMuted
                    )
                    Text(
                        text = "${event.eventHash.take(8)}...${event.eventHash.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoraColors.ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ForwardApprovalStatus) {
    val (color, label) = when (status) {
        ForwardApprovalStatus.APPROVED -> Pair(CryptoraColors.EmeraldSafe, "APPROVED")
        ForwardApprovalStatus.PENDING -> Pair(CryptoraColors.AmberWarning, "PENDING")
        ForwardApprovalStatus.REJECTED -> Pair(CryptoraColors.CrimsonDanger, "REJECTED")
        ForwardApprovalStatus.EXPIRED -> Pair(CryptoraColors.TextMuted, "EXPIRED")
    }

    Surface(
        shape = RoundedCornerShape(CryptoraDimens.CornerExtraSmall),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
