package com.cryptora.securechat.core.security

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

interface KeyManager {
    /**
     * Retrieves existing hardware-backed SecretKey from AndroidKeyStore or creates a new one
     * using AES-256 in GCM mode.
     */
    fun getOrCreateMasterKey(alias: String): SecretKey

    /**
     * Checks if a key exists within the KeyStore.
     */
    fun hasKey(alias: String): Boolean

    /**
     * Permanently deletes a key from the KeyStore.
     */
    fun deleteKey(alias: String)

    /**
     * Generates a single-use ephemeral AES-256 key.
     */
    fun generateEphemeralKey(): SecretKey

    /**
     * Generates an EC key pair (secp256r1) for ECDH key agreement.
     * If alias is provided, key is stored in the KeyStore.
     */
    fun generateAsymmetricKeyPair(alias: String? = null): KeyPair

    /**
     * Retrieves an asymmetric keypair by alias from the KeyStore.
     */
    fun getAsymmetricKeyPair(alias: String): KeyPair?

    /**
     * Deletes an asymmetric keypair by alias from the KeyStore.
     */
    fun deleteAsymmetricKey(alias: String)

    /**
     * Computes ECDH shared secret from private key and recipient public key.
     */
    fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray

    /**
     * Derives a 256-bit AES symmetric key from a shared secret using standard HKDF-SHA256 (RFC 5869).
     */
    fun deriveKeyHkdf(
        sharedSecret: ByteArray,
        salt: ByteArray = ByteArray(32),
        info: ByteArray = "Cryptora-E2EE-v1".toByteArray(Charsets.UTF_8)
    ): SecretKey

    /**
     * Securely wraps a content SecretKey using AES-256-GCM.
     */
    fun wrapKey(keyToWrap: SecretKey, wrappingKey: SecretKey): ByteArray

    /**
     * Unwraps a content SecretKey using AES-256-GCM.
     */
    fun unwrapKey(wrappedKeyBytes: ByteArray, unwrappingKey: SecretKey): SecretKey

    /**
     * Rotates master key by creating a new versioned alias.
     */
    fun rotateMasterKey(baseAlias: String, newVersion: Int): Pair<String, SecretKey>
}
