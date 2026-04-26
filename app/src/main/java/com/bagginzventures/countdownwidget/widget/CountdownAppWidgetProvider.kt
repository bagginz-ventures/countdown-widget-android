package com.bagginzventures.countdownwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.bagginzventures.countdownwidget.MainActivity
import com.bagginzventures.countdownwidget.R
import com.bagginzventures.countdownwidget.data.CountdownCalculator
import com.bagginzventures.countdownwidget.data.CountdownPresentation
import com.bagginzventures.countdownwidget.data.CountdownRepository
import com.bagginzventures.countdownwidget.data.PhotoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

const val ACTION_REFRESH_WIDGET = "com.bagginzventures.countdownwidget.action.REFRESH_WIDGET"

private enum class WidgetLayoutMode {
    SMALL,
    COMPACT,
    FULL
}

class CountdownAppWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.schedule(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateScheduler.schedule(context)
        updateWidgets(context, appWidgetIds, appWidgetManager)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, intArrayOf(appWidgetId), appWidgetManager)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetUpdateScheduler.schedule(context)
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownAppWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(context, ids, manager)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetIds: IntArray,
            appWidgetManager: AppWidgetManager
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = CountdownRepository(context)
                val config = repository.config.first()
                val presentation = CountdownCalculator.presentation(config)
                val photoStorage = PhotoStorage(context)
                val activePhotoPath = resolveActivePhotoPath(config.backgroundPhotoPaths, config.rotationHours)
                val backgroundBitmap = activePhotoPath?.let { photoStorage.loadBitmap(it) }
                val openAppIntent = PendingIntent.getActivity(
                    context,
                    991,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                appWidgetIds.forEach { widgetId ->
                    val layoutMode = resolveLayoutMode(appWidgetManager.getAppWidgetOptions(widgetId))
                    val layoutRes = when (layoutMode) {
                        WidgetLayoutMode.SMALL -> R.layout.app_widget_small
                        WidgetLayoutMode.COMPACT -> R.layout.app_widget_compact
                        WidgetLayoutMode.FULL -> R.layout.app_widget_countdown
                    }
                    val views = RemoteViews(context.packageName, layoutRes).apply {
                        setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.widget_background)
                        setTextViewText(R.id.widgetDaysValue, presentation.daysValue)
                        setTextColor(R.id.widgetDaysValue, Color.WHITE)
                        setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)

                        if (backgroundBitmap != null) {
                            setViewVisibility(R.id.widgetBackgroundImage, View.VISIBLE)
                            setImageViewBitmap(R.id.widgetBackgroundImage, backgroundBitmap)
                            setInt(R.id.widgetOverlay, "setBackgroundColor", 0x66000000)
                        } else {
                            setViewVisibility(R.id.widgetBackgroundImage, View.GONE)
                            setInt(R.id.widgetOverlay, "setBackgroundColor", Color.TRANSPARENT)
                        }

                        when (layoutMode) {
                            WidgetLayoutMode.SMALL -> Unit
                            WidgetLayoutMode.COMPACT -> {
                                setTextViewText(R.id.widgetTitle, config.title)
                                setTextViewText(R.id.widgetDaysLabel, compactLabel(presentation))
                                setTextColor(R.id.widgetTitle, 0xFFE7ECF5.toInt())
                                setTextColor(R.id.widgetDaysLabel, 0xFFCAD5E2.toInt())
                            }
                            WidgetLayoutMode.FULL -> {
                                setInt(R.id.widgetAccentBar, "setBackgroundColor", config.accentTheme.accentColor.toInt())
                                setTextColor(R.id.widgetChip, config.accentTheme.accentColor.toInt())
                                setTextViewText(R.id.widgetChip, config.accentTheme.displayName)
                                setTextViewText(R.id.widgetTitle, config.title)
                                setTextViewText(R.id.widgetDaysLabel, presentation.statusLabel)
                                setTextViewText(R.id.widgetTargetDate, presentation.detailLabel)
                                setTextColor(R.id.widgetTitle, Color.WHITE)
                                setTextColor(R.id.widgetDaysLabel, 0xFFCAD5E2.toInt())
                                setTextColor(R.id.widgetTargetDate, 0xFF93A4B8.toInt())
                            }
                        }
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }

        private fun resolveLayoutMode(options: Bundle?): WidgetLayoutMode {
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

            return when {
                minWidth <= 110 && minHeight <= 110 -> WidgetLayoutMode.SMALL
                minWidth <= 180 || minHeight <= 110 -> WidgetLayoutMode.COMPACT
                else -> WidgetLayoutMode.FULL
            }
        }

        private fun compactLabel(presentation: CountdownPresentation): String = when (presentation.statusLabel) {
            "days left" -> "days"
            "days since" -> "since"
            "happening today" -> "today"
            else -> presentation.statusLabel
        }

        private fun resolveActivePhotoPath(photoPaths: List<String>, rotationHours: Int): String? {
            if (photoPaths.isEmpty()) return null
            if (photoPaths.size == 1) return photoPaths.first()
            val rotationWindow = rotationHours.coerceIn(1, 168)
            val epochHours = Instant.now().atZone(ZoneOffset.UTC).toEpochSecond() / 3600
            val index = ((epochHours / rotationWindow) % photoPaths.size).toInt()
            return photoPaths.getOrNull(index)
        }
    }
}
