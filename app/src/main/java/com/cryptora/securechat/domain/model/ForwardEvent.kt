package com.cryptora.securechat.domain.model

import java.security.MessageDigest

enum class ForwardApprovalStatus {
    APPROVED,
    PENDING,
    REJECTED,
    EXPIRED
}

data class ForwardEvent(
    val eventId: String,
    val rootMessageId: String,
    val secureMessageId: String,
    val parentEventId: String?,
    val fromUserId: String,
    val fromUsername: String,
    val toUserId: String,
    val toUsername: String,
    val timestamp: Long,
    val policyVersion: Int = 1,
    val approvalStatus: ForwardApprovalStatus = ForwardApprovalStatus.APPROVED,
    val previousEventHash: String,
    val eventHash: String,
    val signatureMetadata: String = ""
) {
    companion object {
        const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        fun calculateHash(
            previousEventHash: String,
            eventId: String,
            rootMessageId: String,
            fromUserId: String,
            toUserId: String,
            timestamp: Long,
            policyVersion: Int,
            approvalStatus: ForwardApprovalStatus
        ): String {
            val input = "$previousEventHash:$eventId:$rootMessageId:$fromUserId:$toUserId:$timestamp:$policyVersion:${approvalStatus.name}"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
