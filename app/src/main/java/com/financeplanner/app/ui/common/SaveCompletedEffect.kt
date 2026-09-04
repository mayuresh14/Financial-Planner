package com.financeplanner.app.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.financeplanner.app.R

/**
 * Shared post-save behavior for every calculator's Save flow: a quick toast,
 * then close the calculator (pop back), landing wherever the saved item's
 * list tab is (Investments or Calculations — the ViewModel already told
 * PendingTabNavigator which one before setting [saveCompleted]).
 */
@Composable
fun SaveCompletedEffect(saveCompleted: Boolean, onHandled: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val message = stringResource(R.string.save_success_toast)
    LaunchedEffect(saveCompleted) {
        if (saveCompleted) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onHandled()
            onBack()
        }
    }
}
