package com.cryptora.securechat.data.auth

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.network.otp.OtpProvider
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.PasswordHasher
import com.cryptora.securechat.core.security.SecureStorage
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class PersistedAccount(
    val id: String,
    val username: String,
    val fullName: String,
    val mobileNumber: String,
    val passwordSaltedHash: String,
    val publicKey: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val createdAt: Long
)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val passwordHasher: PasswordHasher,
    private val otpProvider: OtpProvider,
    private val keyManager: KeyManager,
    private val dispatchers: DispatcherProvider
) : AuthRepository {

    companion object {
        private const val KEY_SESSION_TOKEN = "auth_session_token"
        private const val KEY_CURRENT_USER_ID = "auth_current_user_id"
        private const val KEY_USER_REGISTRY_PREFIX = "user_account_"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val _authenticatedUser = MutableStateFlow<User?>(null)

    override fun getAuthenticatedUser(): Flow<User?> = _authenticatedUser.asStateFlow()

    override suspend fun checkUsernameAvailability(username: String): Resource<Boolean> = withContext(dispatchers.io) {
        val cleanUsername = username.lowercase().trim()
        val existingData = secureStorage.getString(KEY_USER_REGISTRY_PREFIX + cleanUsername)
        val isAvailable = existingData.isNullOrBlank()
        Resource.Success(isAvailable)
    }

    override suspend fun sendOtp(mobileNumber: String, countryCode: String): Resource<String> = withContext(dispatchers.io) {
        otpProvider.sendOtp(mobileNumber, countryCode)
    }

    override suspend fun verifyOtp(sessionId: String, otp: String): Resource<Boolean> = withContext(dispatchers.io) {
        otpProvider.verifyOtp(sessionId, otp)
    }

    override suspend fun registerUser(
        fullName: String,
        username: String,
        mobileNumber: String,
        password: String,
        avatarUrl: String?,
        bio: String
    ): Resource<User> = withContext(dispatchers.io) {
        val cleanUsername = username.lowercase().trim()

        // 1. Verify username uniqueness
        if (!secureStorage.getString(KEY_USER_REGISTRY_PREFIX + cleanUsername).isNullOrBlank()) {
            return@withContext Resource.Error(AppError.Validation("Username '@$cleanUsername' is already taken"))
        }

        // 2. Hash password with salted PBKDF2
        val saltedHash = passwordHasher.hashPassword(password)

        // 3. Initialize hardware-backed key in AndroidKeyStore
        val keyAlias = "user_master_$cleanUsername"
        keyManager.getOrCreateMasterKey(keyAlias)
        val publicKey = "pk_cryptora_${cleanUsername}_${UUID.randomUUID().toString().take(8)}"

        val userId = "usr_${System.currentTimeMillis()}"
        val user = User(
            id = userId,
            username = cleanUsername,
            fullName = fullName.trim(),
            mobileNumber = mobileNumber,
            publicKey = publicKey,
            avatarUrl = avatarUrl,
            bio = bio.trim(),
            isVerified = true
        )

        // 4. Persist account safely in encrypted storage
        val account = PersistedAccount(
            id = user.id,
            username = user.username,
            fullName = user.fullName,
            mobileNumber = user.mobileNumber,
            passwordSaltedHash = saltedHash,
            publicKey = user.publicKey,
            avatarUrl = user.avatarUrl,
            bio = user.bio,
            createdAt = user.createdAt
        )
        secureStorage.saveString(KEY_USER_REGISTRY_PREFIX + cleanUsername, json.encodeToString(account))

        // 5. Establish session
        val sessionToken = "tok_${UUID.randomUUID()}"
        secureStorage.saveString(KEY_SESSION_TOKEN, sessionToken)
        secureStorage.saveString(KEY_CURRENT_USER_ID, cleanUsername)

        _authenticatedUser.value = user
        Resource.Success(user)
    }

    override suspend fun loginUser(username: String, password: String): Resource<User> = withContext(dispatchers.io) {
        val cleanUsername = username.lowercase().trim()
        val accountJson = secureStorage.getString(KEY_USER_REGISTRY_PREFIX + cleanUsername)
            ?: return@withContext Resource.Error(AppError.Unauthorized("Account '@$cleanUsername' not found. Please register first."))

        val account = try {
            json.decodeFromString<PersistedAccount>(accountJson)
        } catch (e: Exception) {
            return@withContext Resource.Error(AppError.Cryptography("Failed to read account record"))
        }

        val passwordValid = passwordHasher.verifyPassword(password, account.passwordSaltedHash)
        if (!passwordValid) {
            return@withContext Resource.Error(AppError.Unauthorized("Invalid password. Please check your credentials."))
        }

        val user = User(
            id = account.id,
            username = account.username,
            fullName = account.fullName,
            mobileNumber = account.mobileNumber,
            publicKey = account.publicKey,
            avatarUrl = account.avatarUrl,
            bio = account.bio,
            isVerified = true,
            createdAt = account.createdAt
        )

        // Establish session
        val sessionToken = "tok_${UUID.randomUUID()}"
        secureStorage.saveString(KEY_SESSION_TOKEN, sessionToken)
        secureStorage.saveString(KEY_CURRENT_USER_ID, cleanUsername)

        _authenticatedUser.value = user
        Resource.Success(user)
    }

    override suspend fun restoreSession(): Resource<User?> = withContext(dispatchers.io) {
        val sessionToken = secureStorage.getString(KEY_SESSION_TOKEN)
        val currentUsername = secureStorage.getString(KEY_CURRENT_USER_ID)

        if (sessionToken.isNullOrBlank() || currentUsername.isNullOrBlank()) {
            _authenticatedUser.value = null
            return@withContext Resource.Success(null)
        }

        val accountJson = secureStorage.getString(KEY_USER_REGISTRY_PREFIX + currentUsername)
        if (accountJson.isNullOrBlank()) {
            _authenticatedUser.value = null
            return@withContext Resource.Success(null)
        }

        val account = try {
            json.decodeFromString<PersistedAccount>(accountJson)
        } catch (e: Exception) {
            _authenticatedUser.value = null
            return@withContext Resource.Success(null)
        }

        val user = User(
            id = account.id,
            username = account.username,
            fullName = account.fullName,
            mobileNumber = account.mobileNumber,
            publicKey = account.publicKey,
            avatarUrl = account.avatarUrl,
            bio = account.bio,
            isVerified = true,
            createdAt = account.createdAt
        )

        _authenticatedUser.value = user
        Resource.Success(user)
    }

    override suspend fun logout(): Resource<Unit> = withContext(dispatchers.io) {
        secureStorage.remove(KEY_SESSION_TOKEN)
        secureStorage.remove(KEY_CURRENT_USER_ID)
        _authenticatedUser.value = null
        Resource.Success(Unit)
    }
}
