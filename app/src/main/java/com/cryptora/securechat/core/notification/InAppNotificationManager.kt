package com.cryptora.securechat.core.notification

import com.cryptora.securechat.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class InAppNotificationType {
    SUCCESS,
    INFO,
    WARNING,
    DANGER,
    ACCESS,
    EXPIRY
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String = "",
    val type: InAppNotificationType = InAppNotificationType.INFO,
    val durationMillis: Long = 3500L,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class InAppNotificationManager @Inject constructor(
    private val dispatchers: DispatcherProvider
) {

    private val _currentNotification = MutableStateFlow<InAppNotification?>(null)
    val currentNotification: StateFlow<InAppNotification?> = _currentNotification.asStateFlow()

    // Deduplication tracking to prevent notification spamming
    private val recentAlertHistory = ConcurrentHashMap<String, Long>()
    private val managerScope = CoroutineScope(dispatchers.main + SupervisorJob())

    fun showNotification(
        title: String,
        message: String = "",
        type: InAppNotificationType = InAppNotificationType.INFO,
        durationMillis: Long = 3500L
    ) {
        val now = System.currentTimeMillis()
        val signatureKey = "${type.name}:$title:$message"

        // Rate-limiting: Suppress identical alert within 3-second debounce window
        val lastSeen = recentAlertHistory[signatureKey] ?: 0L
        if (now - lastSeen < 3000L) {
            return
        }
        recentAlertHistory[signatureKey] = now

        // Housekeeping to prevent memory leak in history map
        if (recentAlertHistory.size > 50) {
            val pruneThreshold = now - 15000L
            recentAlertHistory.entries.removeIf { it.value < pruneThreshold }
        }

        val notification = InAppNotification(
            title = title,
            message = message,
            type = type,
            durationMillis = durationMillis
        )
        _currentNotification.value = notification

        managerScope.launch {
            delay(durationMillis)
            if (_currentNotification.value?.id == notification.id) {
                _currentNotification.value = null
            }
        }
    }

    fun dismiss() {
        _currentNotification.value = null
    }

    // --- High-level semantic shortcuts ---

    fun notifyMessageSent(isSecure: Boolean = true) {
        showNotification(
            title = if (isSecure) "✓ Secure message sent" else "✓ Message sent",
            type = InAppNotificationType.SUCCESS
        )
    }

    fun notifyAccessRequestReceived(requesterUsername: String, contentTitle: String) {
        showNotification(
            title = "🔐 Access request received",
            message = "@$requesterUsername requested access to $contentTitle",
            type = InAppNotificationType.ACCESS
        )
    }

    fun notifyExpiringSoon(minutes: Int) {
        showNotification(
            title = "⏱️ Secure content expires in $minutes minutes",
            message = "Content will be cryptographically locked soon",
            type = InAppNotificationType.EXPIRY
        )
    }

    fun notifyExpired() {
        showNotification(
            title = "🔒 Secure content expired",
            message = "Authoritative time elapsed. Keys shredded permanently.",
            type = InAppNotificationType.DANGER
        )
    }

    fun notifyForwardSuccess(recipientUsername: String) {
        showNotification(
            title = "🔗 Forwarded to @$recipientUsername",
            message = "Cryptographic forward chain updated",
            type = InAppNotificationType.INFO
        )
    }
}
