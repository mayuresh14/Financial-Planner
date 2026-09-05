package com.financeplanner.app.data.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financeplanner.app.R
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.SavedInvestmentType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Runs roughly once a day (see [ReminderScheduler]) and checks two independent things:
 * 1. Every saved FD/RD, for two milestones — 7 days before its maturity date, and the day it
 *    matures. Only FD/RD carry a fixed maturity date ([endDateMillis]) — other saved types just
 *    have a duration, not a calendar date to remind against.
 * 2. Whether today is the 1st of the month, for the separate "review your money" nudge.
 */
@HiltWorker
class MaturityReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SavedInvestmentRepository,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val notifier: MaturityReminderNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        notifier.ensureChannel()

        val preferences = preferencesDataStore.preferencesFlow.first()

        if (preferences.maturityRemindersEnabled) {
            val nowMillis = System.currentTimeMillis()
            val items = repository.observeAll().first()
            items.forEach { item ->
                if (item.type != SavedInvestmentType.FD && item.type != SavedInvestmentType.RD) return@forEach
                val endDateMillis = SavedInvestmentJson.decode(item.detailsJson)["endDateMillis"]?.toLongOrNull() ?: return@forEach
                val daysUntil = ((endDateMillis - nowMillis) / MILLIS_PER_DAY).toInt()
                if (daysUntil == REMINDER_LEAD_DAYS || daysUntil == 0) {
                    notifier.notify(item, daysUntil)
                }
            }
        }

        if (preferences.monthlyReminderEnabled) {
            val calendar = Calendar.getInstance()
            if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
                val monthIndex = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
                val textResId = MONTHLY_REMINDER_TEXTS[monthIndex.mod(MONTHLY_REMINDER_TEXTS.size)]
                notifier.notifyMonthlyReminder(applicationContext.getString(textResId))
            }
        }

        return Result.success()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        const val REMINDER_LEAD_DAYS = 7

        // Rotated by (year * 12 + month) so the same text isn't repeated month over month for
        // several years running, keeping the monthly nudge from feeling copy-pasted.
        val MONTHLY_REMINDER_TEXTS = listOf(
            R.string.monthly_reminder_body_1,
            R.string.monthly_reminder_body_2,
            R.string.monthly_reminder_body_3,
            R.string.monthly_reminder_body_4,
            R.string.monthly_reminder_body_5
        )
    }
}
