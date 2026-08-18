package com.iti.domain.payment.di

import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.GetPaymentStatusUseCase
import com.iti.domain.usecase.subscription.CheckMeetingEligibilityUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionMinutesUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import org.koin.dsl.module

/**
 * Everything that needs a `PaymentRepository`. Loaded by the student app only — the sheikh app
 * earns rather than pays and never registers a payment graph, so these definitions must not
 * live in the shared `presentationModule`: a definition whose dependency is missing fails at
 * *creation* time, which `getOrNull()` cannot rescue.
 */
val paymentDomainModule = module {
    factory { CreatePaymentIntentionUseCase(get()) }
    factory { GetPaymentStatusUseCase(get()) }
    factory { ActivateSubscriptionAfterPaymentUseCase(get()) }
    factory { GetSubscriptionPackagesUseCase(get()) }
    factory { GetSubscriptionMinutesUseCase(get()) }
    factory { CheckMeetingEligibilityUseCase(get()) }
}
