package com.iti.domain.auth.usecase

import java.util.regex.Pattern

object AuthValidators {

    private const val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    private const val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$"


    private const val EGYPT_PHONE_PATTERN = "^(?:\\+20|0)?1[0125][0-9]{8}$"

    private val emailPattern = Pattern.compile(EMAIL_PATTERN)
    private val passwordPattern = Pattern.compile(PASSWORD_PATTERN)
    private val egyptPhonePattern = Pattern.compile(EGYPT_PHONE_PATTERN)

    fun isValidEmail(email: String): Boolean = emailPattern.matcher(email).matches()

    fun isValidPassword(password: String): Boolean = passwordPattern.matcher(password).matches()

    fun isValidPhoneNumber(phoneNumber: String): Boolean =
        egyptPhonePattern.matcher(phoneNumber.filterNot { it.isWhitespace() || it == '-' }).matches()
}

