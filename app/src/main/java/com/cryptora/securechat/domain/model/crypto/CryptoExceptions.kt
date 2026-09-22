package com.cryptora.securechat.domain.model.crypto

sealed class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DecryptionFailedException(
        message: String = "Decryption failed or authentication tag mismatch",
        cause: Throwable? = null
    ) : CryptoException(message, cause)

    class KeyNotFoundException(
        message: String = "Cryptographic key not found or shredded"
    ) : CryptoException(message)

    class KeyVersionMismatchException(
        message: String = "Message key version is incompatible or revoked"
    ) : CryptoException(message)

    class CorruptedCiphertextException(
        message: String = "Ciphertext payload is truncated, malformed, or tampered"
    ) : CryptoException(message)

    class InvalidKeyException(
        message: String = "Provided key is invalid or incompatible",
        cause: Throwable? = null
    ) : CryptoException(message, cause)
}
