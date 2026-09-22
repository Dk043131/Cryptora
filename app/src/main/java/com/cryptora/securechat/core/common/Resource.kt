package com.cryptora.securechat.core.common

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val error: AppError, val cause: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    inline fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error, cause)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError, Throwable?) -> Unit): Resource<T> {
        if (this is Error) action(error, cause)
        return this
    }
}
