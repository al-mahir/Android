package com.iti.presentation.payment.checkout.di

import com.iti.presentation.payment.checkout.CheckoutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentPresentationModule = module {
    viewModel { (packageId: String) ->
        CheckoutViewModel(packageId, get(), get(), get(), get(), get(), get())
    }
}
