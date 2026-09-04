package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R
import com.financeplanner.app.domain.util.amountToIndianWords

/**
 * The standard amount-entry field for every calculator: live Indian
 * comma-grouping as the user types (via [IndianCurrencyVisualTransformation])
 * plus a spelled-out "amount in words" readout, shown as a bold tonal chip
 * above the field so it actually draws the eye — a quiet caption below the
 * field (the original design) went unnoticed in practice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val visualTransformation = remember { IndianCurrencyVisualTransformation() }
    val words = remember(value) {
        value.substringBefore('.').toLongOrNull()?.takeIf { it > 0 }?.let { amountToIndianWords(it) }
    }

    Column(modifier = modifier) {
        if (words != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.amount_in_words_suffix, words),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
            label = label,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon
        )
    }
}
