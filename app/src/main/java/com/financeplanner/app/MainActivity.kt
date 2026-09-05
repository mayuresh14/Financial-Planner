package com.financeplanner.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.MaturityReminderIntroDialog
import com.financeplanner.app.ui.common.NotificationPermissionBlockedDialog
import com.financeplanner.app.ui.common.NotificationPermissionRevokedDialog
import com.financeplanner.app.ui.common.NotificationPermissionState
import com.financeplanner.app.ui.common.notificationPermissionState
import com.financeplanner.app.ui.common.openAppNotificationSettings
import com.financeplanner.app.ui.navigation.FinancePlannerNavHost
import com.financeplanner.app.ui.navigation.PendingInvestmentDetailNavigator
import com.financeplanner.app.ui.navigation.PendingTabNavigator
import com.financeplanner.app.ui.navigation.TabRoutes
import com.financeplanner.app.ui.theme.FinancePlannerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Single-activity app. More destinations (remaining calculators, goals,
// settings, onboarding, "Coming Soon" gating) get added to
// FinancePlannerNavHost as they're built — see ui/navigation/.
//
// AppSettingsViewModel is hoisted here (Activity-scoped) so every screen's
// hiltViewModel() call for it resolves to the SAME instance — theme/language
// changes from any screen's settings sheet apply app-wide immediately.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var pendingTabNavigator: PendingTabNavigator
    @Inject lateinit var pendingInvestmentDetailNavigator: PendingInvestmentDetailNavigator
    @Inject lateinit var analytics: AppAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsState()
            val hasLoadedPreferences by settingsViewModel.hasLoadedPreferences.collectAsState()
            val context = LocalContext.current

            var showIntroDialog by remember { mutableStateOf(false) }
            var showRevokedDialog by remember { mutableStateOf(false) }
            var showBlockedDialog by remember { mutableStateOf(false) }

            // Shared by every path that actually invokes the OS prompt (intro "Turn On", and
            // the launch-time "permission was revoked, re-enable?" dialog) — granted flips both
            // reminder types on, denied flips both off, so the switches always reflect reality.
            // Both share the one POST_NOTIFICATIONS permission, so there's no separate ask per type.
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                settingsViewModel.setHasRequestedNotificationPermission(true)
                settingsViewModel.setHasShownMaturityReminderIntro(true)
                settingsViewModel.setMaturityRemindersEnabled(granted)
                settingsViewModel.setMonthlyReminderEnabled(granted)
            }

            LaunchedEffect(hasLoadedPreferences) {
                if (!hasLoadedPreferences) return@LaunchedEffect

                if (!preferences.hasShownMaturityReminderIntro) {
                    // First launch ever. Below Android 13 there's no runtime permission to ask
                    // for at all, so just turn both reminder types on directly.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        settingsViewModel.setMaturityRemindersEnabled(true)
                        settingsViewModel.setMonthlyReminderEnabled(true)
                        settingsViewModel.setHasShownMaturityReminderIntro(true)
                    } else {
                        showIntroDialog = true
                    }
                    return@LaunchedEffect
                }

                // Not first launch: if either reminder type was left on, re-verify the
                // permission still holds — it may have been revoked from system Settings.
                val remindersWanted = preferences.maturityRemindersEnabled || preferences.monthlyReminderEnabled
                if (remindersWanted) {
                    when (notificationPermissionState(context, preferences.hasRequestedNotificationPermission)) {
                        NotificationPermissionState.GRANTED -> Unit
                        NotificationPermissionState.CAN_PROMPT -> showRevokedDialog = true
                        NotificationPermissionState.BLOCKED -> {
                            settingsViewModel.setMaturityRemindersEnabled(false)
                            settingsViewModel.setMonthlyReminderEnabled(false)
                            showBlockedDialog = true
                        }
                    }
                }
            }

            if (showIntroDialog) {
                MaturityReminderIntroDialog(
                    onTurnOn = {
                        showIntroDialog = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onNotNow = {
                        showIntroDialog = false
                        settingsViewModel.setMaturityRemindersEnabled(false)
                        settingsViewModel.setMonthlyReminderEnabled(false)
                        settingsViewModel.setHasShownMaturityReminderIntro(true)
                    }
                )
            }
            if (showRevokedDialog) {
                NotificationPermissionRevokedDialog(
                    onEnable = {
                        showRevokedDialog = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onTurnOff = {
                        showRevokedDialog = false
                        settingsViewModel.setMaturityRemindersEnabled(false)
                        settingsViewModel.setMonthlyReminderEnabled(false)
                    }
                )
            }
            if (showBlockedDialog) {
                NotificationPermissionBlockedDialog(
                    onDismiss = { showBlockedDialog = false },
                    onOpenSettings = {
                        showBlockedDialog = false
                        openAppNotificationSettings(context)
                    }
                )
            }

            // Apply the selected app language via AppCompatDelegate — takes
            // effect without an Activity restart on Android 13+, and via the
            // compat path on older versions (requires SplitCompat / manifest
            // service handled automatically by the appcompat library).
            LaunchedEffect(preferences.language) {
                val localeList = LocaleListCompat.forLanguageTags(preferences.language.localeTag)
                AppCompatDelegate.setApplicationLocales(localeList)
            }

            FinancePlannerTheme(
                themeMode = preferences.themeMode,
                themePreset = preferences.themePreset
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinancePlannerNavHost()
                }
            }
        }
    }

    // Notification tap on an already-running instance (FLAG_ACTIVITY_CLEAR_TOP) delivers here
    // rather than a fresh onCreate — handle the deep link the same way in both places.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_REMINDER_TYPE)?.let { analytics.logReminderNotificationTapped(it) }

        if (intent?.getBooleanExtra(EXTRA_OPEN_INVESTMENTS_TAB, false) == true) {
            pendingTabNavigator.requestTab(TabRoutes.INVESTMENTS)
            val investmentId = intent.getLongExtra(EXTRA_INVESTMENT_ID, NO_INVESTMENT_ID)
            if (investmentId != NO_INVESTMENT_ID) {
                pendingInvestmentDetailNavigator.requestDetail(investmentId)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_INVESTMENTS_TAB = "open_investments_tab"
        const val EXTRA_INVESTMENT_ID = "investment_id"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        private const val NO_INVESTMENT_ID = Long.MIN_VALUE
    }
}
