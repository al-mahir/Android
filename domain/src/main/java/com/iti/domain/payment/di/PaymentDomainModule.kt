package com.iti.domain.payment.di

import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.ConfirmCardPaymentUseCase
import com.iti.domain.payment.usecase.ConfirmWalletPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import org.koin.dsl.module

val paymentDomainModule = module {
    factory { CreatePaymentIntentionUseCase(get()) }
    factory { ConfirmWalletPaymentUseCase(get()) }
    factory { ConfirmCardPaymentUseCase(get()) }
    factory { ActivateSubscriptionAfterPaymentUseCase(get()) }
}
