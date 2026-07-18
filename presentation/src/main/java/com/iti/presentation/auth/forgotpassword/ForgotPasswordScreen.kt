package com.iti.presentation.auth.forgotpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        Text(
            text = "Forgot Password",
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )
        
        Text(
            text = "Enter your email to receive a reset link/OTP",
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        TextField(
            text = state.email,
            onTextChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
            title = "Email Address",
            hint = "Enter your email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null,
            errorMessage = state.emailError,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        
        PrimaryButton(
            caption = "Send Reset Link",
            onClick = { onIntent(ForgotPasswordIntent.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        
        Text(
            text = "Back to Sign In",
            style = Theme.typography.body.medium,
            color = Theme.colors.primary,
            modifier = Modifier.clickable { onNavigateBack() }
        )
    }
}
