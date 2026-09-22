package com.cryptora.securechat.core.common

sealed class AppError(open val message: String) {
    data class Network(override val message: String = "Network connection failed", val code: Int? = null) : AppError(message)
    data class Cryptography(override val message: String = "Cryptographic operation failed") : AppError(message)
    data class Unauthorized(override val message: String = "Authentication credentials invalid or missing") : AppError(message)
    data class NotFound(override val message: String = "Requested entity could not be found") : AppError(message)
    data class Validation(override val message: String = "Input validation failed") : AppError(message)
    data class Expired(override val message: String = "Content access duration has expired") : AppError(message)
    data class Revoked(override val message: String = "Access to this content has been revoked by the sender") : AppError(message)
    data class ForwardingDenied(override val message: String = "Forwarding permission rejected or unauthorized") : AppError(message)
    data class Storage(override val message: String = "Storage operation failed") : AppError(message)
    data class Unknown(override val message: String = "An unexpected error occurred", val cause: Throwable? = null) : AppError(message)
}
