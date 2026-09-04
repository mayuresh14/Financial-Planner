package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.financeplanner.app.domain.model.AssetCategory

/**
 * Shared "Save" flow for every investment-type calculator (SIP, FD, RD,
 * PPF, EPF, SSY, NPS, Lumpsum, Goal-based SIP). Collects a custom name plus
 * optional institution/notes so the same calculator can represent multiple
 * real-world instances — e.g. two RDs at different banks for different
 * purposes — and be told apart later in "My Investments".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveInvestmentSheet(
    onDismiss: () -> Unit,
    onSave: (customName: String, institutionName: String?, notes: String?, assetCategory: AssetCategory?) -> Unit,
    initialCustomName: String = "",
    initialInstitutionName: String = "",
    initialNotes: String = "",
    showAssetCategory: Boolean = false,
    initialAssetCategory: AssetCategory = AssetCategory.MUTUAL_FUND
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customName by remember { mutableStateOf(initialCustomName) }
    var institutionName by remember { mutableStateOf(initialInstitutionName) }
    var notes by remember { mutableStateOf(initialNotes) }
    var assetCategory by remember { mutableStateOf(initialAssetCategory) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.save_investment_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(stringResource(R.string.save_investment_label_name)) },
                placeholder = { Text(stringResource(R.string.save_investment_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (showAssetCategory) {
                Text(text = stringResource(R.string.save_investment_label_asset_category), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AssetCategory.entries) { category ->
                        FilterChip(
                            selected = assetCategory == category,
                            onClick = { assetCategory = category },
                            label = { Text(assetCategoryLabel(category)) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = institutionName,
                onValueChange = { institutionName = it },
                label = { Text(stringResource(R.string.save_investment_label_institution)) },
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
                onClick = {
                    onSave(
                        customName.trim(),
                        institutionName.trim().ifBlank { null },
                        notes.trim().ifBlank { null },
                        if (showAssetCategory) assetCategory else null
                    )
                },
                enabled = customName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_investment_button_save))
            }
        }
    }
}
