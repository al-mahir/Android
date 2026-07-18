package com.iti.presentation.auth.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.textfield.OtpField
import com.example.designsystem.theme.Theme

@Composable
fun OtpScreen(
    state: OtpState,
    onIntent: (OtpIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        Text(
            text = "Check Your Email",
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )
        
        Text(
            text = "We sent a 6-digit code to\n${state.email}",
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        OtpField(
            value = state.otpCode,
            onValueChange = { onIntent(OtpIntent.OtpChanged(it)) },
            length = 6,
            isError = state.isError,
            modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.large)
        )
        
        PrimaryButton(
            caption = "Verify Code",
            onClick = { onIntent(OtpIntent.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
        )

        if (state.canResend) {
            Text(
                text = "Resend Code",
                style = Theme.typography.body.medium,
                color = Theme.colors.primary,
                modifier = Modifier.clickable { onIntent(OtpIntent.ResendOtp) }
            )
        } else {
            val formattedTime = String.format("00:%02d", state.timerSeconds)
            Text(
                text = "Resend Code in $formattedTime",
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont
            )
        }
    }
}
