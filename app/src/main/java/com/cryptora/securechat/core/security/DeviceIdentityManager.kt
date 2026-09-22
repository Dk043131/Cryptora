package com.cryptora.securechat.core.security

import com.cryptora.securechat.domain.model.crypto.DeviceIdentity
import java.security.PrivateKey
import java.security.PublicKey

interface DeviceIdentityManager {
    /**
     * Retrieves or generates the device's asymmetric identity keypair.
     */
    fun getOrCreateDeviceIdentity(): DeviceIdentity

    /**
     * Returns existing device identity if present, or null.
     */
    fun getDeviceIdentity(): DeviceIdentity?

    /**
     * Rotates device identity key, incrementing key version.
     */
    fun rotateIdentityKey(): DeviceIdentity

    /**
     * Returns the device's private identity key.
     */
    fun getPrivateKey(): PrivateKey

    /**
     * Returns the device's public identity key.
     */
    fun getPublicKey(): PublicKey

    /**
     * Returns public key encoded as Base64 (X.509 SubjectPublicKeyInfo).
     */
    fun getPublicKeyBase64(): String

    /**
     * Returns SHA-256 human-readable fingerprint of the public key (for Safety Number verification).
     */
    fun getIdentityFingerprint(): String

    /**
     * Returns current identity key version.
     */
    fun getCurrentKeyVersion(): Int

    /**
     * Securely deletes the device identity key (crypto-shredding).
     */
    fun deleteIdentityKey()
}
