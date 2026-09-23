package com.cryptora.securechat.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TimeResponse(
    val serverTime: Long
)

@Serializable
data class HealthResponse(
    val status: String,
    val serverTime: Long? = null
)

@Serializable
data class RegisterPolicyRequest(
    val messageId: String,
    val accessMode: String,
    val expiresAt: Long?,
    val maxExpiresAt: Long? = null,
    val forwardingPolicy: String,
    val approvalRequired: Boolean,
    val ownerId: String
)

@Serializable
data class CreateAccessRequestDto(
    val messageId: String,
    val requesterId: String,
    val requesterUsername: String,
    val requestedDuration: Long,
    val contentTitle: String
)

@Serializable
data class RespondAccessRequestDto(
    val responderUserId: String,
    val approved: Boolean,
    val finalDurationMillis: Long? = null
)

@Serializable
data class RevokePolicyRequest(
    val callerUserId: String
)

@Serializable
data class SyncExpiredRequest(
    val messageIds: List<String>
)

@Serializable
data class SyncExpiredResponse(
    val expired: List<String>,
    val serverTime: Long? = null
)

@Serializable
data class RespondAccessResponse(
    val grant: AccessGrantResponse? = null,
    val success: Boolean? = null,
    val status: String? = null
)

interface CryptoraApiService {
    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("time")
    suspend fun getServerTime(): Response<TimeResponse>

    @POST("policies")
    suspend fun registerPolicy(@Body request: RegisterPolicyRequest): Response<Unit>

    @GET("policies/{messageId}/validate")
    suspend fun validateAccess(
        @Path("messageId") messageId: String,
        @Query("requesterId") requesterId: String
    ): Response<AccessValidationResponse>

    @POST("access-requests")
    suspend fun createAccessRequest(@Body request: CreateAccessRequestDto): Response<AccessRequestResponse>

    @POST("access-requests/{requestId}/respond")
    suspend fun respondToAccessRequest(
        @Path("requestId") requestId: String,
        @Body request: RespondAccessRequestDto
    ): Response<RespondAccessResponse>

    @POST("policies/{messageId}/revoke")
    suspend fun revokePolicy(
        @Path("messageId") messageId: String,
        @Body request: RevokePolicyRequest
    ): Response<Unit>

    @POST("sync/expired")
    suspend fun syncExpired(@Body request: SyncExpiredRequest): Response<SyncExpiredResponse>
}
