package com.financeplanner.app.domain.util

/**
 * Indian digit grouping (2,2,3 — e.g. 12,34,56,789) for the integer part of
 * a raw numeric string. The fractional part (if any, after '.') is left
 * untouched. Used to live-format amount inputs as the user types.
 */
fun groupIndianDigits(raw: String): String {
    val dotIndex = raw.indexOf('.')
    val intPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
    val fracPart = if (dotIndex >= 0) raw.substring(dotIndex) else ""

    if (intPart.length <= 3) return intPart + fracPart

    val lastThree = intPart.takeLast(3)
    var remaining = intPart.dropLast(3)
    val groups = ArrayDeque<String>()
    while (remaining.length > 2) {
        groups.addFirst(remaining.takeLast(2))
        remaining = remaining.dropLast(2)
    }
    if (remaining.isNotEmpty()) groups.addFirst(remaining)
    groups.addLast(lastThree)

    return groups.joinToString(",") + fracPart
}
