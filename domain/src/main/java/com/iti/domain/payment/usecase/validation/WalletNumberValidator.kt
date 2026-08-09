package com.iti.domain.payment.usecase.validation

import com.iti.domain.payment.model.WalletProvider
import java.util.regex.Pattern


object WalletNumberValidator {

    private const val EGYPT_MOBILE_PATTERN = "^(?:\\+20|0)?1[0125][0-9]{8}$"
    private val egyptMobilePattern = Pattern.compile(EGYPT_MOBILE_PATTERN)

    fun isValidEgyptianMobile(number: String): Boolean =
        egyptMobilePattern.matcher(number.normalized()).matches()

    fun matchesCarrierPrefix(number: String, provider: WalletProvider): Boolean {
        val normalized = number.normalized()
        if (!egyptMobilePattern.matcher(normalized).matches()) return false
        val local = normalized.removePrefix("+20").removePrefix("20").let {
            if (it.startsWith("0")) it else "0$it"
        }
        return local.startsWith(provider.egyptianPrefix)
    }

    private fun String.normalized(): String = filterNot { it.isWhitespace() || it == '-' }
}
