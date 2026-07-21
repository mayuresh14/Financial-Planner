package com.financeplanner.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R

private data class TourStep(val titleRes: Int, val bodyRes: Int)

private val tourSteps = listOf(
    TourStep(R.string.tour_step_calculators_title, R.string.tour_step_calculators_body),
    TourStep(R.string.tour_step_goals_title, R.string.tour_step_goals_body),
    TourStep(R.string.tour_step_settings_title, R.string.tour_step_settings_body)
)

/**
 * Shown once on first launch (gated by AppDisplayPreferences.hasSeenAppTour
 * in DataStore — see AppSettingsViewModel.setHasSeenAppTour). A simple
 * dialog-based carousel rather than a full spotlight overlay, to keep this
 * lightweight; each step highlights one pillar of the app.
 */
@Composable
fun TourOverlay(onFinished: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = tourSteps[stepIndex]
    val isLastStep = stepIndex == tourSteps.lastIndex

    AlertDialog(
        onDismissRequest = onFinished,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 8.dp,
        title = {
            Text(
                stringResource(step.titleRes),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(step.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    tourSteps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .background(
                                    color = if (index == stepIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isLastStep) onFinished() else stepIndex++
            }) {
                Text(stringResource(if (isLastStep) R.string.tour_button_done else R.string.tour_button_next))
            }
        },
        dismissButton = {
            if (!isLastStep) {
                TextButton(onClick = onFinished) {
                    Text(stringResource(R.string.tour_button_skip))
                }
            }
        }
    )
}
