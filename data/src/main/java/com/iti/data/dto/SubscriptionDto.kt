package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDto(
    @SerialName("plan") val plan: String,
    @SerialName("renews_at") val renewsAtEpochMillis: Long? = null,
    @SerialName("active_package_id") val activePackageId: String? = null,
)
