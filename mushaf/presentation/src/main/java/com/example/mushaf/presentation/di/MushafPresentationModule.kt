package com.example.mushaf.presentation.di

import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.usecase.CancelResourceDownloadUseCase
import com.example.mushaf.domain.usecase.DeleteResourceDownloadUseCase
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveDownloadableResourcesUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.StartResourceDownloadUseCase
import com.example.mushaf.presentation.MushafViewModel
import com.example.mushaf.presentation.download.DownloadsViewModel
import com.example.mushaf.presentation.settings.MushafSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mushafPresentationModule = module {
    factory { GetPageUseCase(get()) }
    factory { ObserveReaderPreferencesUseCase(get()) }
    factory { SetTajweedEnabledUseCase(get()) }
    factory { SaveLastPageUseCase(get()) }

    factory { ObserveDownloadableResourcesUseCase(get()) }
    factory { StartResourceDownloadUseCase(get()) }
    factory { CancelResourceDownloadUseCase(get()) }
    factory { DeleteResourceDownloadUseCase(get()) }

    viewModel { MushafViewModel(get(), get(), get(), get()) }

    viewModel { MushafSettingsViewModel(get(), get()) }

    viewModel { (kind: ResourceKind) ->
        DownloadsViewModel(kind, get(), get(), get(), get())
    }
}
