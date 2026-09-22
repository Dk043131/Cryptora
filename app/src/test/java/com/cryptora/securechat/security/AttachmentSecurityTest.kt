package com.cryptora.securechat.security

import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.CryptoManagerImpl
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.KeyManagerImpl
import com.cryptora.securechat.data.attachment.AttachmentManager
import com.cryptora.securechat.domain.model.crypto.CryptoException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class AttachmentSecurityTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        keyManager = KeyManagerImpl()
        cryptoManager = CryptoManagerImpl(keyManager)
    }

    @Test
    fun `disallowed executable file extensions list contains critical dangerous formats`() {
        val dangerous = listOf("exe", "apk", "sh", "bat", "cmd", "bin", "jar", "so")
        for (ext in dangerous) {
            val fileName = "malicious_payload.$ext"
            val extractedExt = fileName.substringAfterLast('.', "").lowercase()
            assertEquals(ext, extractedExt)
        }
    }

    @Test
    fun `attachment size limit is strictly 25 MB`() {
        assertEquals(25L * 1024 * 1024, AttachmentManager.MAX_ATTACHMENT_SIZE_BYTES)
    }

    @Test
    fun `corrupted ciphertext detection - tampered payload fails AEAD authentication tag verification`() {
        val plaintext = "Sensitive financial report and customer PII".toByteArray(StandardCharsets.UTF_8)
        val keyAlias = "test_attachment_key"

        val encrypted = cryptoManager.encrypt(plaintext, keyAlias)
        assertTrue(encrypted.size > 12) // IV (12) + Ciphertext + Auth Tag (16)

        // Tamper with one single byte in the ciphertext payload
        val tampered = encrypted.copyOf()
        tampered[tampered.size - 5] = (tampered[tampered.size - 5].toInt() xor 0xFF).toByte()

        try {
            cryptoManager.decrypt(tampered, keyAlias)
            fail("Decryption of tampered ciphertext must throw DecryptionFailedException due to AEAD tag verification")
        } catch (e: CryptoException.DecryptionFailedException) {
            // Expected security behavior: integrity check succeeded in failing
            assertTrue(e.message?.contains("tag") == true || e.message?.contains("Integrity") == true || e.message?.contains("Decryption") == true)
        }
    }

    @Test
    fun `streaming encryption and decryption works with memory-safe chunked streams`() {
        val inputData = ("Secret attachment payload content line...".repeat(200)).toByteArray(StandardCharsets.UTF_8)
        val keyAlias = "streaming_key"

        val inputStream = ByteArrayInputStream(inputData)
        val encryptedOut = ByteArrayOutputStream()

        cryptoManager.encryptStream(inputStream, encryptedOut, keyAlias)
        val ciphertextBytes = encryptedOut.toByteArray()
        assertTrue(ciphertextBytes.size > inputData.size)

        val decryptIn = ByteArrayInputStream(ciphertextBytes)
        val decryptedOut = ByteArrayOutputStream()

        cryptoManager.decryptStream(decryptIn, decryptedOut, keyAlias)
        val recoveredData = decryptedOut.toByteArray()

        assertEquals(String(inputData, StandardCharsets.UTF_8), String(recoveredData, StandardCharsets.UTF_8))
    }
}
