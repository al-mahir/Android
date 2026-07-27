package com.iti.data.core.error

import com.iti.data.core.network.dto.ApiErrorResponse
import com.iti.data.core.network.dto.ApiResponse
import com.iti.domain.core.DomainError
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException


internal fun ApiResponse<*>.toDomainError(): DomainError {
    val reason = message ?: UNKNOWN_ERROR
    val errors = fieldErrors.orEmpty()
    return if (errors.isEmpty()) {
        DomainError.ServerError(reason)
    } else {
        DomainError.ValidationError(reason, errors)
    }
}


internal suspend fun Throwable.toDomainError(): DomainError {
    if (this !is ResponseException) return DomainError.NetworkError(this)

    val status = response.status
    val error = runCatching { response.body<ApiErrorResponse>() }.getOrNull()
    val reason = error?.message ?: status.description
    val fieldErrors = error?.fieldErrors.orEmpty()

    return when (status.value) {
        HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE_ENTITY -> DomainError.ValidationError(reason, fieldErrors)
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> DomainError.Unauthorized(reason)
        HTTP_NOT_FOUND -> DomainError.NotFound(reason)
        HTTP_CONFLICT -> DomainError.ConflictError(reason, fieldErrors)
        else -> DomainError.ServerError(reason, status.value)
    }
}

private const val UNKNOWN_ERROR = "Something went wrong. Please try again."

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE_ENTITY = 422
