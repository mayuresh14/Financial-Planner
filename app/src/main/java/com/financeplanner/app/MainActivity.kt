package com.financeplanner.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.navigation.FinancePlannerNavHost
import com.financeplanner.app.ui.theme.FinancePlannerTheme
import dagger.hilt.android.AndroidEntryPoint

// Single-activity app. More destinations (remaining calculators, goals,
// settings, onboarding, "Coming Soon" gating) get added to
// FinancePlannerNavHost as they're built — see ui/navigation/.
//
// AppSettingsViewModel is hoisted here (Activity-scoped) so every screen's
// hiltViewModel() call for it resolves to the SAME instance — theme/language
// changes from any screen's settings sheet apply app-wide immediately.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsState()

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
}
