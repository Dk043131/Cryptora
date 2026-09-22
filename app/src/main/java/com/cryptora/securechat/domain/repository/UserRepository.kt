package com.cryptora.securechat.domain.repository

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.UserProfile

interface UserRepository {
    suspend fun searchUsers(query: String): Resource<List<UserProfile>>
    suspend fun searchUserByUsername(username: String): Resource<UserProfile?>
    suspend fun getUserPublicKey(userId: String): Resource<String>
    suspend fun getUserProfile(userId: String): Resource<UserProfile>
}
