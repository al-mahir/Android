package com.iti.data.mapper

import com.iti.data.dto.SubscriptionDto
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPlan

internal fun SubscriptionDto.toDomain(): Subscription = Subscription(
    plan = plan.toPlan(),
    renewsAtEpochMillis = renewsAtEpochMillis,
)


private fun String.toPlan(): SubscriptionPlan = when (lowercase()) {
    "premium" -> SubscriptionPlan.PREMIUM
    else -> SubscriptionPlan.NONE
}
