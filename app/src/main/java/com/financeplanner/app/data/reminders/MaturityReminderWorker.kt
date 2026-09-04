package com.financeplanner.app.data.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.SavedInvestmentType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs roughly once a day (see [ReminderScheduler]) and checks every saved FD/RD for two
 * milestones: 7 days before its maturity date, and the day it matures. Only FD/RD carry a
 * fixed maturity date ([endDateMillis]) — other saved types just have a duration, not a
 * calendar date to remind against.
 */
@HiltWorker
class MaturityReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SavedInvestmentRepository,
    private val notifier: MaturityReminderNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        notifier.ensureChannel()

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

        return Result.success()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        const val REMINDER_LEAD_DAYS = 7
    }
}
