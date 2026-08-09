package com.iti.data.payment.di

import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.PaymentFakeDataSource
import com.iti.data.payment.repository.PaymentRepositoryImpl
import com.iti.domain.payment.repository.PaymentRepository
import org.koin.dsl.module

val paymentDataModule = module {
    single<PaymentDataSource> { PaymentFakeDataSource() }
    single { PaymentRepositoryImpl(get()) }
    single<PaymentRepository> { get<PaymentRepositoryImpl>() }
}
