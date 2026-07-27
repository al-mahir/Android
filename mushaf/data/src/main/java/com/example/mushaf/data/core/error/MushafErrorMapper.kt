package com.example.mushaf.data.core.error

import android.database.sqlite.SQLiteException
import com.example.mushaf.data.recite.remote.LiveSessionException
import com.iti.domain.core.DomainError
import io.ktor.client.plugins.ResponseException
import java.io.IOException

/**
 * Maps a raw [Throwable] from anywhere in the Mushaf data layer to a [DomainError]. Unlike
 * Almahir's backend, Mushaf's REST/WS calls don't return a `{ "success": ... }` envelope, so
 * only the HTTP status is available for classification; local SQLite/asset failures have no
 * status at all and fall back to [DomainError.Unknown] with the original exception attached.
 */
internal fun Throwable.toDomainError(): DomainError = when (this) {
    is ResponseException -> when (response.status.value) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> DomainError.Unauthorized(response.status.description)
        HTTP_NOT_FOUND -> DomainError.NotFound(response.status.description)
        else -> DomainError.ServerError(response.status.description, response.status.value)
    }
    is LiveSessionException -> DomainError.ServerError(message ?: "Live recitation session failed")
    is SQLiteException -> DomainError.Unknown(this)
    is IOException -> DomainError.Unknown(this)
    else -> DomainError.NetworkError(this)
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
