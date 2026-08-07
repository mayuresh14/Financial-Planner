package com.financeplanner.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financeplanner.app.R

/**
 * Shown up to [MAX_LOCAL_DATA_POPUP_SHOWN_COUNT] times (once per app launch,
 * via AppDisplayPreferences.localDataPopupShownCount) to reassure new users
 * that every calculation stays on-device — nothing is uploaded or saved to a
 * server. A big bouncing/wiggling smiley plus short, simple copy so it reads
 * as a friendly heads-up, not a legal notice.
 */
const val MAX_LOCAL_DATA_POPUP_SHOWN_COUNT = 3

@Composable
fun LocalDataPopup(shownCount: Int, onDismiss: () -> Unit) {
    // Entrance: pop in with an overshoot bounce.
    val entranceScale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    // Once settled, keep a gentle happy wiggle going for as long as the dialog is up.
    val infiniteTransition = rememberInfiniteTransition(label = "smiley_wiggle")
    val wiggle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smiley_wiggle_angle"
    )

    val remaining = (MAX_LOCAL_DATA_POPUP_SHOWN_COUNT - shownCount - 1).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "😊",
                fontSize = 44.sp,
                modifier = Modifier
                    .scale(entranceScale.value)
                    .rotate(if (entranceScale.value >= 0.99f) wiggle else 0f)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.local_data_popup_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(text = stringResource(R.string.local_data_popup_body))
                if (remaining > 0) {
                    Text(
                        text = stringResource(R.string.local_data_popup_subtext, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.local_data_popup_button))
            }
        }
    )
}
