package com.iti.presentation.profile

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.domain.model.LegalDocumentType
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.core.platform.AppReviewLauncher
import com.iti.presentation.core.platform.ExternalActions
import com.iti.presentation.core.platform.findActivity
import com.iti.presentation.profile.components.ProfileConfirmationDialogs
import com.iti.presentation.profile.model.ProfileWebTarget
import com.iti.presentation.profile.model.SocialChannel
import com.iti.presentation.profile.state.ProfileEffect
import com.iti.presentation.profile.state.ProfileIntent
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun ProfileScreen(
    onOpenPremium: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    onSignedOut: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
    onOpenAttributions: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
    reviewLauncher: AppReviewLauncher = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current


    val socialUrls = socialChannelUrls()
    val webUrls = webTargetUrls()
    val shareMessage = stringResource(
        R.string.profile_share_message,
        stringResource(R.string.profile_share_link),
    )
    val shareChooserTitle = stringResource(R.string.profile_share_chooser_title)

    fun showMessage(messageRes: Int) =
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()

    fun openUrl(url: String) {
        if (!ExternalActions.openUrl(context, url)) {
            showMessage(R.string.profile_no_app_to_handle)
        }
    }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            ProfileEffect.OpenPremium -> onOpenPremium()

            ProfileEffect.OpenSettings -> onOpenSettings()

            ProfileEffect.OpenSessions -> onOpenSessions()

            ProfileEffect.OpenAttributions -> onOpenAttributions()

            is ProfileEffect.OpenLegalDocument -> onOpenLegalDocument(effect.type)

            is ProfileEffect.OpenWebPage -> openUrl(webUrls.getValue(effect.target))

            is ProfileEffect.OpenSocialChannel -> openUrl(socialUrls.getValue(effect.channel))

            ProfileEffect.ShareApp -> {
                val shared = ExternalActions.shareText(
                    context = context,
                    text = shareMessage,
                    chooserTitle = shareChooserTitle,
                )
                if (!shared) showMessage(R.string.profile_no_app_to_handle)
            }

            ProfileEffect.RequestAppReview ->
                context.findActivity()?.let(reviewLauncher::requestReview)

            ProfileEffect.NavigateToAuth -> onSignedOut()

            is ProfileEffect.ShowMessage -> showMessage(effect.messageRes)
        }
    }

    ProfileContent(
        state = state,
        onPremiumClick = { viewModel.onIntent(ProfileIntent.PremiumClicked) },
        onRestorePurchasesClick = { viewModel.onIntent(ProfileIntent.RestorePurchasesClicked) },
        onLogoutClick = { viewModel.onIntent(ProfileIntent.LogoutClicked) },
        onDeleteAccountClick = { viewModel.onIntent(ProfileIntent.DeleteAccountClicked) },
        onMenuOptionClick = { menuType -> viewModel.onIntent(ProfileIntent.MenuOptionClicked(menuType)) },
        onSocialChannelClick = { channel -> viewModel.onIntent(ProfileIntent.SocialChannelClicked(channel)) },
        onRetryClick = { viewModel.onIntent(ProfileIntent.Retry) },
        modifier = modifier,
    )

    ProfileConfirmationDialogs(
        dialog = state.dialog,
        isProcessing = state.isProcessingDialogAction,
        onConfirm = { viewModel.onIntent(ProfileIntent.DialogConfirmed) },
        onDismiss = { viewModel.onIntent(ProfileIntent.DialogDismissed) },
    )
}


@Composable
private fun socialChannelUrls(): Map<SocialChannel, String> = mapOf(
    SocialChannel.GITHUB to stringResource(R.string.profile_social_url_github),
    SocialChannel.DISCORD to stringResource(R.string.profile_social_url_discord),
    SocialChannel.X to stringResource(R.string.profile_social_url_x),
    SocialChannel.FACEBOOK to stringResource(R.string.profile_social_url_facebook),
    SocialChannel.YOUTUBE to stringResource(R.string.profile_social_url_youtube),
    SocialChannel.INSTAGRAM to stringResource(R.string.profile_social_url_instagram),
)

@Composable
private fun webTargetUrls(): Map<ProfileWebTarget, String> = mapOf(
    ProfileWebTarget.FEATURE_REQUEST to stringResource(R.string.profile_url_feature_request),
    ProfileWebTarget.HELP_CENTER to stringResource(R.string.profile_url_help_center),
)
