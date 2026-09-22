package com.cryptora.securechat.core.security

import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.domain.model.crypto.EncryptedEnvelope
import java.io.InputStream
import java.io.OutputStream
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

interface CryptoManager {
    /**
     * Encrypts plaintext bytes using AES-256-GCM with KeyStore alias.
     * Returns: [12-byte IV] + [Ciphertext + Auth Tag]
     */
    fun encrypt(plaintext: ByteArray, keyAlias: String = Constants.MASTER_KEY_ALIAS): ByteArray

    /**
     * Decrypts combined IV + Ciphertext bytes.
     */
    fun decrypt(ciphertextWithIv: ByteArray, keyAlias: String = Constants.MASTER_KEY_ALIAS): ByteArray

    /**
     * Encrypts a UTF-8 string to Base64 encoded ciphertext.
     */
    fun encryptString(plaintext: String, keyAlias: String = Constants.MASTER_KEY_ALIAS): String

    /**
     * Decrypts Base64 encoded ciphertext back to UTF-8 plaintext.
     */
    fun decryptString(ciphertextBase64: String, keyAlias: String = Constants.MASTER_KEY_ALIAS): String

    /**
     * Encrypts plaintext bytes directly using a SecretKey with optional AAD.
     */
    fun encryptWithKey(plaintext: ByteArray, secretKey: SecretKey, associatedData: ByteArray? = null): ByteArray

    /**
     * Decrypts ciphertext bytes directly using a SecretKey with optional AAD.
     */
    fun decryptWithKey(ciphertextWithIv: ByteArray, secretKey: SecretKey, associatedData: ByteArray? = null): ByteArray

    /**
     * End-to-end envelope encryption using ephemeral ECDH (secp256r1), HKDF-SHA256, and AES-256-GCM.
     */
    fun encryptE2EE(
        plaintext: ByteArray,
        recipientPublicKey: PublicKey,
        keyVersion: Int = 1
    ): EncryptedEnvelope

    /**
     * Decrypts an end-to-end encrypted envelope using recipient private key.
     */
    fun decryptE2EE(
        envelope: EncryptedEnvelope,
        recipientPrivateKey: PrivateKey,
        expectedKeyVersion: Int? = null
    ): ByteArray

    /**
     * Streams plaintext from inputStream, encrypts in memory-efficient chunks, and writes [IV] + [Ciphertext] to outputStream.
     */
    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, keyAlias: String = Constants.MASTER_KEY_ALIAS): Long

    /**
     * Streams [IV] + [Ciphertext] from inputStream, decrypts in chunks, and writes plaintext to outputStream.
     */
    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, keyAlias: String = Constants.MASTER_KEY_ALIAS): Long

    /**
     * Streaming encryption with direct SecretKey.
     */
    fun encryptStreamWithKey(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey): Long

    /**
     * Streaming decryption with direct SecretKey.
     */
    fun decryptStreamWithKey(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey): Long

    /**
     * Secure memory wiper for sensitive byte arrays.
     */
    fun wipe(vararg byteArrays: ByteArray)
}
