package nl.essent.selfservice

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.*

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        fetchAndUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        context?.let { fetchAndUpdate(it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED -> {
                fetchAndUpdate(context)
                scheduleWidgetUpdate(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledUpdates(context)
    }

    private fun fetchAndUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val info = ElectricityFetcher.fetch()
            withContext(Dispatchers.Main) {
                // Abort if no data
                val data = info ?: return@withContext

                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, WidgetProvider::class.java)
                )

                // Design tokens
                val bgColor       = ContextCompat.getColor(context, R.color.widget_bg_default)
                val textPrimary   = ContextCompat.getColor(context, R.color.widget_text_primary)
                val textSecondary = ContextCompat.getColor(context, R.color.widget_text_secondary)
                val accentColor   = ContextCompat.getColor(context, R.color.primary)
                val iconTint      = ContextCompat.getColor(context, R.color.primary)

                val now = Calendar.getInstance()
                val futureData = data.hourlyTimestamps
                    .withIndex()
                    .filter {
                        it.value.after(now) ||
                        it.value.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                    }

                val minIndex = futureData.minByOrNull { data.hourlyPrices[it.index] }?.index ?: -1
                val minPrice = if (minIndex != -1) data.hourlyPrices[minIndex] else null
                val currentHour = now.get(Calendar.HOUR_OF_DAY)

               

                ids.forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                    val layoutId = when {
                        minWidth > 300 && minHeight > 260 -> R.layout.widget_layout_large
                        minWidth > 300                     -> R.layout.widget_layout_medium
                        else                               -> R.layout.widget_layout_small
                    }

                    val views = RemoteViews(context.packageName, layoutId)

                    // Deep link Intent
                    val brand = getBrand(context)
                    val ecmp = "wid:gib:gib:gib:\$brand:dynamische_grafiek:widget_click:b2c"
                    val uri = Uri.parse("nl.essent.selfservice://dynamic-pricing")
                        .buildUpon()
                        .appendQueryParameter("ecmp", ecmp)
                        .build()

                    val pi = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(Intent.ACTION_VIEW, uri),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetRoot, pi)

                    // Populate and update
                    WidgetUpdater.updateViews(
                        context,
                        views,
                        data,
                        minIndex,
                        minPrice,
                        currentHour
                    )
                    manager.updateAppWidget(id, views)
                }
            }
        }
    }

    private fun scheduleWidgetUpdate(context: Context) {
        val intent = Intent(context, WidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000L,
            60_000L,
            pi
        )
    }

    private fun cancelScheduledUpdates(context: Context) {
        val intent = Intent(context, WidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    private fun getBrand(context: Context): String = "essent"
}