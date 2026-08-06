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
    fun `accepts valid email formats`() {
        listOf(
            "user@example.com",
            "user.name+tag@sub.domain.org",
            "user_name@domain.co.uk",
            "user@domain.software",
            "  user@example.com  ",
        ).forEach { email ->
            assertTrue(email, AuthValidators.isValidEmail(email))
        }
    }

    @Test
    fun `rejects invalid email formats`() {
        listOf(
            "",
            "   ",
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "username@domain..com",
        ).forEach { email ->
            assertFalse(email, AuthValidators.isValidEmail(email))
        }
    }

    @Test
    fun `accepts strong passwords and rejects weak passwords`() {
        assertTrue(AuthValidators.isValidPassword("P@ssw0rd!"))
        assertTrue(AuthValidators.isValidPassword("Strong123#"))
        assertFalse(AuthValidators.isValidPassword("weak"))
        assertFalse(AuthValidators.isValidPassword("short1!"))
        assertFalse(AuthValidators.isValidPassword("nouppercase1!"))
        assertFalse(AuthValidators.isValidPassword("NOLOWERCASE1!"))
        assertFalse(AuthValidators.isValidPassword("NoSpecial123"))
        assertFalse(AuthValidators.isValidPassword("NoDigitSpecial!"))
    }
}
