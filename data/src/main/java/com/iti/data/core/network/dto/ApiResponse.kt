package com.iti.data.core.network.dto

import kotlinx.serialization.Serializable


@Serializable
data class ApiResponse<T>(
    val success: Boolean? = null,
    val status: String? = null,
    val statusCode: Int? = null,
    val message: String? = null,
    val data: T? = null,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: String? = null,
) {
    /**
     * The backend does not always populate [success]; some envelopes only carry [status],
     * [statusCode], or a bare [data] payload on HTTP 200. Treating a missing [success] as
     * `false` breaks login/refresh even when tokens are present.
     */
    val isSuccessful: Boolean
        get() = success == true ||
            status.equals("success", ignoreCase = true) ||
            (statusCode != null && statusCode in HTTP_SUCCESS_RANGE) ||
            (success == null && status == null && statusCode == null && data != null)

    private companion object {
        val HTTP_SUCCESS_RANGE = 200..299
    }
}

@Serializable
data class ApiErrorResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val statusCode: Int? = null,
    val message: String? = null,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: String? = null,
)
