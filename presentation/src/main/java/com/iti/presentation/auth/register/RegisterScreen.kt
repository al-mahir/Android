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
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Create Account",
                style = Theme.typography.h4,
                color = Theme.colors.primaryFont,
                modifier = Modifier.padding(top = Theme.spacing.large, bottom = Theme.spacing.small)
            )
            
            Text(
                text = "Sign up to get started",
                style = Theme.typography.body.large,
                color = Theme.colors.secondaryFont,
                modifier = Modifier.padding(bottom = Theme.spacing.large)
            )
        }

        item {
            TextField(
                text = state.username,
                onTextChange = { onIntent(RegisterIntent.UsernameChanged(it)) },
                title = "Username",
                hint = "Enter your username",
                isError = state.usernameError != null,
                errorMessage = state.usernameError,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                TextField(
                    text = state.firstName,
                    onTextChange = { onIntent(RegisterIntent.FirstNameChanged(it)) },
                    title = "First Name",
                    hint = "First Name",
                    isError = state.firstNameError != null,
                    errorMessage = state.firstNameError,
                    modifier = Modifier.weight(1f)
                )
                TextField(
                    text = state.lastName,
                    onTextChange = { onIntent(RegisterIntent.LastNameChanged(it)) },
                    title = "Last Name",
                    hint = "Last Name",
                    isError = state.lastNameError != null,
                    errorMessage = state.lastNameError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TextField(
                text = state.email,
                onTextChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                title = "Email Address",
                hint = "Enter your email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.emailError != null,
                errorMessage = state.emailError,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            TextField(
                text = state.phoneNumber,
                onTextChange = { onIntent(RegisterIntent.PhoneNumberChanged(it)) },
                title = "Phone Number",
                hint = "Enter your phone number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.phoneNumberError != null,
                errorMessage = state.phoneNumberError,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            TextField(
                text = state.password,
                onTextChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                title = "Password",
                hint = "Enter your password",
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = painterResource(id = android.R.drawable.ic_menu_view), // placeholder for eye icon
                onClickTrailingIcon = { onIntent(RegisterIntent.TogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.passwordError != null,
                errorMessage = state.passwordError,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.large)
            )
        }

        item {
            PrimaryButton(
                caption = "Sign Up",
                onClick = { onIntent(RegisterIntent.SubmitRegistration) },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.medium)
            )
        }

        item {
            SecondaryButton(
                caption = "Register with Google",
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
                    text = "Already have an account? ",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont
                )
                Text(
                    text = "Sign In",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.primary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
