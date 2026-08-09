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
    val isSuccessful: Boolean
        get() = success == true ||
            status.equals("success", ignoreCase = true) ||
            (statusCode != null && statusCode in 200..299) ||
            (success == null && status == null && statusCode == null)
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
