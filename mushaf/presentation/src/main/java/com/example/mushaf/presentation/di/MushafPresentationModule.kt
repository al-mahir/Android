package com.example.mushaf.presentation.di

import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.presentation.MushafViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mushafPresentationModule = module {
    factory { GetPageUseCase(get()) }
    factory { ObserveReaderPreferencesUseCase(get()) }
    factory { SetTajweedEnabledUseCase(get()) }
    factory { SaveLastPageUseCase(get()) }

    factory { com.example.mushaf.domain.usecase.GetLastReadUseCase() }
    factory { com.example.mushaf.domain.usecase.search.SearchSurahUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchJuzUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchHizbUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchPageUseCase(get()) }
    factory { SearchAyahUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.GetTargetPageUseCase(get()) }

    viewModel { MushafViewModel(get(), get(), get(), get()) }
    viewModel { 
        com.example.mushaf.presentation.search.MushafSearchViewModel(
            getLastReadUseCase = get(),
            searchSurahUseCase = get(),
            searchJuzUseCase = get(),
            searchAyahUseCase = get(),
            getTargetPageUseCase = get(),
            saveLastPageUseCase = get()
        )
    }
}
