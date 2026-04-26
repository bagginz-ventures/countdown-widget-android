package com.bagginzventures.countdownwidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdateScheduler.schedule(context)
        CountdownAppWidgetProvider.updateAllWidgets(context)
    }
}
