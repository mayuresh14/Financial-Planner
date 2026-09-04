package com.financeplanner.app.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.financeplanner.app.R

/** Where POST_NOTIFICATIONS stands for this app right now. */
enum class NotificationPermissionState {
    /** Already granted — nothing to do. Always this on pre-Android 13, since the runtime
     * permission doesn't exist there. */
    GRANTED,
    /** Not granted, but the OS system prompt can still be shown (either it's never been
     * asked, or it was denied once and Android allows asking again). */
    CAN_PROMPT,
    /** Not granted, and the OS won't show its own prompt again — the only way forward is
     * the app's notification settings screen. */
    BLOCKED
}

/**
 * [hasRequestedBefore] is this app's own record of whether the OS prompt has ever actually
 * been shown (see [AppDisplayPreferences.hasRequestedNotificationPermission]) — needed because
 * `shouldShowRequestPermissionRationale` returns false both when a permission was never asked
 * AND when it's permanently denied; the two are only distinguishable together with our own flag.
 */
fun notificationPermissionState(context: Context, hasRequestedBefore: Boolean): NotificationPermissionState {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return NotificationPermissionState.GRANTED
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        return NotificationPermissionState.GRANTED
    }
    val activity = context as? Activity
    val canPrompt = !hasRequestedBefore ||
        (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
    return if (canPrompt) NotificationPermissionState.CAN_PROMPT else NotificationPermissionState.BLOCKED
}

/** Deep-links to this app's notification settings screen — the only way to grant
 * POST_NOTIFICATIONS once the in-app system prompt has been permanently denied. */
fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
    }
    context.startActivity(intent)
}

/** Shown once, on first launch: asks whether to turn maturity reminders on at all, before
 * ever touching the OS permission prompt. */
@Composable
fun MaturityReminderIntroDialog(onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.maturity_reminder_intro_title)) },
        text = { Text(stringResource(R.string.maturity_reminder_intro_message)) },
        confirmButton = {
            TextButton(onClick = onTurnOn) {
                Text(stringResource(R.string.maturity_reminder_intro_turn_on))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.maturity_reminder_intro_not_now))
            }
        }
    )
}

/** Shown when reminders were on but the permission has since been revoked (e.g. from system
 * Settings) and the OS prompt can still be shown again — offers to re-request it inline. */
@Composable
fun NotificationPermissionRevokedDialog(onEnable: () -> Unit, onTurnOff: () -> Unit) {
    AlertDialog(
        onDismissRequest = onTurnOff,
        title = { Text(stringResource(R.string.notification_permission_revoked_title)) },
        text = { Text(stringResource(R.string.notification_permission_revoked_message)) },
        confirmButton = {
            TextButton(onClick = onEnable) {
                Text(stringResource(R.string.notification_permission_revoked_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onTurnOff) {
                Text(stringResource(R.string.notification_permission_denied_turn_off))
            }
        }
    )
}

/** Shown when the OS prompt can no longer be shown (permanently denied) — the only path
 * forward is the app's notification settings screen. */
@Composable
fun NotificationPermissionBlockedDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_denied_title)) },
        text = { Text(stringResource(R.string.notification_permission_denied_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.notification_permission_denied_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_denied_not_now))
            }
        }
    )
}
