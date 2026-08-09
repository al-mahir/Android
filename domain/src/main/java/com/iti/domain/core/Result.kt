package com.iti.domain.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

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
    data class NotFound(val message: String) : DomainError()
    data class Unknown(val exception: Throwable) : DomainError()
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

inline fun <T, R> Result<T>.fold(onSuccess: (T) -> R, onError: (DomainError) -> R): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(error)
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (DomainError) -> Unit): Result<T> {
    if (this is Result.Error) action(error)
    return this
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
}

/**
 * Wraps each successfully-emitted value in [Result.Success] and turns any upstream exception
 * (other than [CancellationException]) into a single terminal [Result.Error] via [mapError],
 * so collectors never see a raw thrown exception — only [Result] values.
 */
fun <T> Flow<T>.asResult(mapError: (Throwable) -> DomainError = { DomainError.Unknown(it) }): Flow<Result<T>> =
    map<T, Result<T>> { Result.Success(it) }.catch { throwable ->
        if (throwable is CancellationException) throw throwable
        emit(Result.Error(mapError(throwable)))
    }

suspend inline fun <T> resultOf(
    mapError: (Throwable) -> DomainError = { DomainError.Unknown(it) },
    block: suspend () -> T,
): Result<T> = try {
    Result.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.Error(mapError(throwable))
}
