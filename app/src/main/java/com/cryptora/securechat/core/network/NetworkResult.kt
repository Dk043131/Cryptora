package com.cryptora.securechat.core.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T, val statusCode: Int) : NetworkResult<T>()
    data class Error(val message: String, val statusCode: Int? = null, val cause: Throwable? = null) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}
