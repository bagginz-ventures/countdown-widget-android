package com.bagginzventures.countdownwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

object WidgetUpdateScheduler {
    private const val REQUEST_CODE = 4011

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CountdownAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextHour = LocalDateTime.now()
            .plusHours(1)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.cancel(pendingIntent)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            nextHour,
            AlarmManager.INTERVAL_HOUR,
            pendingIntent
        )
    }
}
