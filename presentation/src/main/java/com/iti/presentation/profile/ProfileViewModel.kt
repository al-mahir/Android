package com.iti.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.LogoutUseCase
import com.iti.domain.core.Result
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.subscription.RestorePurchasesUseCase
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


class ProfileViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getSubscription: GetSubscriptionUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel(),
    StateHolder<ProfileUiState> by DefaultStateHolder(ProfileUiState()),
    EffectPublisher<ProfileEffect> by DefaultEffectPublisher() {

    private var accountJob: Job? = null

    init {
        observeAccount()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Retry -> observeAccount()
            ProfileIntent.PremiumClicked -> sendEffect(ProfileEffect.OpenPremium)
            ProfileIntent.RestorePurchasesClicked -> restore()
            ProfileIntent.LogoutClicked -> openDialog(ProfileDialog.LOGOUT)
            ProfileIntent.DeleteAccountClicked -> openDialog(ProfileDialog.DELETE_ACCOUNT)
            ProfileIntent.DialogConfirmed -> confirmDialog()
            ProfileIntent.DialogDismissed -> dismissDialog()
            is ProfileIntent.MenuOptionClicked -> openMenuOption(intent.menuType)
            is ProfileIntent.SocialChannelClicked ->
                sendEffect(ProfileEffect.OpenSocialChannel(intent.channel))
        }
    }

    private fun observeAccount() {
        accountJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        accountJob = combine(
            getCurrentUser(),
            getSubscription(),
        ) { user, subscription ->
            ProfileAccountSnapshot(user, subscription)
        }
            .catch {
                updateState {
                    copy(isLoading = false, errorMessageRes = R.string.profile_error_generic)
                }
            }
            .onEach { snapshot ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessageRes = null,
                        user = snapshot.user,
                        subscription = snapshot.subscription,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun restore() {
        if (currentState.isRestoringPurchases) return
        updateState { copy(isRestoringPurchases = true) }

        viewModelScope.launch {
            val messageRes = runCatching { restorePurchases() }
                .fold(
                    onSuccess = { restored ->
                        if (restored) R.string.profile_restore_succeeded
                        else R.string.profile_restore_nothing_found
                    },
                    onFailure = { R.string.profile_restore_failed },
                )

            updateState { copy(isRestoringPurchases = false) }
            sendEffect(ProfileEffect.ShowMessage(messageRes))
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
                ProfileDialog.LOGOUT -> runCatching { logout() }.getOrNull() is Result.Success
                ProfileDialog.DELETE_ACCOUNT -> runCatching { deleteAccount() }.isSuccess
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
