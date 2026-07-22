package com.iti.domain.model


data class Subscription(
    val plan: SubscriptionPlan,
    val renewsAtEpochMillis: Long?,
)

enum class SubscriptionPlan {
    NONE,
    PREMIUM,
}
