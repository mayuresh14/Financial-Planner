package com.financeplanner.app.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.financeplanner.app.domain.util.groupIndianDigits

/**
 * Displays a raw digit(.digit) string with live Indian comma grouping
 * (e.g. "1234567" -> "12,34,567") without altering the underlying field
 * value — so ViewModel state and parsing (toDoubleOrNull) stay untouched.
 */
class IndianCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val grouped = groupIndianDigits(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var digitsSeen = 0
                for ((index, char) in grouped.withIndex()) {
                    if (digitsSeen == offset) return index
                    if (char != ',') digitsSeen++
                }
                return grouped.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, grouped.length)
                var digitsSeen = 0
                for (index in 0 until clamped) {
                    if (grouped[index] != ',') digitsSeen++
                }
                return digitsSeen
            }
        }

        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }
}
