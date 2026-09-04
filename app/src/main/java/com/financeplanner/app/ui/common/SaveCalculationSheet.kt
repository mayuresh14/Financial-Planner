package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R

/**
 * Shared "Save" flow for scenario-calculator saves (SIP, Lumpsum, Goal-based
 * SIP) — a "what-if" snapshot, not a real account, so unlike
 * [SaveInvestmentSheet] there's no institution/bank field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveCalculationSheet(
    onDismiss: () -> Unit,
    onSave: (customName: String, notes: String?) -> Unit,
    initialCustomName: String = "",
    initialNotes: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customName by remember { mutableStateOf(initialCustomName) }
    var notes by remember { mutableStateOf(initialNotes) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.save_calculation_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(stringResource(R.string.save_investment_label_name)) },
                placeholder = { Text(stringResource(R.string.save_calculation_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.save_investment_label_notes)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(customName.trim(), notes.trim().ifBlank { null }) },
                enabled = customName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_investment_button_save))
            }
        }
    }
}
