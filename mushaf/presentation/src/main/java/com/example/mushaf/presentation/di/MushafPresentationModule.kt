package com.example.mushaf.presentation.di

import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.usecase.CancelResourceDownloadUseCase
import com.example.mushaf.domain.usecase.CaptureRecitationAudioUseCase
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.DeleteResourceDownloadUseCase
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.GetRecitationSchemaUseCase
import com.example.mushaf.domain.usecase.ObserveDownloadableResourcesUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.StartResourceDownloadUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
import com.example.mushaf.presentation.MushafViewModel
import com.example.mushaf.presentation.download.DownloadsViewModel
import com.example.mushaf.presentation.settings.MushafSettingsViewModel
import com.example.mushaf.presentation.settings.recite.ReciteSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val mushafPresentationModule = module {
    factory { GetPageUseCase(get()) }
    factory { ObserveReaderPreferencesUseCase(get()) }
    factory { SetTajweedEnabledUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.SetFirstMushafLaunchCompletedUseCase(get()) }
    factory { SaveLastPageUseCase(get()) }

    factory { com.example.mushaf.domain.usecase.GetLastReadUseCase() }
    factory { com.example.mushaf.domain.usecase.search.SearchSurahUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchJuzUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchHizbUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchPageUseCase(get()) }
    factory { SearchAyahUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchAyahByMeaningUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.GetTargetPageUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.GetTafsirForAyahUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.search.SearchTafsirUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.ObserveAvailableTafsirBooksUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.ManageTafsirDownloadUseCase(get()) }

    factory { ObserveDownloadableResourcesUseCase(get()) }
    factory { StartResourceDownloadUseCase(get()) }
    factory { CancelResourceDownloadUseCase(get()) }
    factory { DeleteResourceDownloadUseCase(get()) }

    factory { com.example.mushaf.domain.usecase.GetRecitersUseCase(get()) }
    factory { com.example.mushaf.domain.usecase.GetAyahTimingsUseCase(get()) }
    factory { CaptureRecitationAudioUseCase(get()) }
    factory { StartLiveRecitationUseCase(get()) }
    
    factory<com.example.mushaf.presentation.audio.AudioPlayer> { 
        com.example.mushaf.presentation.audio.AudioPlaybackManager(androidContext()) 
    }

    factory { com.iti.domain.usecase.SaveRecitationSessionUseCase(get()) }

    factory { ObserveRecitationSettingsUseCase(get()) }
    factory { UpdateRecitationSettingsUseCase(get()) }
    factory { GetRecitationSchemaUseCase(get()) }

    factory { com.example.mushaf.domain.usecase.GetAvailableTafsirBooksUseCase(get()) }

    viewModel {
        MushafViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

    viewModel { 
        com.example.mushaf.presentation.search.MushafSearchViewModel(
            getLastReadUseCase = get(),
            searchSurahUseCase = get(),
            searchJuzUseCase = get(),
            searchAyahUseCase = get(),
            searchAyahByMeaningUseCase = get(),
            searchTafsirUseCase = get(),
            getTargetPageUseCase = get(),
            saveLastPageUseCase = get(),
            connectivityObserver = get()
        )
    }

    viewModel { MushafSettingsViewModel(get(), get()) }

    viewModel { ReciteSettingsViewModel(get(), get(), get()) }

    viewModel { (kind: ResourceKind) ->
        DownloadsViewModel(kind, get(), get(), get(), get())
    }
}
