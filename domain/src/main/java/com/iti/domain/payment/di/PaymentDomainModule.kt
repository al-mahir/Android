package com.iti.domain.payment.di

import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.GetPaymentStatusUseCase
import org.koin.dsl.module

val paymentDomainModule = module {
    factory { CreatePaymentIntentionUseCase(get()) }
    factory { GetPaymentStatusUseCase(get()) }
    factory { ActivateSubscriptionAfterPaymentUseCase(get()) }
}
