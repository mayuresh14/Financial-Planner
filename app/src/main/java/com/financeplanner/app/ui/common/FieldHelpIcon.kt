package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R

/**
 * A small tap-to-reveal "what does this mean" affordance for fields whose
 * label alone isn't self-explanatory (step-up %, inflation %, NPS annuity
 * allocation, etc.). Deliberately a tap-triggered dialog rather than a
 * long-press tooltip — long-press has poor discoverability on touch UIs
 * with no hover state to hint it exists.
 */
@Composable
fun FieldHelpIcon(explanation: String) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(24.dp)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.field_help_icon_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 8.dp,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            text = {
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.field_help_dismiss))
                }
            }
        )
    }
}
