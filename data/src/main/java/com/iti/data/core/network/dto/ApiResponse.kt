package com.iti.data.core.network.dto

import kotlinx.serialization.Serializable


@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val success: Boolean = false,
    val message: String? = null,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: String? = null,
)
