package com.iti.domain.payment.usecase.validation

import com.iti.domain.payment.model.CardBrand
import java.util.Calendar


object CardValidators {

    private val VISA_PREFIX = Regex("^4")
    private val MASTERCARD_PREFIX = Regex("^(5[1-5]|2(2[2-9][1-9]|2[3-9][0-9]|[3-6][0-9]{2}|7[01][0-9]|720))")
    private val EXPIRY_PATTERN = Regex("^(0[1-9]|1[0-2])/([0-9]{2})$")

    fun luhnCheck(cardNumber: String): Boolean {
        val digits = cardNumber.filterNot { it.isWhitespace() }
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return false

        var sum = 0
        var doubleDigit = false
        for (i in digits.length - 1 downTo 0) {
            var digit = digits[i] - '0'
            if (doubleDigit) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            doubleDigit = !doubleDigit
        }
        return sum % 10 == 0
    }

    fun brandFromNumber(cardNumber: String): CardBrand? {
        val digits = cardNumber.filterNot { it.isWhitespace() }
        return when {
            VISA_PREFIX.containsMatchIn(digits) -> CardBrand.VISA
            MASTERCARD_PREFIX.containsMatchIn(digits) -> CardBrand.MASTERCARD
            else -> null
        }
    }

    fun brandMatches(cardNumber: String, selectedBrand: CardBrand): Boolean =
        brandFromNumber(cardNumber) == selectedBrand

    fun isValidExpiry(mmYy: String, now: Calendar = Calendar.getInstance()): Boolean {
        val match = EXPIRY_PATTERN.matchEntire(mmYy) ?: return false
        val month = match.groupValues[1].toInt()
        val twoDigitYear = match.groupValues[2].toInt()
        val fullYear = 2000 + twoDigitYear

        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed

        return fullYear > currentYear || (fullYear == currentYear && month >= currentMonth)
    }

    fun isValidCvv(cvv: String): Boolean = cvv.length == 3 && cvv.all { it.isDigit() }

    fun isNonBlankName(name: String): Boolean = name.isNotBlank()
}
