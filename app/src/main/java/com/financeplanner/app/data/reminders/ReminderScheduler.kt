package com.financeplanner.app.data.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Starts/stops the daily [MaturityReminderWorker] check, keyed off the user's
 * "maturity reminders" setting toggle. */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<MaturityReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextNineAm(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** So the daily check lands at a reasonable hour rather than whenever the toggle happened to be flipped. */
    private fun millisUntilNextNineAm(): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    private companion object {
        const val WORK_NAME = "maturity_reminder_check"
    }
}
