package com.cryptora.securechat.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.domain.model.crypto.CryptoException
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManagerImpl @Inject constructor() : KeyManager {

    private val isAndroidKeyStoreAvailable: Boolean by lazy {
        try {
            KeyStore.getInstance(Constants.KEYSTORE_PROVIDER).apply { load(null) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private val keyStore: KeyStore by lazy {
        if (isAndroidKeyStoreAvailable) {
            KeyStore.getInstance(Constants.KEYSTORE_PROVIDER).apply { load(null) }
        } else {
            KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        }
    }

    // In-memory fallback cache for software keys or non-Android unit test environments
    private val memoryKeyMap = ConcurrentHashMap<String, SecretKey>()
    private val memoryKeyPairMap = ConcurrentHashMap<String, KeyPair>()

    override fun getOrCreateMasterKey(alias: String): SecretKey {
        if (isAndroidKeyStoreAvailable) {
            val existing = keyStore.getKey(alias, null) as? SecretKey
            if (existing != null) {
                return existing
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                Constants.KEYSTORE_PROVIDER
            )

            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(Constants.AES_KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } else {
            return memoryKeyMap.computeIfAbsent(alias) {
                generateEphemeralKey()
            }
        }
    }

    override fun hasKey(alias: String): Boolean {
        return if (isAndroidKeyStoreAvailable) {
            keyStore.containsAlias(alias)
        } else {
            memoryKeyMap.containsKey(alias) || memoryKeyPairMap.containsKey(alias)
        }
    }

    override fun deleteKey(alias: String) {
        if (isAndroidKeyStoreAvailable && keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
        memoryKeyMap.remove(alias)
        memoryKeyPairMap.remove(alias)
    }

    override fun generateEphemeralKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(Constants.AES_KEY_SIZE_BITS)
        return keyGenerator.generateKey()
    }

    override fun generateAsymmetricKeyPair(alias: String?): KeyPair {
        val keyPair = if (alias != null && isAndroidKeyStoreAvailable) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                Constants.KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_SIGN
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        } else {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val pair = kpg.generateKeyPair()
            if (alias != null) {
                memoryKeyPairMap[alias] = pair
            }
            pair
        }
        return keyPair
    }

    override fun getAsymmetricKeyPair(alias: String): KeyPair? {
        if (isAndroidKeyStoreAvailable && keyStore.containsAlias(alias)) {
            val privateKey = keyStore.getKey(alias, null) as? PrivateKey
            val certificate = keyStore.getCertificate(alias)
            val publicKey = certificate?.publicKey
            if (privateKey != null && publicKey != null) {
                return KeyPair(publicKey, privateKey)
            }
        }
        return memoryKeyPairMap[alias]
    }

    override fun deleteAsymmetricKey(alias: String) {
        deleteKey(alias)
    }

    override fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        try {
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(publicKey, true)
            return keyAgreement.generateSecret()
        } catch (e: Exception) {
            throw CryptoException.InvalidKeyException("Failed to compute ECDH key agreement: ${e.message}", e)
        }
    }

    override fun deriveKeyHkdf(
        sharedSecret: ByteArray,
        salt: ByteArray,
        info: ByteArray
    ): SecretKey {
        var prk: ByteArray? = null
        var okm: ByteArray? = null
        try {
            val mac = Mac.getInstance("HmacSHA256")

            // 1. HKDF-Extract: PRK = HMAC-Hash(salt, IKM)
            val effectiveSalt = if (salt.isEmpty()) ByteArray(32) else salt
            mac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
            prk = mac.doFinal(sharedSecret)

            // 2. HKDF-Expand: OKM = HMAC-Hash(PRK, info || 0x01)
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(info)
            mac.update(0x01.toByte())
            okm = mac.doFinal()

            // Truncate to 256 bits (32 bytes) for AES-256
            val aesKeyBytes = okm.copyOf(32)
            val secretKey = SecretKeySpec(aesKeyBytes, "AES")

            // Secure memory wipe of intermediate raw array
            Arrays.fill(aesKeyBytes, 0.toByte())
            return secretKey
        } finally {
            prk?.let { Arrays.fill(it, 0.toByte()) }
            okm?.let { Arrays.fill(it, 0.toByte()) }
        }
    }

    override fun wrapKey(keyToWrap: SecretKey, wrappingKey: SecretKey): ByteArray {
        try {
            val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)

            val iv = cipher.iv
            val rawKeyBytes = keyToWrap.encoded
                ?: throw CryptoException.InvalidKeyException("Key does not support raw encoding for wrapping")

            val ciphertext = cipher.doFinal(rawKeyBytes)

            // Secure memory wipe of raw key bytes
            Arrays.fill(rawKeyBytes, 0.toByte())

            return ByteBuffer.allocate(iv.size + ciphertext.size)
                .put(iv)
                .put(ciphertext)
                .array()
        } catch (e: Exception) {
            throw CryptoException.InvalidKeyException("Key wrapping failed: ${e.message}", e)
        }
    }

    override fun unwrapKey(wrappedKeyBytes: ByteArray, unwrappingKey: SecretKey): SecretKey {
        require(wrappedKeyBytes.size > Constants.GCM_IV_LENGTH_BYTES) {
            "Wrapped key buffer too short"
        }

        var decryptedKeyBytes: ByteArray? = null
        try {
            val buffer = ByteBuffer.wrap(wrappedKeyBytes)
            val iv = ByteArray(Constants.GCM_IV_LENGTH_BYTES)
            buffer.get(iv)

            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION)
            val spec = GCMParameterSpec(Constants.GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, unwrappingKey, spec)

            decryptedKeyBytes = cipher.doFinal(ciphertext)
            val secretKey = SecretKeySpec(decryptedKeyBytes, "AES")
            return secretKey
        } catch (e: Exception) {
            throw CryptoException.DecryptionFailedException("Failed to unwrap content key: ${e.message}", e)
        } finally {
            decryptedKeyBytes?.let { Arrays.fill(it, 0.toByte()) }
        }
    }

    override fun rotateMasterKey(baseAlias: String, newVersion: Int): Pair<String, SecretKey> {
        val newAlias = "${baseAlias}_v$newVersion"
        val newKey = getOrCreateMasterKey(newAlias)
        return Pair(newAlias, newKey)
    }
}
