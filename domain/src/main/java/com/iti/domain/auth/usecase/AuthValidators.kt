package com.iti.domain.auth.usecase

import java.util.regex.Pattern

object AuthValidators {

    private const val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    private const val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$"

    fun isValidEmail(email: String): Boolean {
        return Pattern.compile(EMAIL_PATTERN).matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches()
    }
}
