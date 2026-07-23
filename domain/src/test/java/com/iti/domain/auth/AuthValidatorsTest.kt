package com.iti.domain.auth

import com.iti.domain.auth.usecase.AuthValidators
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The accepted/rejected numbers below were confirmed against the backend's own registration
 * validation, so this test fails if the local rule ever drifts away from the server's.
 */
class AuthValidatorsTest {

    @Test
    fun `accepts the mobile formats the backend accepts`() {
        listOf(
            "01012345678",
            "01112345678",
            "01212345678",
            "01512345678",
            "1012345678",
            "1247170592",
            "+201012345678",
            "010 1234 5678",
        ).forEach { number ->
            assertTrue(number, AuthValidators.isValidPhoneNumber(number))
        }
    }

    @Test
    fun `rejects the mobile formats the backend rejects`() {
        listOf(
            "",
            "01312345678",    // 013 is not an Egyptian operator prefix
            "0101234567",     // one digit short
            "010123456789",   // one digit long
            "01012345678a",
            "0201012345678",
        ).forEach { number ->
            assertFalse(number, AuthValidators.isValidPhoneNumber(number))
        }
    }
}
