package com.iti.domain.payment

import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.usecase.validation.CardValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CardValidatorsTest {

    @Test
    fun `accepts a Luhn-valid test card number`() {
        assertTrue(CardValidators.luhnCheck("4242 4242 4242 4242"))
    }

    @Test
    fun `rejects a card number with a broken checksum`() {
        assertFalse(CardValidators.luhnCheck("4242 4242 4242 4241"))
    }

    @Test
    fun `rejects blank or non-numeric input`() {
        assertFalse(CardValidators.luhnCheck(""))
        assertFalse(CardValidators.luhnCheck("not-a-card"))
    }

    @Test
    fun `detects Visa and Mastercard from the leading digits`() {
        assertEquals(CardBrand.VISA, CardValidators.brandFromNumber("4242424242424242"))
        assertEquals(CardBrand.MASTERCARD, CardValidators.brandFromNumber("5555555555554444"))
        assertEquals(CardBrand.MASTERCARD, CardValidators.brandFromNumber("2223000048400011"))
        assertNull(CardValidators.brandFromNumber("6011000000000004"))
    }

    @Test
    fun `flags a card number that does not match the selected brand`() {
        assertTrue(CardValidators.brandMatches("4242424242424242", CardBrand.VISA))
        assertFalse(CardValidators.brandMatches("4242424242424242", CardBrand.MASTERCARD))
    }

    @Test
    fun `accepts the current month and rejects a past month`() {
        val now = fixedCalendar(year = 2026, month = Calendar.AUGUST)
        assertTrue(CardValidators.isValidExpiry("08/26", now))
        assertTrue(CardValidators.isValidExpiry("12/26", now))
        assertFalse(CardValidators.isValidExpiry("07/26", now))
        assertFalse(CardValidators.isValidExpiry("01/25", now))
    }

    @Test
    fun `rejects malformed expiry input`() {
        val now = fixedCalendar(year = 2026, month = Calendar.AUGUST)
        assertFalse(CardValidators.isValidExpiry("13/26", now))
        assertFalse(CardValidators.isValidExpiry("2026-08", now))
        assertFalse(CardValidators.isValidExpiry("", now))
    }

    @Test
    fun `only a 3-digit CVV is accepted`() {
        assertTrue(CardValidators.isValidCvv("123"))
        assertFalse(CardValidators.isValidCvv("12"))
        assertFalse(CardValidators.isValidCvv("12345"))
        assertFalse(CardValidators.isValidCvv("12a"))
    }

    @Test
    fun `blank cardholder name is rejected`() {
        assertTrue(CardValidators.isNonBlankName("Test User"))
        assertFalse(CardValidators.isNonBlankName("   "))
        assertFalse(CardValidators.isNonBlankName(""))
    }

    private fun fixedCalendar(year: Int, month: Int): Calendar =
        Calendar.getInstance().apply { set(year, month, 15) }
}
