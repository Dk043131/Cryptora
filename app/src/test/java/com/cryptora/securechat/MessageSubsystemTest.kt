package com.cryptora.securechat

import com.cryptora.securechat.data.attachment.AttachmentManager
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSubsystemTest {

    @Test
    fun testCommonMessageModel_textMessage() {
        val message = Message(
            id = "msg_001",
            conversationId = "conv_100",
            senderId = "user_a",
            recipientId = "user_b",
            encryptedContentBase64 = "ZW5jcnlwdGVkX3RleHQ=",
            encryptionMetadata = EncryptionMetadata(
                initializationVectorBase64 = "aXY=",
                keyAlias = "test_alias"
            ),
            policy = SecureMessagePolicy(),
            messageType = MessageType.TEXT,
            deliveryStatus = MessageDeliveryStatus.SENT,
            isOutgoing = true,
            decryptedTextCache = "Hello Cryptora",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(MessageType.TEXT, message.messageType)
        assertEquals(MessageDeliveryStatus.SENT, message.deliveryStatus)
        assertTrue(message.isOutgoing)
        assertEquals("Hello Cryptora", message.decryptedTextCache)
    }

    @Test
    fun testCommonMessageModel_attachmentMessage() {
        val attachment = Attachment(
            attachmentId = "att_001",
            messageId = "msg_002",
            encryptedFileReference = "/data/user/0/com.cryptora.securechat/files/encrypted_attachments/att_001.enc",
            fileName = "secure_document.pdf",
            mimeType = "application/pdf",
            size = 1024 * 100, // 100 KB
            encryptedSize = 1024 * 100 + 28,
            createdAt = System.currentTimeMillis()
        )

        val message = Message(
            id = "msg_002",
            conversationId = "conv_100",
            senderId = "user_a",
            recipientId = "user_b",
            encryptedContentBase64 = "ZW5jcnlwdGVkX2ZpbGU=",
            encryptionMetadata = EncryptionMetadata(
                initializationVectorBase64 = "aXY=",
                keyAlias = "test_alias"
            ),
            policy = SecureMessagePolicy(),
            messageType = MessageType.FILE,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            isOutgoing = true,
            decryptedTextCache = "[File: secure_document.pdf]",
            timestamp = System.currentTimeMillis(),
            attachment = attachment
        )

        assertEquals(MessageType.FILE, message.messageType)
        assertEquals(MessageDeliveryStatus.DELIVERED, message.deliveryStatus)
        assertNotNull(message.attachment)
        assertEquals("secure_document.pdf", message.attachment?.fileName)
        assertEquals("application/pdf", message.attachment?.mimeType)
        assertEquals(102400L, message.attachment?.size)
    }

    @Test
    fun testAttachmentSizeLimit() {
        val maxAllowed = AttachmentManager.MAX_ATTACHMENT_SIZE_BYTES
        assertEquals(25L * 1024 * 1024, maxAllowed)

        val validFileSize = 20L * 1024 * 1024 // 20 MB
        val oversizeFileSize = 26L * 1024 * 1024 // 26 MB

        assertTrue(validFileSize <= AttachmentManager.MAX_ATTACHMENT_SIZE_BYTES)
        assertFalse(oversizeFileSize <= AttachmentManager.MAX_ATTACHMENT_SIZE_BYTES)
    }

    @Test
    fun testDeliveryStatusProgression() {
        val statuses = listOf(
            MessageDeliveryStatus.SENDING,
            MessageDeliveryStatus.SENT,
            MessageDeliveryStatus.DELIVERED,
            MessageDeliveryStatus.READ,
            MessageDeliveryStatus.FAILED
        )

        assertEquals(5, statuses.size)
        assertEquals(MessageDeliveryStatus.SENT, MessageDeliveryStatus.valueOf("SENT"))
        assertEquals(MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.valueOf("DELIVERED"))
        assertEquals(MessageDeliveryStatus.READ, MessageDeliveryStatus.valueOf("READ"))
        assertEquals(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.valueOf("FAILED"))
    }
}
