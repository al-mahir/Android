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
import androidx.compose.ui.res.stringResource
import com.iti.presentation.R

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.designsystem.text.asString

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
            text = stringResource(id = R.string.auth_forgot_password_title),
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )
        
        Text(
            text = stringResource(id = R.string.auth_enter_email_reset),
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        TextField(
            text = state.email,
            onTextChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
            title = stringResource(id = R.string.auth_email_address),
            hint = stringResource(id = R.string.auth_enter_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null,
            errorMessage = state.emailError?.asString(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        
        PrimaryButton(
            caption = stringResource(id = R.string.auth_send_reset_link),
            onClick = { onIntent(ForgotPasswordIntent.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        
        Text(
            text = stringResource(id = R.string.auth_back_to_sign_in),
            style = Theme.typography.body.medium,
            color = Theme.colors.primary,
            modifier = Modifier.clickable { onNavigateBack() }
        )
    }
}
