package com.cryptora.securechat.core.security

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {

    companion object {
        private const val ITERATIONS = 10_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    private val secureRandom = SecureRandom()

    /**
     * Hashes a plaintext password using salted PBKDF2-HMAC-SHA256.
     * Returns: "<Base64Salt>:<Base64Hash>"
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        return "$saltBase64:$hashBase64"
    }

    /**
     * Verifies if input password matches the stored salted hash.
     */
    fun verifyPassword(password: String, storedSaltedHash: String): Boolean {
        val parts = storedSaltedHash.split(":")
        if (parts.size != 2) return false

        val salt = try {
            Base64.getDecoder().decode(parts[0])
        } catch (e: Exception) {
            return false
        }
        val expectedHash = try {
            Base64.getDecoder().decode(parts[1])
        } catch (e: Exception) {
            return false
        }

        val testHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return slowEquals(expectedHash, testHash)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        return try {
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            skf.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Unsupported PBKDF2 algorithm", e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException("Invalid key spec for PBKDF2", e)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Constant-time byte array comparison to protect against timing attacks.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}
