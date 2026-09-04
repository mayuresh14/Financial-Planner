package com.financeplanner.app.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.financeplanner.app.MainActivity
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.SavedInvestment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/** Builds and fires the "FD/RD maturing soon" local notification — one channel, one
 * notification per saved item per milestone (7 days out, and on the day itself). */
class MaturityReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.maturity_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.maturity_reminder_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** No-ops silently if POST_NOTIFICATIONS isn't granted — the worker has no UI to fall back to. */
    fun notify(item: SavedInvestment, daysUntilMaturity: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val body = if (daysUntilMaturity <= 0) {
            context.getString(R.string.maturity_reminder_body_today, item.customName)
        } else {
            context.getString(R.string.maturity_reminder_body_upcoming, item.customName, daysUntilMaturity)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_INVESTMENTS_TAB, true)
                .putExtra(MainActivity.EXTRA_INVESTMENT_ID, item.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.maturity_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        // Distinct notification ID per item+milestone so the 7-day and day-of reminders for the
        // same item don't overwrite each other, and different items never collide.
        val notificationId = (item.id * 10 + if (daysUntilMaturity <= 0) 0 else 1).toInt()
        runCatching { notificationManager.notify(notificationId, notification) }
    }

    private companion object {
        const val CHANNEL_ID = "maturity_reminders"
    }
}
