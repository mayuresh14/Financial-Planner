package com.financeplanner.app.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Adds [months] to [startMillis] using calendar month arithmetic (handles month-length differences). */
fun addMonths(startMillis: Long, months: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startMillis
    calendar.add(Calendar.MONTH, months)
    return calendar.timeInMillis
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
