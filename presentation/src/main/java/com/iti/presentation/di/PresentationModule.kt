package com.iti.presentation.di

import com.iti.domain.model.LegalDocumentType
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.legal.GetLegalDocumentUseCase
import com.iti.domain.usecase.reading.GetReadingProgressUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.subscription.RestorePurchasesUseCase
import com.iti.domain.usecase.user.DeleteAccountUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.domain.usecase.user.LogoutUseCase
import com.iti.presentation.core.platform.AppReviewLauncher
import com.iti.presentation.core.platform.StoreListingAppReviewLauncher
import com.iti.presentation.home.HomeViewModel
import com.iti.presentation.profile.ProfileViewModel
import com.iti.presentation.staticcontent.StaticContentViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    factory { GetCurrentUserUseCase(get()) }
    factory { GetReadingProgressUseCase(get()) }
    factory { GetSheikhsUseCase(get()) }
    factory { GetStudyCirclesUseCase(get()) }
    factory { JoinStudyCircleUseCase(get()) }
    factory { GetSubscriptionUseCase(get()) }
    factory { RestorePurchasesUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { GetLegalDocumentUseCase(get()) }

    // Swap this binding for a Play `ReviewManager`-backed launcher once the review dependency
    // is in the version catalog — nothing above the interface changes.
    single<AppReviewLauncher> { StoreListingAppReviewLauncher(androidContext().packageName) }

    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { (documentType: LegalDocumentType) ->
        StaticContentViewModel(documentType, get())
    }
}
