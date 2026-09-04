package com.financeplanner.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R

/**
 * Shown up to [MAX_LOCAL_DATA_POPUP_SHOWN_COUNT] times (once per app launch,
 * via AppDisplayPreferences.localDataPopupShownCount) to reassure new users
 * that every calculation stays on-device — nothing is uploaded or saved to a
 * server. A big bouncing/wiggling/blinking smiley plus short, simple copy so
 * it reads as a friendly heads-up, not a legal notice.
 *
 * Custom-drawn on a Canvas rather than an emoji glyph: emoji are fixed-color
 * bitmap/COLR fonts, so a Text's `color` param has no effect on them — there
 * was no way to make "😊" dark yellow. Drawing it directly also makes the
 * blinking-eyes animation possible.
 */
const val MAX_LOCAL_DATA_POPUP_SHOWN_COUNT = 3

private val SmileyFaceColor = Color(0xFFF9A825) // dark yellow / amber, not a pale/bright yellow

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

    // Eyes stay open most of the time, then snap shut and back open for a quick blink,
    // repeating every 3s — independent of the wiggle so the two don't sync up oddly.
    val blinkTransition = rememberInfiniteTransition(label = "smiley_blink")
    val eyeOpenness by blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                1f at 0
                1f at 2700
                0.05f at 2860 using LinearEasing
                1f at 3000 using LinearEasing
            }
        ),
        label = "smiley_blink_openness"
    )

    val remaining = (MAX_LOCAL_DATA_POPUP_SHOWN_COUNT - shownCount - 1).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Canvas(
                modifier = Modifier
                    .size(64.dp)
                    .scale(entranceScale.value)
                    .rotate(if (entranceScale.value >= 0.99f) wiggle else 0f)
            ) {
                val radius = size.minDimension / 2f
                drawCircle(color = SmileyFaceColor, radius = radius)

                val eyeWidth = radius * 0.16f
                val eyeHeight = eyeWidth * eyeOpenness.coerceAtLeast(0.05f)
                val eyeY = center.y - radius * 0.25f
                val eyeOffsetX = radius * 0.35f
                listOf(-1f, 1f).forEach { side ->
                    drawOval(
                        color = Color.Black,
                        topLeft = Offset(
                            x = center.x + side * eyeOffsetX - eyeWidth / 2f,
                            y = eyeY - eyeHeight / 2f
                        ),
                        size = Size(eyeWidth, eyeHeight)
                    )
                }

                val mouthWidth = radius * 1.0f
                val mouthHeight = radius * 0.7f
                drawArc(
                    color = Color.Black,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - mouthWidth / 2f, center.y - radius * 0.1f),
                    size = Size(mouthWidth, mouthHeight)
                )
            }
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
