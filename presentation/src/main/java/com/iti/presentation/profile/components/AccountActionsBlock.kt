package com.iti.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.button.ButtonIconPosition
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

@Composable
internal fun AccountActionsBlock(
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
    onMySubscriptionClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        PrimaryButton(
            caption = stringResource(
                if (isPremium) R.string.profile_menu_my_subscription else R.string.profile_buy_premium
            ),
            onClick = if (isPremium) onMySubscriptionClick else onPremiumClick,
            shape = Theme.shapes.large,
            captionStyle = Theme.typography.body.large.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        SecondaryButton(
            caption = stringResource(R.string.profile_logout),
            onClick = onLogoutClick,
            iconPainter = painterResource(DesignSystemR.drawable.ic_logout),
            iconPosition = ButtonIconPosition.End,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
