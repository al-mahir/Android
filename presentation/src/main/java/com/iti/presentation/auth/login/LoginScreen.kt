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

@Composable
fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
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
            text = "Welcome Back",
            style = Theme.typography.h4,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.small)
        )
        
        Text(
            text = "Sign in to continue",
            style = Theme.typography.body.large,
            color = Theme.colors.secondaryFont,
            modifier = Modifier.padding(bottom = Theme.spacing.large)
        )

        TextField(
            text = state.email,
            onTextChange = { onIntent(LoginIntent.EmailChanged(it)) },
            title = "Email Address",
            hint = "Enter your email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null,
            errorMessage = state.emailError,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))

        TextField(
            text = state.password,
            onTextChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            title = "Password",
            hint = "Enter your password",
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = painterResource(id = android.R.drawable.ic_menu_view), // placeholder for eye icon
            onClickTrailingIcon = { onIntent(LoginIntent.TogglePasswordVisibility) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.passwordError != null,
            errorMessage = state.passwordError,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.small))
        
        Text(
            text = "Forgot Password?",
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
            caption = "Sign In",
            onClick = { onIntent(LoginIntent.SubmitLogin) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        
        SecondaryButton(
            caption = "Continue with Google",
            onClick = { onIntent(LoginIntent.GoogleSignInClicked) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.padding(vertical = Theme.spacing.medium),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account? ",
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont
            )
            Text(
                text = "Sign Up",
                style = Theme.typography.body.medium,
                color = Theme.colors.primary,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}
