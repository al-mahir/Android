package com.iti.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LogoutUseCase
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.usecase.subscription.GetSubscriptionMinutesUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.user.DeleteAccountUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.model.ProfileWebTarget
import com.iti.presentation.profile.state.ProfileDialog
import com.iti.presentation.profile.state.ProfileEffect
import com.iti.presentation.profile.state.ProfileIntent
import com.iti.presentation.profile.state.ProfileUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus
import com.iti.domain.core.fold
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.repository.CircleRepository

class ProfileViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getSubscription: GetSubscriptionUseCase,
    /** Absent in apps without a payment graph (sheikh app) — then no subscription UI at all. */
    private val getSubscriptionMinutes: GetSubscriptionMinutesUseCase?,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val circleRepository: CircleRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel(),
    StateHolder<ProfileUiState> by DefaultStateHolder(ProfileUiState()),
    EffectPublisher<ProfileEffect> by DefaultEffectPublisher() {

    private var accountJob: Job? = null
    private var minutesJob: Job? = null

    init {
        updateState { copy(isSubscriptionSupported = getSubscriptionMinutes != null) }
        observeAccount()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Retry -> observeAccount()
            ProfileIntent.Refresh -> loadSubscriptionMinutes()
            ProfileIntent.PremiumClicked -> sendEffect(ProfileEffect.OpenPremium)
            ProfileIntent.MySubscriptionClicked -> sendEffect(ProfileEffect.OpenMySubscription)
            ProfileIntent.LogoutClicked -> openDialog(ProfileDialog.LOGOUT)
            ProfileIntent.DeleteAccountClicked -> openDialog(ProfileDialog.DELETE_ACCOUNT)
            ProfileIntent.DialogConfirmed -> confirmDialog()
            ProfileIntent.DialogDismissed -> dismissDialog()
            is ProfileIntent.MenuOptionClicked -> openMenuOption(intent.menuType)
            is ProfileIntent.SocialChannelClicked ->
                sendEffect(ProfileEffect.OpenSocialChannel(intent.channel))
            ProfileIntent.SeeAllCirclesClicked -> sendEffect(ProfileEffect.OpenCircleList)
            is ProfileIntent.CircleClicked -> sendEffect(ProfileEffect.OpenCircle(intent.circleId))
        }
    }

    private fun observeAccount() {
        accountJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        loadSubscriptionMinutes()

        viewModelScope.launch {
            circleRepository.getMyCircles().fold(
                onSuccess = { circles -> updateState { copy(myCircles = circles) } },
                onFailure = { },
            )
        }

        viewModelScope.launch {
            circleRepository.getPublicCircles().fold(
                onSuccess = { circles ->
                    updateState {
                        copy(
                            availableCircles = circles.filter { circle ->
                                circle.status == CircleStatus.SCHEDULED ||
                                    circle.status == CircleStatus.ONGOING
                            },
                        )
                    }
                },
                onFailure = { },
            )
        }

        accountJob = combine(
            getCurrentUser(),
            getSubscription(),
            connectivityObserver.status
        ) { userResult, subscriptionResult, connectivity ->
            val user = userResult.getOrNull()
            val subscription = subscriptionResult.getOrNull()
            if (user == null || subscription == null) {
                null
            } else {
                val isOffline = connectivity == ConnectivityStatus.Unavailable
                ProfileAccountSnapshot(user, subscription, isOffline)
            }
        }
            .catch {
                updateState {
                    copy(isLoading = false, errorMessageRes = R.string.profile_error_generic)
                }
            }
            .onEach { snapshot ->
                if (snapshot == null) {
                    updateState { copy(isLoading = false, errorMessageRes = R.string.profile_error_generic) }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessageRes = null,
                            user = snapshot.user,
                            subscription = snapshot.subscription,
                            isOffline = snapshot.isOffline,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * The entitlement is fetched separately from the account stream: it must not be able to fail
     * the whole profile. A failed lookup leaves [ProfileUiState.subscriptionMinutes] null with
     * loading finished, which the state treats as "not subscribed" and shows the packages CTA —
     * the same as a student who genuinely has none.
     */
    private fun loadSubscriptionMinutes() {
        val useCase = getSubscriptionMinutes ?: return
        minutesJob?.cancel()
        updateState { copy(isLoadingMinutes = true) }

        minutesJob = viewModelScope.launch {
            val minutes = useCase().getOrNull()
            updateState {
                copy(
                    isLoadingMinutes = false,
                    subscriptionMinutes = minutes,
                    nowEpochMillis = now(),
                )
            }
        }
    }

    private fun openDialog(dialog: ProfileDialog) {
        if (currentState.isProcessingDialogAction) return
        updateState { copy(dialog = dialog) }
    }

    private fun dismissDialog() {
        // A confirmed action is already running; letting it close would strand the request.
        if (currentState.isProcessingDialogAction) return
        updateState { copy(dialog = null) }
    }

    private fun confirmDialog() {
        val dialog = currentState.dialog ?: return
        if (currentState.isProcessingDialogAction) return
        updateState { copy(isProcessingDialogAction = true) }

        viewModelScope.launch {
            // Signing out always clears the local session, so it only fails if it never ran.
            val succeeded = when (dialog) {
                ProfileDialog.LOGOUT -> logout() is Result.Success
                ProfileDialog.DELETE_ACCOUNT -> deleteAccount() is Result.Success
            }

            updateState { copy(isProcessingDialogAction = false, dialog = null) }

            if (succeeded) {
                // Both paths end the session, so both land back on auth.
                sendEffect(ProfileEffect.NavigateToAuth)
            } else {
                val messageRes = when (dialog) {
                    ProfileDialog.LOGOUT -> R.string.profile_logout_failed
                    ProfileDialog.DELETE_ACCOUNT -> R.string.profile_delete_account_failed
                }
                sendEffect(ProfileEffect.ShowMessage(messageRes))
            }
        }
    }

    private fun openMenuOption(menuType: ProfileMenuType) {
        val effect = when (menuType) {
            ProfileMenuType.SESSIONS -> ProfileEffect.OpenSessions

            ProfileMenuType.ATTRIBUTIONS -> ProfileEffect.OpenAttributions

            ProfileMenuType.SETTINGS -> ProfileEffect.OpenSettings

            ProfileMenuType.ABOUT ->
                ProfileEffect.OpenLegalDocument(LegalDocumentType.ABOUT)

            ProfileMenuType.TERMS_OF_SERVICE ->
                ProfileEffect.OpenLegalDocument(LegalDocumentType.TERMS_OF_SERVICE)

            ProfileMenuType.PRIVACY_POLICY ->
                ProfileEffect.OpenLegalDocument(LegalDocumentType.PRIVACY_POLICY)

            ProfileMenuType.FEATURE_REQUEST ->
                ProfileEffect.OpenWebPage(ProfileWebTarget.FEATURE_REQUEST)

            ProfileMenuType.HELP_CENTER ->
                ProfileEffect.OpenWebPage(ProfileWebTarget.HELP_CENTER)

            ProfileMenuType.SHARE_APP -> ProfileEffect.ShareApp
            ProfileMenuType.RATE_APP -> ProfileEffect.RequestAppReview
        }
        sendEffect(effect)
    }
}
