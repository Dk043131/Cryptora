package com.cryptora.securechat.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CryptoraFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: CryptoraNotificationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store or synchronize push token with secure backend
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"] ?: "message"

        when (type) {
            "access_request" -> {
                val requester = data["requesterUsername"] ?: "Someone"
                val title = data["contentTitle"] ?: "Secure Content"
                val reqId = data["requestId"] ?: "req_push_${System.currentTimeMillis()}"
                notificationManager.showAccessRequestNotification(requester, title, reqId)
            }
            "access_approved" -> {
                val title = data["contentTitle"] ?: "Secure Content"
                val reqId = data["requestId"] ?: "req_push_${System.currentTimeMillis()}"
                notificationManager.showAccessApprovedNotification(title, reqId)
            }
            "access_rejected" -> {
                val title = data["contentTitle"] ?: "Secure Content"
                val reqId = data["requestId"] ?: "req_push_${System.currentTimeMillis()}"
                notificationManager.showAccessRejectedNotification(title, reqId)
            }
            "forward_request" -> {
                val forwarder = data["forwarderUsername"] ?: "Someone"
                val recipient = data["recipientUsername"] ?: "Someone"
                val eventId = data["eventId"] ?: "fwd_push_${System.currentTimeMillis()}"
                notificationManager.showForwardRequestNotification(forwarder, recipient, eventId)
            }
            "expiry_warning" -> {
                val title = data["contentTitle"] ?: "Time-locked message"
                val mins = data["minutesRemaining"]?.toIntOrNull() ?: 2
                val msgId = data["messageId"] ?: "msg_${System.currentTimeMillis()}"
                notificationManager.showExpiringSoonNotification(title, mins, msgId)
            }
            else -> {
                val sender = data["senderUsername"] ?: remoteMessage.notification?.title ?: "Cryptora User"
                val body = data["body"] ?: remoteMessage.notification?.body ?: "New encrypted message received"
                val convId = data["conversationId"] ?: "default_conv"
                notificationManager.showNewMessageNotification(sender, body, convId)
            }
        }
    }
}
