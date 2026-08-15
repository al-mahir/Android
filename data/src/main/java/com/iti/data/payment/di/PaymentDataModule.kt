package com.iti.data.payment.di

import com.iti.data.core.di.AlmahirClient
import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.remote.PaymentRemoteDataSource
import com.iti.data.payment.repository.PaymentRepositoryImpl
import com.iti.domain.payment.repository.PaymentRepository
import org.koin.dsl.module

val paymentDataModule = module {
    single<PaymentDataSource> { PaymentRemoteDataSource(client = get(AlmahirClient)) }
    single { PaymentRepositoryImpl(get()) }
    single<PaymentRepository> { get<PaymentRepositoryImpl>() }
}
