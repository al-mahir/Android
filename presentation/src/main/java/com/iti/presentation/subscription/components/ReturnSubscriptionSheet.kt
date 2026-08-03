package com.iti.presentation.subscription.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import com.iti.presentation.R


@Composable
internal fun ReturnSubscriptionSheet(
    message: String,
    onMessageChange: (String) -> Unit,
    isSending: Boolean,
    isSent: Boolean,
    onSendClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        if (isSent) {
            CancellationSentContent(onDoneClick = onDismiss)
        } else {
            CancellationFormContent(
                message = message,
                onMessageChange = onMessageChange,
                isSending = isSending,
                onSendClick = onSendClick,
            )
        }
    }
}

@Composable
private fun CancellationFormContent(
    message: String,
    onMessageChange: (String) -> Unit,
    isSending: Boolean,
    onSendClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        BasicText(
            text = stringResource(R.string.subscription_cancellation_sheet_title),
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Bold,
            ),
        )

        BasicText(
            text = stringResource(R.string.subscription_cancellation_sheet_description),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )

        TextField(
            text = message,
            onTextChange = onMessageChange,
            hint = stringResource(R.string.subscription_cancellation_message_hint),
            fieldHeight = 120.dp,
            fieldVerticalAlignment = Alignment.Top,
            minLines = 4,
            maxLines = 6,
            enabled = !isSending,
        )

        PrimaryButton(
            caption = stringResource(
                if (isSending) R.string.subscription_cancellation_sending
                else R.string.subscription_cancellation_send_button
            ),
            onClick = onSendClick,
            isLoading = isSending,
            isDisabled = message.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CancellationSentContent(onDoneClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Theme.colors.success.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_check),
                contentDescription = null,
                tint = Theme.colors.success,
                modifier = Modifier.size(Theme.size.iconLarge),
            )
        }

        BasicText(
            text = stringResource(R.string.subscription_cancellation_sent_title),
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )

        BasicText(
            text = stringResource(R.string.subscription_cancellation_sent_description),
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.Center,
            ),
        )

        PrimaryButton(
            caption = stringResource(R.string.subscription_cancellation_done_button),
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
