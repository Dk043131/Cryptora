package com.cryptora.securechat.crypto

import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.CryptoManagerImpl
import com.cryptora.securechat.core.security.DeviceIdentityManagerImpl
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.KeyManagerImpl
import com.cryptora.securechat.core.security.SecureStorage
import com.cryptora.securechat.domain.model.crypto.CryptoException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class CryptoArchitectureTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        keyManager = KeyManagerImpl()
        cryptoManager = CryptoManagerImpl(keyManager)
    }

    @Test
    fun testEncryptDecrypt_symmetricAES256GCM() {
        val originalText = "Top-Secret Cryptora Message Payload: 12345"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val secretKey = keyManager.generateEphemeralKey()

        val ciphertextWithIv = cryptoManager.encryptWithKey(originalBytes, secretKey)
        // Ciphertext should contain 12-byte IV + original length + 16-byte GCM tag
        assertEquals(12 + originalBytes.size + 16, ciphertextWithIv.size)

        val decryptedBytes = cryptoManager.decryptWithKey(ciphertextWithIv, secretKey)
        val decryptedText = String(decryptedBytes, StandardCharsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testDecryptionFailure_wrongKey() {
        val originalBytes = "Secret Content".toByteArray(StandardCharsets.UTF_8)
        val keyAlice = keyManager.generateEphemeralKey()
        val keyBob = keyManager.generateEphemeralKey()

        val encrypted = cryptoManager.encryptWithKey(originalBytes, keyAlice)

        // Attempting to decrypt with Bob's key must fail authenticated tag check
        val exception = assertThrows(CryptoException.DecryptionFailedException::class.java) {
            cryptoManager.decryptWithKey(encrypted, keyBob)
        }
        assertTrue(exception.message?.contains("authentication tag") == true || exception.message?.contains("wrong key") == true)
    }

    @Test
    fun testDecryptionFailure_modifiedCiphertext() {
        val originalBytes = "Uncompromised Data".toByteArray(StandardCharsets.UTF_8)
        val secretKey = keyManager.generateEphemeralKey()

        val encrypted = cryptoManager.encryptWithKey(originalBytes, secretKey)

        // Tamper with one byte in the ciphertext payload (after the 12-byte IV)
        val tampered = encrypted.copyOf()
        tampered[15] = (tampered[15].toInt() xor 0xFF).toByte()

        assertThrows(CryptoException.DecryptionFailedException::class.java) {
            cryptoManager.decryptWithKey(tampered, secretKey)
        }
    }

    @Test
    fun testKeyVersionMismatch_throwsException() {
        val bobKeyPair = keyManager.generateAsymmetricKeyPair()
        val plaintext = "Envelope version test".toByteArray(StandardCharsets.UTF_8)

        val envelope = cryptoManager.encryptE2EE(
            plaintext = plaintext,
            recipientPublicKey = bobKeyPair.public,
            keyVersion = 1
        )

        // Expecting keyVersion = 2 when envelope is version 1 must throw KeyVersionMismatchException
        val exception = assertThrows(CryptoException.KeyVersionMismatchException::class.java) {
            cryptoManager.decryptE2EE(
                envelope = envelope,
                recipientPrivateKey = bobKeyPair.private,
                expectedKeyVersion = 2
            )
        }
        assertTrue(exception.message?.contains("Key version mismatch") == true)
    }

    @Test
    fun testAsymmetricE2EE_ECDH_HKDF_AESGCM_success() {
        val aliceKeyPair = keyManager.generateAsymmetricKeyPair()
        val bobKeyPair = keyManager.generateAsymmetricKeyPair()

        val secretMessage = "Confidential E2EE Payload using ECDH and HKDF-SHA256"
        val plaintext = secretMessage.toByteArray(StandardCharsets.UTF_8)

        // Alice encrypts for Bob
        val envelope = cryptoManager.encryptE2EE(
            plaintext = plaintext,
            recipientPublicKey = bobKeyPair.public,
            keyVersion = 1
        )

        assertNotNull(envelope.ephemeralPublicKeyBase64)
        assertEquals("AES-256-GCM", envelope.algorithm)
        assertEquals(1, envelope.keyVersion)

        // Bob decrypts with Bob's private key
        val decryptedBytes = cryptoManager.decryptE2EE(
            envelope = envelope,
            recipientPrivateKey = bobKeyPair.private,
            expectedKeyVersion = 1
        )

        assertEquals(secretMessage, String(decryptedBytes, StandardCharsets.UTF_8))
    }

    @Test
    fun testAsymmetricE2EE_wrongRecipientPrivateKey_fails() {
        val bobKeyPair = keyManager.generateAsymmetricKeyPair()
        val eveKeyPair = keyManager.generateAsymmetricKeyPair()

        val plaintext = "Sensitive data intended only for Bob".toByteArray(StandardCharsets.UTF_8)

        val envelope = cryptoManager.encryptE2EE(
            plaintext = plaintext,
            recipientPublicKey = bobKeyPair.public,
            keyVersion = 1
        )

        // Eve tries to decrypt Bob's message
        assertThrows(CryptoException.DecryptionFailedException::class.java) {
            cryptoManager.decryptE2EE(
                envelope = envelope,
                recipientPrivateKey = eveKeyPair.private,
                expectedKeyVersion = 1
            )
        }
    }

    @Test
    fun testAttachmentStreamEncryptionAndDecryption() {
        // Generate 128 KB simulated attachment
        val originalAttachment = ByteArray(128 * 1024) { (it % 256).toByte() }
        val secretKey = keyManager.generateEphemeralKey()

        val inputStream = ByteArrayInputStream(originalAttachment)
        val encryptedOut = ByteArrayOutputStream()

        val bytesWritten = cryptoManager.encryptStreamWithKey(inputStream, encryptedOut, secretKey)
        val encryptedData = encryptedOut.toByteArray()

        // 128KB + 12 IV + 16 Auth tag
        assertEquals((128 * 1024 + 12 + 16).toLong(), bytesWritten)
        assertEquals(bytesWritten, encryptedData.size.toLong())

        // Decrypt stream
        val encInputStream = ByteArrayInputStream(encryptedData)
        val decryptedOut = ByteArrayOutputStream()

        cryptoManager.decryptStreamWithKey(encInputStream, decryptedOut, secretKey)
        val decryptedBytes = decryptedOut.toByteArray()

        assertArrayEquals(originalAttachment, decryptedBytes)
    }

    @Test
    fun testAttachmentDecryptionFailure_tamperedStream() {
        val originalAttachment = ByteArray(32 * 1024) { 0x42.toByte() }
        val secretKey = keyManager.generateEphemeralKey()

        val inputStream = ByteArrayInputStream(originalAttachment)
        val encryptedOut = ByteArrayOutputStream()
        cryptoManager.encryptStreamWithKey(inputStream, encryptedOut, secretKey)

        val tamperedData = encryptedOut.toByteArray()
        // Tamper with payload byte
        tamperedData[50] = (tamperedData[50].toInt() xor 0x01).toByte()

        val tamperedInputStream = ByteArrayInputStream(tamperedData)
        val decryptedOut = ByteArrayOutputStream()

        assertThrows(CryptoException.DecryptionFailedException::class.java) {
            cryptoManager.decryptStreamWithKey(tamperedInputStream, decryptedOut, secretKey)
        }
    }

    @Test
    fun testKeyWrapping_wrapAndUnwrap() {
        val contentKey = keyManager.generateEphemeralKey()
        val wrappingKey = keyManager.generateEphemeralKey()

        val wrappedBytes = keyManager.wrapKey(contentKey, wrappingKey)
        assertTrue(wrappedBytes.size > 12)

        val unwrappedKey = keyManager.unwrapKey(wrappedBytes, wrappingKey)
        assertArrayEquals(contentKey.encoded, unwrappedKey.encoded)

        // Unwrap with wrong key must fail
        val wrongWrappingKey = keyManager.generateEphemeralKey()
        assertThrows(CryptoException.DecryptionFailedException::class.java) {
            keyManager.unwrapKey(wrappedBytes, wrongWrappingKey)
        }
    }

    @Test
    fun testDeviceIdentityManager_creationRotationAndShredding() {
        val inMemoryStorage = InMemorySecureStorage()
        val identityManager = DeviceIdentityManagerImpl(keyManager, inMemoryStorage)

        // 1. Create identity
        val identityV1 = identityManager.getOrCreateDeviceIdentity()
        assertEquals(1, identityV1.keyVersion)
        assertNotNull(identityV1.fingerprint)
        assertTrue(identityV1.fingerprint.contains(":"))
        assertEquals(identityV1.publicKeyBase64, identityManager.getPublicKeyBase64())

        // 2. Rotate identity key
        val identityV2 = identityManager.rotateIdentityKey()
        assertEquals(2, identityV2.keyVersion)
        assertNotEquals(identityV1.publicKeyBase64, identityV2.publicKeyBase64)
        assertNotEquals(identityV1.fingerprint, identityV2.fingerprint)

        // 3. Delete identity key (shredding)
        identityManager.deleteIdentityKey()
        assertEquals(0, inMemoryStorage.getInt("cryptora_identity_key_version"))
    }

    @Test
    fun testSecureMemoryWipe() {
        val sensitiveBuffer = byteArrayOf(0x12, 0x34, 0x56, 0x78.toByte(), 0x9A.toByte())
        cryptoManager.wipe(sensitiveBuffer)

        for (b in sensitiveBuffer) {
            assertEquals(0.toByte(), b)
        }
    }

    // Helper in-memory test implementation of SecureStorage for unit testing
    private class InMemorySecureStorage : SecureStorage {
        private val map = mutableMapOf<String, Any>()

        override fun saveString(key: String, value: String) { map[key] = value }
        override fun getString(key: String): String? = map[key] as? String
        override fun saveInt(key: String, value: Int) { map[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = (map[key] as? Int) ?: defaultValue
        override fun saveBoolean(key: String, value: Boolean) { map[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (map[key] as? Boolean) ?: defaultValue
        override fun remove(key: String) { map.remove(key) }
        override fun clear() { map.clear() }
    }
}
