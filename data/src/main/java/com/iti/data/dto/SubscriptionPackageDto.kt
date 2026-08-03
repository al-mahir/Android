package com.iti.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPackageDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("monthly_price_minor_units") val monthlyPriceMinorUnits: Long,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("features") val features: List<String>,
    @SerialName("is_recommended") val isRecommended: Boolean = false,
)
