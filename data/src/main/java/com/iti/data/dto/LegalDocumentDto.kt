package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LegalDocumentDto(
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("updated_at") val updatedAtEpochMillis: Long? = null,
)
