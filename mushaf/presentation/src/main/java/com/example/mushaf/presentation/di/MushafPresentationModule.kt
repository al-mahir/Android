package com.example.mushaf.presentation.di

import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.presentation.MushafViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mushafPresentationModule = module {
    factory { GetPageUseCase(get()) }
    factory { ObserveReaderPreferencesUseCase(get()) }
    factory { SetTajweedEnabledUseCase(get()) }
    factory { SaveLastPageUseCase(get()) }

    viewModel { MushafViewModel(get(), get(), get(), get()) }
}
