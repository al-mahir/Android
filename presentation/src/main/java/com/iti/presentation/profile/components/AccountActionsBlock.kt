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
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.button.ButtonIconPosition
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

/**
 * Sign-out only. The subscribe / renew / manage entry point deliberately lives solely on the
 * subscription card above this block — having it here too gave the profile two green buttons
 * leading to the same screen.
 */
@Composable
internal fun AccountActionsBlock(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
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
