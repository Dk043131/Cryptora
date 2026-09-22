package com.cryptora.securechat.core.common

object Constants {
    // Database
    const val DATABASE_NAME = "cryptora_encrypted.db"

    // Preferences & Secure Storage
    const val PREFERENCES_NAME = "cryptora_secure_prefs"
    const val MASTER_KEY_ALIAS = "cryptora_master_key_v1"

    // Cryptographic Specifications
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    // Network Timeouts (seconds)
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // Forwarding Policy Constraints
    const val DEFAULT_MAX_FORWARD_DEPTH = 3
}
