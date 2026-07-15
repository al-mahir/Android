package com.example.designsystem.components.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme

@Preview(showBackground = true, name = "OTP – empty")
@Composable
private fun OtpFieldEmptyPreview() {
    AlMahirTheme {
        OtpField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "OTP – partial")
@Composable
private fun OtpFieldPartialPreview() {
    AlMahirTheme {
        OtpField(
            value = "12",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "OTP – filled")
@Composable
private fun OtpFieldFilledPreview() {
    AlMahirTheme {
        OtpField(
            value = "3351",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "OTP – error")
@Composable
private fun OtpFieldErrorPreview() {
    AlMahirTheme {
        OtpField(
            value = "3300",
            onValueChange = {},
            isError = true,
            errorMessage = "Invalid code",
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
        )
    }
}
