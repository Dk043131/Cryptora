package com.cryptora.securechat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.notification.InAppNotification
import com.cryptora.securechat.core.notification.InAppNotificationManager
import com.cryptora.securechat.core.notification.InAppNotificationType

@Composable
fun InAppNotificationHost(
    notificationManager: InAppNotificationManager,
    modifier: Modifier = Modifier
) {
    val notification by notificationManager.currentNotification.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            notification?.let { item ->
                InAppNotificationCard(
                    notification = item,
                    onDismiss = { notificationManager.dismiss() }
                )
            }
        }
    }
}

@Composable
private fun InAppNotificationCard(
    notification: InAppNotification,
    onDismiss: () -> Unit
) {
    val (accentColor, iconVector) = when (notification.type) {
        InAppNotificationType.SUCCESS -> Pair(CryptoraColors.ElectricCyan, Icons.Default.CheckCircle)
        InAppNotificationType.INFO -> Pair(CryptoraColors.ElectricCyan, Icons.Default.Info)
        InAppNotificationType.WARNING -> Pair(CryptoraColors.AmberWarning, Icons.Default.Warning)
        InAppNotificationType.DANGER -> Pair(CryptoraColors.CrimsonDanger, Icons.Default.Lock)
        InAppNotificationType.ACCESS -> Pair(CryptoraColors.AmberWarning, Icons.Default.LockClock)
        InAppNotificationType.EXPIRY -> Pair(CryptoraColors.AmberWarning, Icons.Default.LockClock)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CryptoraColors.SurfaceNavy,
        border = BorderStroke(1.5.dp, accentColor),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clickable { onDismiss() }
            .semantics { contentDescription = "In-app alert: ${notification.title}" }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary,
                        fontSize = 13.sp
                    )

                    if (notification.message.isNotBlank()) {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoraColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = CryptoraColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
