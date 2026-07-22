package com.iti.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.iti.presentation.R
import com.iti.presentation.core.ui.asString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.rememberVectorPainter

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
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
            text = stringResource(id = R.string.auth_welcome_back),
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )
        
        Text(
            text = stringResource(id = R.string.auth_sign_in_to_continue),
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        TextField(
            text = state.email,
            onTextChange = { onIntent(LoginIntent.EmailChanged(it)) },
            title = stringResource(id = R.string.auth_email_address),
            hint = stringResource(id = R.string.auth_enter_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null,
            errorMessage = state.emailError?.asString(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))

        TextField(
            text = state.password,
            onTextChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            title = stringResource(id = R.string.auth_password),
            hint = stringResource(id = R.string.auth_enter_password),
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = rememberVectorPainter(image = if (state.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff),
            onClickTrailingIcon = { onIntent(LoginIntent.TogglePasswordVisibility) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.passwordError != null,
            errorMessage = state.passwordError?.asString(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.small))
        
        Text(
            text = stringResource(id = R.string.auth_forgot_password),
            style = Theme.typography.body.medium,
            color = Theme.colors.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToForgotPassword() }
                .padding(vertical = Theme.spacing.small)
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        
        PrimaryButton(
            caption = stringResource(id = R.string.auth_sign_in),
            onClick = { onIntent(LoginIntent.SubmitLogin) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        
        SecondaryButton(
            caption = stringResource(id = R.string.auth_continue_with_google),
            onClick = { onIntent(LoginIntent.GoogleSignInClicked) },
            iconPainter = painterResource(id = com.example.designsystem.R.drawable.ic_google),
            tintIcon = false,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        
        Row(
            modifier = Modifier.padding(vertical = Theme.spacing.medium),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.auth_dont_have_account),
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont
            )
            Text(
                text = stringResource(id = R.string.auth_sign_up),
                style = Theme.typography.body.medium,
                color = Theme.colors.primary,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}
