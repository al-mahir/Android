package com.iti.domain.model

data class SubscriptionPackage(
    val id: String,
    val name: String,
    val monthlyPriceMinorUnits: Long,
    val currencyCode: String,
    val features: List<String>,
    val isRecommended: Boolean,
)
