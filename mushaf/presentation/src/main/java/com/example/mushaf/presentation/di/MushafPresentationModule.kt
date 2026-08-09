package com.example.mushaf.presentation.di

import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.usecase.CancelDownloadRecitationUseCase
import com.example.mushaf.domain.usecase.CancelResourceDownloadUseCase
import com.example.mushaf.domain.usecase.CaptureRecitationAudioUseCase
import com.example.mushaf.domain.usecase.StartLiveRecitationUseCase
import com.example.mushaf.domain.usecase.DeleteResourceDownloadUseCase
import com.example.mushaf.domain.usecase.DownloadRecitationUseCase
import com.example.mushaf.domain.usecase.GetAvailableTafsirBooksUseCase
import com.example.mushaf.domain.usecase.GetAyahTextUseCase
import com.example.mushaf.domain.usecase.GetAyahTimingsUseCase
import com.example.mushaf.domain.usecase.GetDownloadProgressUseCase
import com.example.mushaf.domain.usecase.GetLastReadUseCase
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.GetRecitationSchemaUseCase
import com.example.mushaf.domain.usecase.GetRecitersUseCase
import com.example.mushaf.domain.usecase.GetTafsirForAyahUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.example.mushaf.domain.usecase.ManageTafsirDownloadUseCase
import com.example.mushaf.domain.usecase.ObserveAvailableTafsirBooksUseCase
import com.example.mushaf.domain.usecase.ObserveDownloadableResourcesUseCase
import com.example.mushaf.domain.usecase.ObserveReaderPreferencesUseCase
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.SaveLastPageUseCase
import com.example.mushaf.domain.usecase.SetFirstMushafLaunchCompletedUseCase
import com.example.mushaf.domain.usecase.SetTajweedEnabledUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahUseCase
import com.example.mushaf.domain.usecase.StartResourceDownloadUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.search.SearchAyahByMeaningUseCase
import com.example.mushaf.domain.usecase.search.SearchHizbUseCase
import com.example.mushaf.domain.usecase.search.SearchJuzUseCase
import com.example.mushaf.domain.usecase.search.SearchPageUseCase
import com.example.mushaf.domain.usecase.search.SearchSurahUseCase
import com.example.mushaf.domain.usecase.search.SearchTafsirUseCase
import com.example.mushaf.presentation.MushafViewModel
import com.example.mushaf.presentation.audio.AudioPlaybackManager
import com.example.mushaf.presentation.audio.AudioPlayer
import com.example.mushaf.presentation.download.DownloadsViewModel
import com.example.mushaf.presentation.download.SurahDownloadViewModel
import com.example.mushaf.presentation.search.MushafSearchViewModel
import com.example.mushaf.presentation.settings.MushafSettingsViewModel
import com.example.mushaf.presentation.settings.recite.ReciteSettingsViewModel
import com.iti.domain.usecase.SaveRecitationSessionUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val mushafPresentationModule = module {
    factory { GetPageUseCase(get()) }
    factory { ObserveReaderPreferencesUseCase(get()) }
    factory { SetTajweedEnabledUseCase(get()) }
    factory { SetFirstMushafLaunchCompletedUseCase(get()) }
    factory { SaveLastPageUseCase(get()) }

    factory { GetLastReadUseCase() }
    factory { SearchSurahUseCase(get()) }
    factory { SearchJuzUseCase(get()) }
    factory { SearchHizbUseCase(get()) }
    factory { SearchPageUseCase(get()) }
    factory { SearchAyahUseCase(get()) }
    factory { SearchAyahByMeaningUseCase(get()) }
    factory { GetTargetPageUseCase(get()) }
    factory { GetAyahTextUseCase(get()) }
    factory { GetTafsirForAyahUseCase(get()) }
    factory { SearchTafsirUseCase(get()) }
    factory { ObserveAvailableTafsirBooksUseCase(get()) }
    factory { ManageTafsirDownloadUseCase(get()) }

    factory { ObserveDownloadableResourcesUseCase(get()) }
    factory { StartResourceDownloadUseCase(get()) }
    factory { CancelResourceDownloadUseCase(get()) }
    factory { DeleteResourceDownloadUseCase(get()) }

    factory { GetRecitersUseCase(get()) }
    factory { GetAyahTimingsUseCase(get()) }
    factory { CaptureRecitationAudioUseCase(get()) }
    factory { StartLiveRecitationUseCase(get()) }
    factory { DownloadRecitationUseCase(get()) }
    factory { CancelDownloadRecitationUseCase(get()) }
    factory { GetDownloadProgressUseCase(get()) }
    
    factory<AudioPlayer> {
        AudioPlaybackManager(androidContext())
    }

    factory { SaveRecitationSessionUseCase(get()) }

    factory { ObserveRecitationSettingsUseCase(get()) }
    factory { UpdateRecitationSettingsUseCase(get()) }
    factory { GetRecitationSchemaUseCase(get()) }

    factory { GetAvailableTafsirBooksUseCase(get()) }

    viewModel {
        MushafViewModel(
            getPage = get(),
            observeReaderPreferences = get(),
            setTajweedEnabled = get(),
            setFirstMushafLaunchCompleted = get(),
            saveLastPage = get(),
            getReciters = get(),
            getAyahTimings = get(),
            getTafsirForAyah = get(),
            getTargetPage = get(),
            playbackManager = get(),
            startLiveRecitation = get(),
            saveRecitationSession = get(),
            observeRecitationSettings = get(),
            updateRecitationSettings = get(),
            downloadRecitation = get(),
            localWordCorpusRepository = get(),
            referencePhonemeRepository = get(),
            observeAvailableTafsirBooks = get(),
            manageTafsirDownload = get(),
            observeAppPreferences = get(),
            connectivityObserver = get(),
            toggleBookmarkUseCase = get(),
            observeBookmarks = get(),
        )
    }

    viewModel {
        MushafSearchViewModel(
            getLastReadUseCase = get(),
            searchSurahUseCase = get(),
            searchJuzUseCase = get(),
            searchAyahUseCase = get(),
            searchAyahByMeaningUseCase = get(),
            searchTafsirUseCase = get(),
            getTargetPageUseCase = get(),
            saveLastPageUseCase = get(),
            connectivityObserver = get(),
            toggleBookmarkUseCase = get(),
            observeBookmarks = get(),
        )
    }

    viewModel { MushafSettingsViewModel(get(), get()) }

    viewModel { ReciteSettingsViewModel(get(), get(), get()) }

    viewModel { (kind: ResourceKind) ->
        DownloadsViewModel(kind, get(), get(), get(), get())
    }

    viewModel { (reciterId: Int) ->
        SurahDownloadViewModel(reciterId, get(), get(), get(), get())
    }
}
