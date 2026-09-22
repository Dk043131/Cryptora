package com.cryptora.securechat.core.security

import com.cryptora.securechat.domain.model.crypto.CryptoException
import com.cryptora.securechat.domain.model.crypto.DeviceIdentity
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentityManagerImpl @Inject constructor(
    private val keyManager: KeyManager,
    private val secureStorage: SecureStorage
) : DeviceIdentityManager {

    companion object {
        private const val KEY_DEVICE_ID = "cryptora_device_id"
        private const val KEY_IDENTITY_KEY_VERSION = "cryptora_identity_key_version"
        private const val KEY_IDENTITY_FINGERPRINT = "cryptora_identity_fingerprint"
        private const val KEY_IDENTITY_PUBLIC_KEY = "cryptora_identity_public_key"
        private const val BASE_ALIAS = "cryptora_device_identity"
    }

    override fun getOrCreateDeviceIdentity(): DeviceIdentity {
        val existing = getDeviceIdentity()
        if (existing != null) {
            return existing
        }

        val deviceId = secureStorage.getString(KEY_DEVICE_ID) ?: UUID.randomUUID().toString().also {
            secureStorage.saveString(KEY_DEVICE_ID, it)
        }

        val version = 1
        val alias = "${BASE_ALIAS}_v$version"
        val keyPair = keyManager.generateAsymmetricKeyPair(alias)

        val pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val fingerprint = computeFingerprint(keyPair.public)

        secureStorage.saveInt(KEY_IDENTITY_KEY_VERSION, version)
        secureStorage.saveString(KEY_IDENTITY_PUBLIC_KEY, pubKeyBase64)
        secureStorage.saveString(KEY_IDENTITY_FINGERPRINT, fingerprint)

        return DeviceIdentity(
            deviceId = deviceId,
            publicKeyBase64 = pubKeyBase64,
            keyVersion = version,
            fingerprint = fingerprint
        )
    }

    override fun getDeviceIdentity(): DeviceIdentity? {
        val deviceId = secureStorage.getString(KEY_DEVICE_ID) ?: return null
        val version = secureStorage.getInt(KEY_IDENTITY_KEY_VERSION, 0)
        if (version <= 0) return null

        val pubKeyBase64 = secureStorage.getString(KEY_IDENTITY_PUBLIC_KEY) ?: return null
        val fingerprint = secureStorage.getString(KEY_IDENTITY_FINGERPRINT) ?: return null

        return DeviceIdentity(
            deviceId = deviceId,
            publicKeyBase64 = pubKeyBase64,
            keyVersion = version,
            fingerprint = fingerprint
        )
    }

    override fun rotateIdentityKey(): DeviceIdentity {
        val deviceId = secureStorage.getString(KEY_DEVICE_ID) ?: UUID.randomUUID().toString().also {
            secureStorage.saveString(KEY_DEVICE_ID, it)
        }

        val currentVersion = getCurrentKeyVersion()
        val newVersion = currentVersion + 1
        val newAlias = "${BASE_ALIAS}_v$newVersion"

        val keyPair = keyManager.generateAsymmetricKeyPair(newAlias)
        val pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val fingerprint = computeFingerprint(keyPair.public)

        secureStorage.saveInt(KEY_IDENTITY_KEY_VERSION, newVersion)
        secureStorage.saveString(KEY_IDENTITY_PUBLIC_KEY, pubKeyBase64)
        secureStorage.saveString(KEY_IDENTITY_FINGERPRINT, fingerprint)

        return DeviceIdentity(
            deviceId = deviceId,
            publicKeyBase64 = pubKeyBase64,
            keyVersion = newVersion,
            fingerprint = fingerprint
        )
    }

    override fun getPrivateKey(): PrivateKey {
        val version = getCurrentKeyVersion()
        val alias = "${BASE_ALIAS}_v$version"
        val pair = keyManager.getAsymmetricKeyPair(alias)
            ?: throw CryptoException.KeyNotFoundException("Device identity private key not found for version $version")
        return pair.private
    }

    override fun getPublicKey(): PublicKey {
        val version = getCurrentKeyVersion()
        val alias = "${BASE_ALIAS}_v$version"
        val pair = keyManager.getAsymmetricKeyPair(alias)
            ?: throw CryptoException.KeyNotFoundException("Device identity public key not found for version $version")
        return pair.public
    }

    override fun getPublicKeyBase64(): String {
        return secureStorage.getString(KEY_IDENTITY_PUBLIC_KEY) ?: run {
            getOrCreateDeviceIdentity().publicKeyBase64
        }
    }

    override fun getIdentityFingerprint(): String {
        return secureStorage.getString(KEY_IDENTITY_FINGERPRINT) ?: run {
            getOrCreateDeviceIdentity().fingerprint
        }
    }

    override fun getCurrentKeyVersion(): Int {
        val v = secureStorage.getInt(KEY_IDENTITY_KEY_VERSION, 0)
        return if (v <= 0) 1 else v
    }

    override fun deleteIdentityKey() {
        val version = getCurrentKeyVersion()
        val alias = "${BASE_ALIAS}_v$version"
        keyManager.deleteAsymmetricKey(alias)
        secureStorage.remove(KEY_IDENTITY_PUBLIC_KEY)
        secureStorage.remove(KEY_IDENTITY_FINGERPRINT)
        secureStorage.remove(KEY_IDENTITY_KEY_VERSION)
    }

    private fun computeFingerprint(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey.encoded)
        return hash.joinToString(":") { "%02X".format(it) }
    }
}
