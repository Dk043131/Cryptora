package com.cryptora.securechat.data.user

import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.security.SecureStorage
import com.cryptora.securechat.domain.model.UserProfile
import com.cryptora.securechat.domain.repository.UserRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class PersistedAccountData(
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
class UserRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val dispatchers: DispatcherProvider
) : UserRepository {

    companion object {
        private const val KEY_CURRENT_USER_ID = "auth_current_user_id"
        private const val KEY_USER_REGISTRY_PREFIX = "user_account_"
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Built-in verified contacts for local discovery & testing
    private val defaultDiscoveryDirectory = listOf(
        UserProfile(
            id = "usr_harshanth_01",
            username = "harshanth",
            fullName = "Harshanth",
            avatarUrl = "⚡",
            bio = "Cryptora Security Contributor • E2EE Active",
            publicKeyFingerprint = "7F:3A:9C:12",
            isVerified = true
        ),
        UserProfile(
            id = "usr_dk_01",
            username = "dk",
            fullName = "DK",
            avatarUrl = "⚡",
            bio = "Cryptora Core Architecture • Enclave Lead",
            publicKeyFingerprint = "DK:04:31:31",
            isVerified = true
        ),
        UserProfile(
            id = "usr_sarumathy_02",
            username = "sarumathy",
            fullName = "Sarumathy",
            avatarUrl = "🌸",
            bio = "Hardware Security Specialist • AES-GCM",
            publicKeyFingerprint = "SA:RU:MA:88",
            isVerified = true
        ),
        UserProfile(
            id = "usr_karthiga_03",
            username = "karthiga",
            fullName = "Karthiga",
            avatarUrl = "🌟",
            bio = "Quantum Key Distribution Contributor",
            publicKeyFingerprint = "KA:RT:HI:99",
            isVerified = true
        ),
        UserProfile(
            id = "usr_alex_02",
            username = "alex_rivera",
            fullName = "Alex Rivera",
            avatarUrl = "🛡️",
            bio = "Core Protocol Lead",
            publicKeyFingerprint = "1A:8D:4F:72",
            isVerified = true
        ),
        UserProfile(
            id = "usr_elena_03",
            username = "elena_k",
            fullName = "Elena Rostova",
            avatarUrl = "💼",
            bio = "Time-Lock Architecture Reviewer",
            publicKeyFingerprint = "9C:42:0E:5B",
            isVerified = true
        )
    )

    override suspend fun searchUsers(query: String): Resource<List<UserProfile>> = withContext(dispatchers.io) {
        val cleanQuery = query.lowercase().trim().removePrefix("@")
        if (cleanQuery.isBlank()) {
            return@withContext Resource.Success(emptyList())
        }

        val currentUsername = secureStorage.getString(KEY_CURRENT_USER_ID).orEmpty()

        // 1. Check local registry in SecureStorage
        val registeredAccountJson = secureStorage.getString(KEY_USER_REGISTRY_PREFIX + cleanQuery)
        val registeredUsers = mutableListOf<UserProfile>()

        if (!registeredAccountJson.isNullOrBlank()) {
            try {
                val acc = json.decodeFromString<PersistedAccountData>(registeredAccountJson)
                if (acc.username != currentUsername) {
                    registeredUsers.add(
                        UserProfile(
                            id = acc.id,
                            username = acc.username,
                            fullName = acc.fullName.ifBlank { acc.username },
                            avatarUrl = acc.avatarUrl ?: "👤",
                            bio = acc.bio.ifBlank { "Verified Cryptora User" },
                            publicKeyFingerprint = acc.publicKey.takeLast(8),
                            isVerified = true
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Query discovery directory
        val directoryMatches = defaultDiscoveryDirectory.filter {
            (it.username.contains(cleanQuery) || it.fullName.lowercase().contains(cleanQuery)) &&
                    it.username != currentUsername
        }

        // Combine and deduplicate
        val allMatches = (registeredUsers + directoryMatches)
            .distinctBy { it.username }

        Resource.Success(allMatches)
    }

    override suspend fun searchUserByUsername(username: String): Resource<UserProfile?> = withContext(dispatchers.io) {
        val clean = username.lowercase().trim().removePrefix("@")
        val searchResult = searchUsers(clean)
        searchResult.map { list -> list.find { it.username == clean } }
    }

    override suspend fun getUserPublicKey(userId: String): Resource<String> = withContext(dispatchers.io) {
        if (userId.isBlank()) {
            return@withContext Resource.Error(AppError.Validation("User ID is required"))
        }
        Resource.Success("pk_$userId")
    }

    override suspend fun getUserProfile(userId: String): Resource<UserProfile> = withContext(dispatchers.io) {
        val match = defaultDiscoveryDirectory.find { it.id == userId }
            ?: UserProfile(
                id = userId,
                username = "user_$userId",
                fullName = "Cryptora User",
                avatarUrl = "👤",
                bio = "End-to-End Encrypted",
                publicKeyFingerprint = "PK:${userId.takeLast(6)}"
            )
        Resource.Success(match)
    }
}
