package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R

/**
 * Shown once, right after the app tour finishes or is skipped (see
 * HomeScreen), so every calculator can prefill its return/inflation fields
 * from day one instead of starting blank. Editable later from the same two
 * fields in ThemeLanguageSheet — this sheet only exists to collect an
 * initial value, not to be the sole place these can be changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultRatesSetupSheet(
    initialInflationPercent: Double,
    initialExpectedReturnPercent: Double,
    onSave: (inflationPercent: Double, expectedReturnPercent: Double) -> Unit
) {
    var inflationText by remember { mutableStateOf(initialInflationPercent.toString()) }
    var returnText by remember { mutableStateOf(initialExpectedReturnPercent.toString()) }

    // Fully non-cancellable: confirmValueChange rejects the swipe-to-Hidden
    // gesture (a no-op onDismissRequest alone doesn't stop that, since
    // dragging manipulates the sheet's internal state directly), and
    // shouldDismissOnBackPress/onDismissRequest block back-press and
    // tap-outside. The user must tap Continue to close this sheet.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    ModalBottomSheet(
        onDismissRequest = { /* one-time setup — must be confirmed, not dismissed */ },
        sheetState = sheetState,
        properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.default_rates_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.default_rates_setup_message),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = returnText,
                onValueChange = { returnText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text(stringResource(R.string.settings_default_expected_return)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = inflationText,
                onValueChange = { inflationText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text(stringResource(R.string.settings_default_inflation)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = {
                    onSave(
                        inflationText.toDoubleOrNull() ?: initialInflationPercent,
                        returnText.toDoubleOrNull() ?: initialExpectedReturnPercent
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.default_rates_button_continue))
            }
        }
    }
}
