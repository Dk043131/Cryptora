package com.cryptora.securechat.presentation.access

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraButton
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraOutlinedButton
import com.cryptora.securechat.domain.model.AccessRequest
import com.cryptora.securechat.domain.model.AccessRequestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessRequestsScreen(
    viewModel: AccessRequestsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog for sender approval with custom duration
    uiState.selectedRequestForApproval?.let { request ->
        ApproveAccessRequestDialog(
            request = request,
            onApprove = { duration -> viewModel.onApproveWithDuration(request.requestId, duration) },
            onReject = { viewModel.onRejectRequest(request.requestId) },
            onDismiss = viewModel::dismissApprovalDialog
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CryptoraColors.DeepNavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Access Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CryptoraColors.TextPrimary
                        )
                        Text(
                            text = "Cryptographic access control dashboard",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CryptoraColors.SurfaceNavy
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Feedback or error banner
                uiState.feedbackMessage?.let { feedback ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { viewModel.clearFeedback() },
                        shape = RoundedCornerShape(8.dp),
                        color = CryptoraColors.EmeraldSafe.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.EmeraldSafe)
                    ) {
                        Text(
                            text = feedback,
                            color = CryptoraColors.EmeraldSafe,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { viewModel.clearFeedback() },
                        shape = RoundedCornerShape(8.dp),
                        color = CryptoraColors.CrimsonDanger.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.CrimsonDanger)
                    ) {
                        Text(
                            text = error,
                            color = CryptoraColors.CrimsonDanger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AccessRequestFilter.entries) { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = {
                                Text(
                                    text = filter.label,
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
                                borderColor = CryptoraColors.BorderSubtle,
                                selectedBorderColor = CryptoraColors.ElectricCyan
                            )
                        )
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CryptoraColors.ElectricCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                val filteredRequests = uiState.requests.filter { req ->
                    when (uiState.selectedFilter) {
                        AccessRequestFilter.ALL -> true
                        AccessRequestFilter.PENDING -> req.status == AccessRequestStatus.PENDING
                        AccessRequestFilter.APPROVED -> req.status == AccessRequestStatus.APPROVED
                        AccessRequestFilter.REJECTED -> req.status == AccessRequestStatus.REJECTED
                        AccessRequestFilter.EXPIRED -> req.status == AccessRequestStatus.EXPIRED
                    }
                }

                if (filteredRequests.isEmpty() && !uiState.isLoading) {
                    EmptyRequestsState(uiState.selectedFilter)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRequests, key = { it.requestId }) { request ->
                            AccessRequestCard(
                                request = request,
                                onApprove = { viewModel.openApprovalDialog(request) },
                                onReject = { viewModel.onRejectRequest(request.requestId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessRequestCard(
    request: AccessRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CryptoraColors.SurfaceNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f))
                        .border(1.dp, CryptoraColors.ElectricCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.requesterUsername.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${request.requesterUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    Text(
                        text = request.contentTitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = CryptoraColors.ElectricCyan
                    )
                    val reqMinutes = (request.requestedDuration ?: (30L * 60L * 1000L)) / 60000L
                    Text(
                        text = "Requested: $reqMinutes mins • ${formatTime(request.requestedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoraColors.TextMuted,
                        fontSize = 11.sp
                    )
                }

                StatusBadge(status = request.status)
            }

            // If Approved, show granted info
            if (request.status == AccessRequestStatus.APPROVED && request.grantedDuration != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val grantMins = request.grantedDuration / 60000L
                Text(
                    text = "Granted Duration: $grantMins minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.EmeraldSafe
                )
            }

            // Action Buttons only if PENDING
            if (request.status == AccessRequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CryptoraOutlinedButton(
                        text = "Reject",
                        onClick = onReject,
                        icon = Icons.Default.Close,
                        borderColor = CryptoraColors.CrimsonDanger.copy(alpha = 0.5f),
                        contentColor = CryptoraColors.CrimsonDanger,
                        modifier = Modifier.weight(1f)
                    )

                    CryptoraButton(
                        text = "Approve",
                        onClick = onApprove,
                        icon = Icons.Default.Check,
                        containerColor = CryptoraColors.EmeraldSafe,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: AccessRequestStatus) {
    val (color, text) = when (status) {
        AccessRequestStatus.PENDING -> Pair(CryptoraColors.AmberWarning, "PENDING")
        AccessRequestStatus.APPROVED -> Pair(CryptoraColors.EmeraldSafe, "APPROVED")
        AccessRequestStatus.REJECTED -> Pair(CryptoraColors.CrimsonDanger, "REJECTED")
        AccessRequestStatus.EXPIRED -> Pair(CryptoraColors.TextMuted, "EXPIRED")
        AccessRequestStatus.CANCELLED -> Pair(CryptoraColors.TextMuted, "CANCELLED")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyRequestsState(filter: AccessRequestFilter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No ${filter.label} Requests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CryptoraColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "When recipients request access to your time-locked messages, they will appear here for cryptographic authorization.",
            style = MaterialTheme.typography.bodySmall,
            color = CryptoraColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
