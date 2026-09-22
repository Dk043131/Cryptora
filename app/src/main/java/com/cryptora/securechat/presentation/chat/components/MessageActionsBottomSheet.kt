package com.cryptora.securechat.presentation.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsBottomSheet(
    message: Message,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onViewHistory: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CryptoraColors.SurfaceNavy,
        contentColor = CryptoraColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CryptoraColors.TextPrimary
                )
            }

            // Message Preview snippet
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CryptoraColors.DeepNavyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = message.decryptedTextCache?.take(120) ?: "[Encrypted message content]",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary,
                    maxLines = 3,
                    modifier = Modifier.padding(12.dp)
                )
            }

            HorizontalDivider(color = CryptoraColors.BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            // Action: Copy Text
            if (!message.decryptedTextCache.isNullOrBlank()) {
                ActionItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Text",
                    color = CryptoraColors.TextPrimary,
                    onClick = onCopy
                )
            }

            // Action: Forward Securely
            ActionItem(
                icon = Icons.Default.Share,
                title = "Forward Securely",
                color = CryptoraColors.ElectricCyan,
                onClick = onForward
            )

            // Action: Forward Lineage History (if forwarded)
            if (message.isForwarded) {
                ActionItem(
                    icon = Icons.Default.Info,
                    title = "View Forward Chain (${message.forwardCount} hops)",
                    color = CryptoraColors.ElectricCyan,
                    onClick = onViewHistory
                )
            }

            // Action: Retry (if failed)
            if (message.deliveryStatus == MessageDeliveryStatus.FAILED) {
                ActionItem(
                    icon = Icons.Default.Refresh,
                    title = "Retry Sending",
                    color = CryptoraColors.AmberWarning,
                    onClick = onRetry
                )
            }

            // Action: Delete Local Copy
            ActionItem(
                icon = Icons.Default.Delete,
                title = "Delete Local Copy",
                color = CryptoraColors.CrimsonDanger,
                onClick = onDelete
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .semantics { contentDescription = title },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color,
            fontSize = 14.sp
        )
    }
}
