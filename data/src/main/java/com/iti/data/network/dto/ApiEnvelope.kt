package com.iti.data.network.dto

import kotlinx.serialization.Serializable

/** The `{ "success": true, "data": T }` envelope used by the pre-existing Sheikh Management API. */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T? = null,
)


