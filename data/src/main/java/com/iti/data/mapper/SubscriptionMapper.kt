package com.iti.data.mapper

import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.SubscriptionPackageDto
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.model.SubscriptionPlan

internal fun SubscriptionDto.toDomain(): Subscription = Subscription(
    plan = plan.toPlan(),
    renewsAtEpochMillis = renewsAtEpochMillis,
    activePackageId = activePackageId,
)

internal fun SubscriptionPackageDto.toDomain(): SubscriptionPackage = SubscriptionPackage(
    id = id,
    name = name,
    monthlyPriceMinorUnits = monthlyPriceMinorUnits,
    currencyCode = currencyCode,
    features = features,
    isRecommended = isRecommended,
)


private fun String.toPlan(): SubscriptionPlan = when (lowercase()) {
    "premium" -> SubscriptionPlan.PREMIUM
    else -> SubscriptionPlan.NONE
}
