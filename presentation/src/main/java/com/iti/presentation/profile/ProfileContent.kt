package com.iti.presentation.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.bottomnav.bottomNavBarHeight
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.profile.components.AccountActionsBlock
import com.iti.presentation.profile.components.ProfileHeader
import com.iti.presentation.profile.components.ProfileMenuRow
import com.iti.presentation.profile.components.ProfileSkeleton
import com.iti.presentation.profile.components.SocialMediaChannelsRow
import com.iti.presentation.profile.components.SubscriptionStatusRow
import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.model.SocialChannel
import com.iti.presentation.profile.state.ProfileUiState


@Composable
fun ProfileContent(
    state: ProfileUiState,
    onPremiumClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onMenuOptionClick: (ProfileMenuType) -> Unit,
    onSocialChannelClick: (SocialChannel) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier
        .fillMaxSize()
        .background(Theme.colors.backGround)

    val gutter = Modifier.padding(horizontal = Theme.spacing.medium)

    when {
        state.hasError -> NetworkErrorScreen(
            modifier = rootModifier,
            description = stringResource(state.errorMessageRes ?: R.string.profile_error_generic),
            onRetry = onRetryClick,
        )

        state.isLoading && state.user == null -> ProfileSkeleton(modifier = rootModifier)

        state.user != null -> Column(modifier = rootModifier) {
            val user = state.user

            // Account block is pinned: it is the screen's identity, and the menu below is the
            // only part long enough to need scrolling.
            Column(
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                modifier = Modifier.padding(
                    top = Theme.spacing.large,
                    bottom = Theme.spacing.large,
                ),
            ) {
                ProfileHeader(
                    displayName = user.displayName,
                    email = user.email,
                    initials = user.initials,
                    avatarUrl = user.avatarUrl,
                    modifier = gutter,
                )

                SubscriptionStatusRow(
                    isPremium = state.isPremium,
                    joinedAtEpochMillis = user.joinedAtEpochMillis,
                    modifier = gutter,
                )

                AccountActionsBlock(
                    isPremium = state.isPremium,
                    isRestoringPurchases = state.isRestoringPurchases,
                    onPremiumClick = onPremiumClick,
                    onRestorePurchasesClick = onRestorePurchasesClick,
                    onLogoutClick = onLogoutClick,
                    modifier = gutter,
                )

                SocialMediaChannelsRow(
                    onChannelClick = onSocialChannelClick,
                    modifier = gutter,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    bottom = Theme.spacing.large + bottomNavBarHeight(),
                ),
            ) {
                items(items = MenuEntries, key = { entry -> entry.type.name }) { entry ->
                    ProfileMenuRow(
                        title = stringResource(entry.titleRes),
                        iconRes = entry.iconRes,
                        showChevron = entry.showChevron,
                        onClick = { onMenuOptionClick(entry.type) },
                        modifier = gutter,
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Theme.colors.outline,
                        modifier = gutter,
                    )
                }

                item(key = "delete-account") {
                    ProfileMenuRow(
                        title = stringResource(R.string.profile_delete_account),
                        contentColor = Theme.colors.error,
                        onClick = onDeleteAccountClick,
                        modifier = gutter.padding(top = Theme.spacing.small),
                    )
                }
            }
        }
    }
}


private data class MenuEntry(
    val type: ProfileMenuType,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int? = null,
    val showChevron: Boolean = false,
)

private val MenuEntries = listOf(
    MenuEntry(
        type = ProfileMenuType.SESSIONS,
        titleRes = R.string.profile_menu_sessions,
        iconRes = DesignSystemR.drawable.ic_info,
        showChevron = true,
    ),
    MenuEntry(
        type = ProfileMenuType.SETTINGS,
        titleRes = R.string.settings_title,
        iconRes = DesignSystemR.drawable.ic_settings,
        showChevron = true,
    ),
    MenuEntry(
        type = ProfileMenuType.ABOUT,
        titleRes = R.string.profile_menu_about,
        iconRes = DesignSystemR.drawable.ic_info,
    ),
    MenuEntry(
        type = ProfileMenuType.FEATURE_REQUEST,
        titleRes = R.string.profile_menu_feature_request,
        iconRes = DesignSystemR.drawable.ic_chat,
    ),
    MenuEntry(
        type = ProfileMenuType.HELP_CENTER,
        titleRes = R.string.profile_menu_help_center,
        iconRes = DesignSystemR.drawable.ic_help,
    ),
    MenuEntry(
        type = ProfileMenuType.SHARE_APP,
        titleRes = R.string.profile_menu_share_app,
        iconRes = DesignSystemR.drawable.ic_share,
    ),
    MenuEntry(
        type = ProfileMenuType.RATE_APP,
        titleRes = R.string.profile_menu_rate_app,
        iconRes = DesignSystemR.drawable.ic_star,
    ),
    MenuEntry(
        type = ProfileMenuType.ATTRIBUTIONS,
        titleRes = R.string.profile_menu_attributions,
        iconRes = DesignSystemR.drawable.ic_info,
    ),
    MenuEntry(
        type = ProfileMenuType.TERMS_OF_SERVICE,
        titleRes = R.string.profile_menu_terms_of_service,
        showChevron = true,
    ),
    MenuEntry(
        type = ProfileMenuType.PRIVACY_POLICY,
        titleRes = R.string.profile_menu_privacy_policy,
        showChevron = true,
    ),
)
