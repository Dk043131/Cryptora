package com.cryptora.securechat.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cryptora.securechat.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoraNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_MESSAGES = "cryptora_channel_messages"
        const val CHANNEL_ACCESS = "cryptora_channel_access"
        const val CHANNEL_FORWARDING = "cryptora_channel_forwarding"
        const val CHANNEL_EXPIRY = "cryptora_channel_expiry"

        private const val ID_OFFSET_MESSAGES = 1000
        private const val ID_OFFSET_ACCESS = 2000
        private const val ID_OFFSET_FORWARD = 3000
        private const val ID_OFFSET_EXPIRY = 4000
    }

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Initializes all standard notification channels for Cryptora.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages & Attachments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming end-to-end encrypted messages and attachments."
                enableVibration(true)
                setShowBadge(true)
            }

            val accessChannel = NotificationChannel(
                CHANNEL_ACCESS,
                "Access Authorization",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Authorization requests, approvals, and rejections for secure content."
                enableVibration(true)
                setShowBadge(true)
            }

            val forwardingChannel = NotificationChannel(
                CHANNEL_FORWARDING,
                "Secure Forwarding",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Forward requests, approvals, and chain lineage updates."
                enableVibration(false)
                setShowBadge(true)
            }

            val expiryChannel = NotificationChannel(
                CHANNEL_EXPIRY,
                "Time-Lock Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Time-lock expiration warnings and automated content shredding alerts."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, accessChannel, forwardingChannel, expiryChannel)
            )
        }
    }

    // --- Message Notifications ---

    fun showNewMessageNotification(senderUsername: String, messageSnippet: String, conversationId: String) {
        val title = "Message from @$senderUsername"
        buildAndNotify(
            notificationId = ID_OFFSET_MESSAGES + conversationId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_MESSAGES,
            title = title,
            content = messageSnippet,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showNewImageNotification(senderUsername: String, fileName: String, conversationId: String) {
        val title = "📷 Photo from @$senderUsername"
        val content = if (fileName.isNotBlank()) "Encrypted image: $fileName" else "Received encrypted photo"
        buildAndNotify(
            notificationId = ID_OFFSET_MESSAGES + conversationId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_MESSAGES,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showNewFileNotification(senderUsername: String, fileName: String, conversationId: String) {
        val title = "📎 File from @$senderUsername"
        val content = if (fileName.isNotBlank()) "Encrypted document: $fileName" else "Received encrypted attachment"
        buildAndNotify(
            notificationId = ID_OFFSET_MESSAGES + conversationId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_MESSAGES,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showSecureMessageReceivedNotification(senderUsername: String, isLocked: Boolean, conversationId: String) {
        val title = if (isLocked) "🔒 Locked Secure Content" else "🔐 Secure Time-Locked Message"
        val content = if (isLocked) {
            "@$senderUsername shared time-locked content requiring access approval."
        } else {
            "@$senderUsername sent an encrypted self-expiring message."
        }
        buildAndNotify(
            notificationId = ID_OFFSET_MESSAGES + conversationId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_MESSAGES,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    // --- Access Authorization Notifications ---

    fun showAccessRequestNotification(requesterUsername: String, contentTitle: String, requestId: String) {
        val title = "🔐 Access Request"
        val content = "@$requesterUsername requested access to \"$contentTitle\"."
        buildAndNotify(
            notificationId = ID_OFFSET_ACCESS + requestId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_ACCESS,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showAccessApprovedNotification(contentTitle: String, requestId: String) {
        val title = "🔓 Access Granted"
        val content = "Your request to view \"$contentTitle\" was approved."
        buildAndNotify(
            notificationId = ID_OFFSET_ACCESS + requestId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_ACCESS,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showAccessRejectedNotification(contentTitle: String, requestId: String) {
        val title = "🚫 Access Request Declined"
        val content = "Request to view \"$contentTitle\" was declined by the owner."
        buildAndNotify(
            notificationId = ID_OFFSET_ACCESS + requestId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_ACCESS,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showAccessRevokedNotification(contentTitle: String) {
        val title = "🛑 Access Revoked"
        val content = "Access to \"$contentTitle\" was revoked by the sender. Local keys destroyed."
        buildAndNotify(
            notificationId = ID_OFFSET_ACCESS + contentTitle.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_ACCESS,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    // --- Forwarding Notifications ---

    fun showForwardRequestNotification(forwarderUsername: String, recipientUsername: String, eventId: String) {
        val title = "🔗 Forward Authorization Required"
        val content = "@$forwarderUsername wants to forward your secure message to @$recipientUsername."
        buildAndNotify(
            notificationId = ID_OFFSET_FORWARD + eventId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_FORWARDING,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showForwardApprovedNotification(contentTitle: String, eventId: String) {
        val title = "✓ Forward Request Approved"
        val content = "Forwarding for \"$contentTitle\" was approved by the original owner."
        buildAndNotify(
            notificationId = ID_OFFSET_FORWARD + eventId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_FORWARDING,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showForwardRejectedNotification(contentTitle: String, eventId: String) {
        val title = "✕ Forward Request Declined"
        val content = "Forwarding for \"$contentTitle\" was declined by the original owner."
        buildAndNotify(
            notificationId = ID_OFFSET_FORWARD + eventId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_FORWARDING,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    // --- Time-Lock & Expiry Notifications ---

    fun showExpiringSoonNotification(contentTitle: String, minutesRemaining: Int, messageId: String) {
        val title = "⏱️ Secure Content Expiring Soon"
        val content = "\"$contentTitle\" will expire and be shredded in $minutesRemaining minute(s)."
        buildAndNotify(
            notificationId = ID_OFFSET_EXPIRY + messageId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_EXPIRY,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showExpiredNotification(contentTitle: String, messageId: String) {
        val title = "🔒 Secure Content Expired"
        val content = "\"$contentTitle\" time elapsed. Decryption keys permanently shredded."
        buildAndNotify(
            notificationId = ID_OFFSET_EXPIRY + messageId.hashCode().and(0x7FFFFFFF) % 1000,
            channelId = CHANNEL_EXPIRY,
            title = title,
            content = content,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun notifyOtpCode(code: String) {
        buildAndNotify(
            notificationId = 9999,
            channelId = CHANNEL_ACCESS,
            title = "🔐 Cryptora Security Code",
            content = "Your Cryptora verification code is $code. Valid for 5 minutes. Do not share.",
            priority = NotificationCompat.PRIORITY_MAX
        )
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager?.cancel(notificationId)
    }

    private fun buildAndNotify(
        notificationId: Int,
        channelId: String,
        title: String,
        content: String,
        priority: Int
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS permission not yet granted by user
        } catch (_: Exception) {
            // Defensive error handling
        }
    }
}
