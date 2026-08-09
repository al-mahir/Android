package com.iti.presentation.auth.resetpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.text.asString
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onIntent: (ResetPasswordIntent) -> Unit,
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
            text = stringResource(id = R.string.auth_reset_password_title),
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )

        Text(
            text = stringResource(id = R.string.auth_reset_password_subtitle),
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        TextField(
            text = state.newPassword,
            onTextChange = { onIntent(ResetPasswordIntent.NewPasswordChanged(it)) },
            title = stringResource(id = R.string.auth_new_password),
            hint = stringResource(id = R.string.auth_enter_new_password),
            visualTransformation = if (state.isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = rememberVectorPainter(
                image = if (state.isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            ),
            onClickTrailingIcon = { onIntent(ResetPasswordIntent.ToggleNewPasswordVisibility) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.newPasswordError != null,
            errorMessage = state.newPasswordError?.asString(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Theme.spacing.medium))

        TextField(
            text = state.confirmPassword,
            onTextChange = { onIntent(ResetPasswordIntent.ConfirmPasswordChanged(it)) },
            title = stringResource(id = R.string.auth_confirm_password),
            hint = stringResource(id = R.string.auth_enter_confirm_password),
            visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = rememberVectorPainter(
                image = if (state.isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            ),
            onClickTrailingIcon = { onIntent(ResetPasswordIntent.ToggleConfirmPasswordVisibility) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.confirmPasswordError != null,
            errorMessage = state.confirmPasswordError?.asString(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Theme.spacing.large))

        PrimaryButton(
            caption = stringResource(id = R.string.auth_reset_password_button),
            onClick = { onIntent(ResetPasswordIntent.Submit) },
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
