package com.iti.domain.core

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: DomainError) : Result<Nothing>()
}

sealed class DomainError {
    data class NetworkError(val exception: Throwable) : DomainError()
    data class ServerError(val message: String, val code: Int? = null) : DomainError()
    data class ValidationError(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : DomainError()
    data class ConflictError(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : DomainError()
    data class Unauthorized(val message: String) : DomainError()
    data class Unknown(val exception: Throwable) : DomainError()
}
