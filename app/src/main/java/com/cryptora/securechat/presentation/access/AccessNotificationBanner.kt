package com.cryptora.securechat.presentation.access

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotInterested
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.domain.model.AccessNotificationEvent

@Composable
fun AccessNotificationBanner(
    event: AccessNotificationEvent?,
    onDismiss: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (event == null) return@AnimatedVisibility

        val (icon, iconColor, title, message) = when (event) {
            is AccessNotificationEvent.RequestReceived -> Quadruple(
                Icons.Default.Key,
                CryptoraColors.ElectricCyan,
                "Access Request Received",
                "@${event.requesterUsername} requested access to '${event.contentTitle}'"
            )
            is AccessNotificationEvent.RequestApproved -> Quadruple(
                Icons.Default.LockOpen,
                CryptoraColors.EmeraldSafe,
                "Access Granted",
                "Your cryptographic access request has been approved! Decryption unlocked."
            )
            is AccessNotificationEvent.RequestRejected -> Quadruple(
                Icons.Default.NotInterested,
                CryptoraColors.CrimsonDanger,
                "Access Request Rejected",
                "The content owner has declined access to this secure message."
            )
            is AccessNotificationEvent.AccessExpired -> Quadruple(
                Icons.Default.HourglassBottom,
                CryptoraColors.AmberWarning,
                "Access Expired",
                "Authorized time limit expired. Cached plaintext has been securely shredded."
            )
            is AccessNotificationEvent.AccessRevoked -> Quadruple(
                Icons.Default.Lock,
                CryptoraColors.CrimsonDanger,
                "Access Revoked",
                "The message owner revoked access. Decryption key invalidated."
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CryptoraColors.SurfaceNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onActionClick?.invoke() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f))
                        .border(1.dp, iconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CryptoraColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = CryptoraColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
