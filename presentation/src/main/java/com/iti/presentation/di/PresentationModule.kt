package com.iti.presentation.di

import com.iti.domain.usecase.DeleteRecitationSessionUseCase
import com.iti.domain.usecase.GetRecitationSessionUseCase
import com.iti.domain.usecase.ObserveRecitationSessionsUseCase
import com.iti.presentation.sessions.SessionHistoryViewModel
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.usecase.circle.CancelJoinCircleUseCase
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.legal.GetLegalDocumentUseCase
import com.iti.domain.usecase.reading.GetAyahOfTheDayUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhByIdUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.subscription.RestorePurchasesUseCase
import com.iti.domain.usecase.user.DeleteAccountUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.domain.usecase.settings.DeleteAllRecordingsUseCase
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import com.iti.domain.usecase.settings.SetAppLanguageUseCase
import com.iti.domain.usecase.settings.SetDataSaverEnabledUseCase
import com.iti.domain.usecase.settings.SetErrorSoundsEnabledUseCase
import com.iti.domain.usecase.settings.SetRemindersEnabledUseCase
import com.iti.domain.usecase.settings.SetThemeModeUseCase
import com.iti.presentation.circle.CircleListViewModel
import com.iti.presentation.circle.InSessionViewModel
import com.iti.presentation.circle.JoiningCircleViewModel
import com.iti.presentation.core.platform.AppReviewLauncher
import com.iti.presentation.core.platform.StoreListingAppReviewLauncher
import com.iti.presentation.home.HomeViewModel
import com.iti.presentation.profile.ProfileViewModel
import com.iti.presentation.settings.SettingsViewModel
import com.iti.presentation.sheikh.SheikhDetailsViewModel
import com.iti.presentation.sheikh.SheikhListViewModel
import com.iti.presentation.staticcontent.StaticContentViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val presentationModule = module {
    // ── Use cases — user / reading / subscription / legal (AlmahirRepository) ──
    factory { GetCurrentUserUseCase(get()) }
    factory { GetReadingProgressUseCase(get()) }
    factory { GetAyahOfTheDayUseCase() }
    factory { GetSubscriptionUseCase(get()) }
    factory { RestorePurchasesUseCase(get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { GetLegalDocumentUseCase(get()) }

    // ── Use cases — Sheikh (SheikhRepository → real API) ─────────────────────
    factory { GetSheikhsUseCase(get()) }
    factory { GetSheikhByIdUseCase(get()) }

    // ── Use cases — Circle (CircleRepository → fake) ──────────────────────────
    factory { GetStudyCirclesUseCase(get()) }
    factory { JoinStudyCircleUseCase(get()) }
    factory { CancelJoinCircleUseCase(get()) }

    // ── Settings use cases ────────────────────────────────────────────────────
    factory { ObserveAppPreferencesUseCase(get()) }
    factory { SetThemeModeUseCase(get()) }
    factory { SetAppLanguageUseCase(get()) }
    factory { SetRemindersEnabledUseCase(get()) }
    factory { SetErrorSoundsEnabledUseCase(get()) }
    factory { SetDataSaverEnabledUseCase(get()) }
    factory { DeleteAllRecordingsUseCase(get()) }

    // ── Recitation session use cases ──────────────────────────────────────────
    factory { ObserveRecitationSessionsUseCase(get()) }
    factory { GetRecitationSessionUseCase(get()) }
    factory { DeleteRecitationSessionUseCase(get()) }

    // ── Platform ──────────────────────────────────────────────────────────────
    single<AppReviewLauncher> { StoreListingAppReviewLauncher(androidContext().packageName) }

    single(named(APP_VERSION)) {
        val context = androidContext()
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { SessionHistoryViewModel(get(), get()) }
    viewModel { (documentType: LegalDocumentType) ->
        StaticContentViewModel(documentType, get())
    }
    viewModel {
        SettingsViewModel(
            appVersion = get(named(APP_VERSION)),
            observePreferences = get(),
            setThemeMode = get(),
            setLanguage = get(),
            setRemindersEnabled = get(),
            setErrorSoundsEnabled = get(),
            setDataSaverEnabled = get(),
            deleteAllRecordings = get(),
        )
    }
    viewModel { SheikhListViewModel(get()) }
    viewModel { (sheikhId: String) -> SheikhDetailsViewModel(sheikhId, get(), get(), get()) }
    viewModel { CircleListViewModel(get(), get()) }
    viewModel { (circleId: String) -> JoiningCircleViewModel(circleId, get(), get()) }
    viewModel { (circleId: String) -> InSessionViewModel(circleId, get()) }
}

const val APP_VERSION = "app_version"

