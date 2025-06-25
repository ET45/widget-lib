package nl.essent.selfservice
import com.essent.widget.R

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*


object WidgetUpdater {

    fun update(context: Context, info: ElectricityInfo?) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, WidgetProvider::class.java)
        val widgetIds = widgetManager.getAppWidgetIds(component)

        val now = Calendar.getInstance()
        val futureIndexes = info?.hourlyTimestamps
            ?.withIndex()
            ?.filter { it.value.after(now) || it.value.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY) }

        val minIndex = futureIndexes?.minByOrNull { info.hourlyPrices[it.index] }?.index ?: -1
        val minPrice = if (minIndex != -1) info?.hourlyPrices?.get(minIndex) else null
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        widgetIds.forEach { id ->
    val options = widgetManager.getAppWidgetOptions(id)
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

    val layoutId = when {
        minWidth > 300 && minHeight > 260 -> R.layout.widget_layout_large
        minWidth > 300 -> R.layout.widget_layout_medium
        else -> R.layout.widget_layout_small
    }

    val views = RemoteViews(context.packageName, layoutId)
    updateViews(context, views, info, minIndex, minPrice, currentHour)
    widgetManager.updateAppWidget(id, views)
}

    }

    fun updateViews(
        context: Context,
        views: RemoteViews,
        info: ElectricityInfo?,
        minHour: Int,
        minPriceValue: Double?,
        currentHour: Int
    ) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val end = (now.clone() as Calendar).apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.MINUTE, 0)
}
        val nowLabel = "Nu tot ${formatter.format(end.time)}"
        views.setTextViewText(R.id.time_range, nowLabel)
        views.setTextViewText(R.id.price_today, info?.currentPrice ?: "N/A")
        views.setTextViewText(R.id.price_min, if (minPriceValue != null) "€ %.3f".format(Locale.GERMANY, minPriceValue) else "?")

        val isValidMin = info != null && minHour in info.hourlyTimestamps.indices &&
            (info.hourlyTimestamps[minHour].after(now) ||
             info.hourlyTimestamps[minHour].get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY))

        val label = if (isValidMin) {
            val start = info!!.hourlyTimestamps[minHour]
            val end = (start.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            if (start.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)) {
                "Nu tot ${formatter.format(end.time)}"
            } else {
                "${formatter.format(start.time)} - ${formatter.format(end.time)}"
            }
        } else ""

        views.setTextViewText(R.id.lowest_hour, label)

        info?.let {
            val chart: Bitmap = WidgetChartRenderer.render(context, it.hourlyPrices, currentHour, it.startHour, it.hourlyTimestamps, minHour)
            views.setImageViewBitmap(R.id.barChart, chart)
        }
    }
}
