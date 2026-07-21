package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financeplanner.app.R
import com.financeplanner.app.domain.util.amountToIndianWords
import java.text.NumberFormat

/** "₹1,00,000 (One Lakh Rupees)" — the standard way a rupee amount is quoted inside a result narrative. */
@Composable
fun formatAmountWithWords(amount: Double, currencyFormat: NumberFormat): String {
    val currencyText = currencyFormat.format(amount)
    val words = amount.toLong().takeIf { it > 0 }?.let { amountToIndianWords(it) }
    return if (words != null) {
        "$currencyText (${stringResource(R.string.amount_in_words_suffix, words)})"
    } else {
        currencyText
    }
}

/**
 * A plain-English narrative summarizing the calculation, shown alongside the
 * numeric result card so a non-technical user can read the outcome as a
 * sentence rather than parsing labeled figures alone.
 */
@Composable
fun NarrativeResultCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}
