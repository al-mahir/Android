package com.iti.domain.auth.model


object AuthField {
    const val USERNAME = "username"
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val PHONE_NUMBER = "phoneNumber"
    const val OTP = "otp"
}


object AuthValidationCode {
    const val VALIDATION_FAILED = "validation_failed"
    const val REQUIRED = "required"
    const val INVALID_EMAIL = "invalid_email"
    const val WEAK_PASSWORD = "weak_password"
    const val INVALID_PHONE_NUMBER = "invalid_phone_number"
    const val INVALID_OTP = "invalid_otp"
}
