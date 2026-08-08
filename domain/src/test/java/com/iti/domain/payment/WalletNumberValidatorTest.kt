package com.iti.domain.payment

import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.usecase.validation.WalletNumberValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletNumberValidatorTest {

    @Test
    fun `accepts valid Egyptian mobile numbers in any common format`() {
        listOf(
            "01012345678",
            "01112345678",
            "01212345678",
            "01512345678",
            "1012345678",
            "+201012345678",
            "010 1234 5678",
        ).forEach { number ->
            assertTrue(number, WalletNumberValidator.isValidEgyptianMobile(number))
        }
    }

    @Test
    fun `rejects malformed numbers`() {
        listOf(
            "",
            "01312345678",
            "0101234567",
            "010123456789",
            "01012345678a",
        ).forEach { number ->
            assertFalse(number, WalletNumberValidator.isValidEgyptianMobile(number))
        }
    }

    @Test
    fun `matches each wallet provider to its own carrier prefix`() {
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("01012345678", WalletProvider.VODAFONE_CASH))
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("01212345678", WalletProvider.ORANGE_CASH))
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("01112345678", WalletProvider.ETISALAT_CASH))
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("01512345678", WalletProvider.WE_PAY))
    }

    @Test
    fun `rejects a number that belongs to a different carrier than the selected wallet`() {
        assertFalse(WalletNumberValidator.matchesCarrierPrefix("01212345678", WalletProvider.VODAFONE_CASH))
        assertFalse(WalletNumberValidator.matchesCarrierPrefix("01012345678", WalletProvider.ORANGE_CASH))
        assertFalse(WalletNumberValidator.matchesCarrierPrefix("01012345678", WalletProvider.ETISALAT_CASH))
        assertFalse(WalletNumberValidator.matchesCarrierPrefix("01012345678", WalletProvider.WE_PAY))
    }

    @Test
    fun `matches carrier prefix regardless of +20 or leading-zero formatting`() {
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("+201012345678", WalletProvider.VODAFONE_CASH))
        assertTrue(WalletNumberValidator.matchesCarrierPrefix("1012345678", WalletProvider.VODAFONE_CASH))
    }

    @Test
    fun `a malformed number never matches any carrier prefix`() {
        assertFalse(WalletNumberValidator.matchesCarrierPrefix("not-a-number", WalletProvider.VODAFONE_CASH))
    }
}
