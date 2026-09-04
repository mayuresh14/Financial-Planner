package com.financeplanner.app.domain.util

/**
 * Converts a non-negative amount to spelled-out English words using the
 * Indian numbering system (thousand / lakh / crore), e.g. 1234567 ->
 * "Twelve Lakh Thirty Four Thousand Five Hundred Sixty Seven".
 *
 * English-only: a linguistically correct number-to-words conversion for
 * Hindi/Marathi/Tamil/Telugu (which don't compose the way English does —
 * e.g. Hindi has a unique word for every number 1-99) is out of scope here;
 * this is shown regardless of the app's selected language.
 */
private val ONES = arrayOf(
    "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
    "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
)
private val TENS = arrayOf(
    "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
)

private fun twoDigitsToWords(n: Int): String = when {
    n == 0 -> ""
    n < 20 -> ONES[n]
    else -> {
        val tensWord = TENS[n / 10]
        val onesDigit = n % 10
        if (onesDigit == 0) tensWord else "$tensWord ${ONES[onesDigit]}"
    }
}

private fun threeDigitsToWords(n: Int): String {
    val hundreds = n / 100
    val rest = n % 100
    return when {
        hundreds == 0 -> twoDigitsToWords(rest)
        rest == 0 -> "${ONES[hundreds]} Hundred"
        else -> "${ONES[hundreds]} Hundred ${twoDigitsToWords(rest)}"
    }
}

fun amountToIndianWords(amount: Long): String {
    if (amount <= 0L) return ONES[0]

    var remaining = amount
    val crore = remaining / 10_000_000L; remaining %= 10_000_000L
    val lakh = remaining / 100_000L; remaining %= 100_000L
    val thousand = remaining / 1_000L; remaining %= 1_000L
    val hundredsGroup = remaining.toInt()

    val parts = mutableListOf<String>()
    if (crore > 0) parts += "${threeDigitsToWords(crore.toInt())} Crore"
    if (lakh > 0) parts += "${twoDigitsToWords(lakh.toInt())} Lakh"
    if (thousand > 0) parts += "${twoDigitsToWords(thousand.toInt())} Thousand"
    if (hundredsGroup > 0) parts += threeDigitsToWords(hundredsGroup)

    return parts.joinToString(", ")
}
