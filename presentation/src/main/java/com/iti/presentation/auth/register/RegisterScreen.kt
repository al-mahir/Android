package com.iti.presentation.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.iti.presentation.R
import com.example.designsystem.text.asString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun RegisterScreen(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .imePadding()
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = stringResource(id = R.string.auth_create_account),
                style = Theme.typography.h4,
                color = Theme.colors.primaryFont,
                modifier = Modifier.padding(top = Theme.spacing.large, bottom = Theme.spacing.small)
            )
            
            Text(
                text = stringResource(id = R.string.auth_sign_up_to_start),
                style = Theme.typography.body.large,
                color = Theme.colors.secondaryFont,
                modifier = Modifier.padding(bottom = Theme.spacing.large)
            )
        }

        item {
            TextField(
                text = state.username,
                onTextChange = { onIntent(RegisterIntent.UsernameChanged(it)) },
                title = stringResource(id = R.string.auth_username),
                hint = stringResource(id = R.string.auth_enter_username),
                isError = state.usernameError != null,
                errorMessage = state.usernameError?.asString(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                TextField(
                    text = state.firstName,
                    onTextChange = { onIntent(RegisterIntent.FirstNameChanged(it)) },
                    title = stringResource(id = R.string.auth_first_name),
                    hint = stringResource(id = R.string.auth_first_name),
                    isError = state.firstNameError != null,
                    errorMessage = state.firstNameError?.asString(),
                    modifier = Modifier.weight(1f)
                )
                TextField(
                    text = state.lastName,
                    onTextChange = { onIntent(RegisterIntent.LastNameChanged(it)) },
                    title = stringResource(id = R.string.auth_last_name),
                    hint = stringResource(id = R.string.auth_last_name),
                    isError = state.lastNameError != null,
                    errorMessage = state.lastNameError?.asString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TextField(
                text = state.email,
                onTextChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                title = stringResource(id = R.string.auth_email_address),
                hint = stringResource(id = R.string.auth_enter_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.emailError != null,
                errorMessage = state.emailError?.asString(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            TextField(
                text = state.phoneNumber,
                onTextChange = { onIntent(RegisterIntent.PhoneNumberChanged(it)) },
                title = stringResource(id = R.string.auth_phone_number),
                hint = stringResource(id = R.string.auth_enter_phone_number),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.phoneNumberError != null,
                errorMessage = state.phoneNumberError?.asString(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            TextField(
                text = state.password,
                onTextChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                title = stringResource(id = R.string.auth_password),
                hint = stringResource(id = R.string.auth_enter_password),
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = rememberVectorPainter(image = if (state.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff),
                onClickTrailingIcon = { onIntent(RegisterIntent.TogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.passwordError != null,
                errorMessage = state.passwordError?.asString(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.large)
            )
        }

        item {
            PrimaryButton(
                caption = stringResource(id = R.string.auth_sign_up),
                onClick = { onIntent(RegisterIntent.SubmitRegistration) },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            SecondaryButton(
                caption = stringResource(id = R.string.auth_register_with_google),
                onClick = { onIntent(RegisterIntent.GoogleSignInClicked) },
                iconPainter = painterResource(id = com.example.designsystem.R.drawable.ic_google),
                tintIcon = false,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.large)
            )
        }

        item {
            Row(
                modifier = Modifier.padding(bottom = Theme.spacing.large),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.auth_already_have_account),
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont
                )
                Text(
                    text = stringResource(id = R.string.auth_sign_in),
                    style = Theme.typography.body.medium,
                    color = Theme.colors.primary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
