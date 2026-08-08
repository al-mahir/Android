package com.iti.domain.model


data class Subscription(
    val plan: SubscriptionPlan,
    val renewsAtEpochMillis: Long?,
    val activePackageId: String? = null,
)

enum class SubscriptionPlan {
    NONE,
    PREMIUM,
}
