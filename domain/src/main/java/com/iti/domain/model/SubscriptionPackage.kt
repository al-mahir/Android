package com.iti.domain.model

/**
 * A purchasable subscription package as published by `GET /api/payment/packages`.
 *
 * [code] is the backend's identifier — it is what gets sent as `packageId` when creating a
 * payment intention and what [Subscription.activePackageId] is matched against.
 *
 * [priceAmount] is the price for one [durationDays] period, not a normalized monthly price, and
 * it is in WHOLE currency units (EGP), not minor units — despite the wire field being named
 * `priceMinorUnits`, the backend sends major units there (10000 means 10000 EGP, not 100.00).
 */
data class SubscriptionPackage(
    val code: String,
    val name: String,
    val description: String?,
    val priceAmount: Long,
    val currencyCode: String,
    val meetingMinutesAllowed: Int,
    val durationDays: Int,
    val features: List<String>,
)
