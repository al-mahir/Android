package com.example.mushaf.presentation.di

import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.usecase.CancelResourceDownloadUseCase
import com.example.mushaf.domain.usecase.DeleteResourceDownloadUseCase
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.ObserveDownloadableResourcesUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.StartResourceDownloadUseCase
import com.example.mushaf.presentation.MushafViewModel
import com.example.mushaf.presentation.download.DownloadsViewModel
import com.example.mushaf.presentation.settings.MushafSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

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

    factory { ObserveDownloadableResourcesUseCase(get()) }
    factory { StartResourceDownloadUseCase(get()) }
    factory { CancelResourceDownloadUseCase(get()) }
    factory { DeleteResourceDownloadUseCase(get()) }

    factory { com.example.mushaf.domain.usecase.GetRecitersUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.GetAyahTimingsUseCase(get()) }
    
    factory<com.example.mushaf.presentation.audio.AudioPlayer> { 
        com.example.mushaf.presentation.audio.AudioPlaybackManager(androidContext()) 
    }

    viewModel { MushafViewModel(get(), get(), get(), get(), get(), get(), get()) }

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

    viewModel { MushafSettingsViewModel(get(), get()) }

    viewModel { (kind: ResourceKind) ->
        DownloadsViewModel(kind, get(), get(), get(), get())
    }
}
