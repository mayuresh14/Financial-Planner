package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.SaveTarget

/**
 * Shown before the actual save sheet for calculators where the result is
 * genuinely ambiguous (SIP, Lumpsum, Goal-based SIP) — could be a real
 * recurring/one-time investment you hold, or just a "what-if" scenario.
 * Skipped when editing an existing saved item, since its kind is already
 * fixed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTargetChooserSheet(onDismiss: () -> Unit, onChoose: (SaveTarget) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.save_target_chooser_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.save_target_chooser_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = { onChoose(SaveTarget.INVESTMENT) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_target_chooser_investment))
            }
            OutlinedButton(onClick = { onChoose(SaveTarget.CALCULATION) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_target_chooser_calculation))
            }
        }
    }
}
