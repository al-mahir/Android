package com.iti.presentation.payment.checkout.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Groups a plain digit string into 4s for display ("4242 4242 4242 4242"). The underlying state
 * (`CheckoutUiState.cardNumber`) stays digits-only — this is display-only formatting.
 */
internal class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(MAX_DIGITS)
        val formatted = digits.chunked(GROUP_SIZE).joinToString(" ")

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digits.length)
                return clamped + clamped.groupsBefore()
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val spaces = formatted.take(clamped).count { it == ' ' }
                return (clamped - spaces).coerceIn(0, digits.length)
            }

            private fun Int.groupsBefore(): Int = if (this == 0) 0 else (this - 1) / GROUP_SIZE
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    private companion object {
        const val GROUP_SIZE = 4
        const val MAX_DIGITS = 16
    }
}

/**
 * Inserts a "/" after the month for display ("08/26"). The underlying state
 * (`CheckoutUiState.expiry`) stays a plain "MMYY" digit string — this is display-only formatting.
 */
internal class ExpiryDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(MAX_DIGITS)
        val formatted = if (digits.length > MONTH_DIGITS) {
            "${digits.take(MONTH_DIGITS)}/${digits.drop(MONTH_DIGITS)}"
        } else {
            digits
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digits.length)
                return if (clamped <= MONTH_DIGITS) clamped else clamped + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return if (clamped <= MONTH_DIGITS) clamped else (clamped - 1).coerceIn(0, digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    private companion object {
        const val MONTH_DIGITS = 2
        const val MAX_DIGITS = 4
    }
}
