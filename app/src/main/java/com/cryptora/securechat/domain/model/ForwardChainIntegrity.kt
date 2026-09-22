package com.cryptora.securechat.domain.model

data class ForwardChainIntegrity(
    val rootMessageId: String,
    val isVerified: Boolean,
    val chainDepth: Int,
    val events: List<ForwardEvent>,
    val brokenAtEventId: String? = null,
    val validationMessage: String = ""
)
