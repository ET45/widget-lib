package nl.essent.selfservice

import android.content.Context
import android.graphics.*
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import java.util.*


object WidgetChartRenderer {

    fun render(
        context: Context,
        prices: List<Double>,
        currentHour: Int,
        startHour: Int,
        hourlyTimestamps: List<Calendar>,
        minIndex: Int
    ): Bitmap {
        val scaleFactor = 2f

        val baseWidth = 316
        val baseHeight = 170
        val totalWidth = (baseWidth * scaleFactor).toInt()
        val totalHeight = (baseHeight * scaleFactor).toInt()

        val nuLabelHeight = 20f
        val labelHeight = 20f
        val chartHeight = baseHeight - nuLabelHeight - labelHeight

        val minHeight = 10
        val horizontalPadding = 16f
        val width = baseWidth - 2 * horizontalPadding

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scaleFactor, scaleFactor)

        val now = Calendar.getInstance()

val nowLinePaint = Paint().apply {
    color = ContextCompat.getColor(context, R.color.widget_text_secondary)
    strokeWidth = 1f
    isAntiAlias = true
}

val textPaint = Paint().apply {
    color = ContextCompat.getColor(context, R.color.widget_text_primary)
    textSize = 15f
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    isSubpixelText = true
}

val labelPaint = Paint().apply {
    color = ContextCompat.getColor(context, R.color.widget_text_primary)
    textSize = 15f
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    isSubpixelText = true
}


        val barCount = 24
        val barWidth = width / barCount.toFloat()
        val maxPrice = prices.maxOrNull() ?: 1.0
        val minPrice = prices.minOrNull() ?: 0.0
        val range = maxPrice - minPrice
        val adjustedRange = if (range == 0.0) 1.0 else range
        val cornerRadius = 4f

        for (i in prices.indices) {
            val percent = ((prices[i] - minPrice) / adjustedRange).coerceIn(0.0, 1.0)
            val baseBarHeight = 20f
            val barHeight = (percent * (chartHeight - baseBarHeight) + baseBarHeight).toFloat()
            val left = i * barWidth + barWidth / 2f - 2f + horizontalPadding
            val top = nuLabelHeight + (chartHeight - barHeight)
            val right = left + 4f

            val isCurrentHour = hourlyTimestamps[i].get(Calendar.HOUR_OF_DAY) == currentHour &&
                hourlyTimestamps[i].get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

            val isMinValid = minIndex == i &&
                (hourlyTimestamps[i].after(now) || isCurrentHour)

            val barColor = when {
                isMinValid -> ContextCompat.getColor(context, R.color.bar_min_valid)
                isCurrentHour -> ContextCompat.getColor(context, R.color.primary)
                hourlyTimestamps[i].before(now) -> ContextCompat.getColor(context, R.color.bar_past)
                else -> ContextCompat.getColor(context, R.color.primary)
            }


            val barPaint = Paint().apply {
                color = barColor
                isAntiAlias = true
            }

            val barRect = RectF(left, top, right, nuLabelHeight + chartHeight)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)

            if (isCurrentHour) {
            val lineX = left + 2f
            canvas.drawLine(lineX, nuLabelHeight, lineX, nuLabelHeight + chartHeight, nowLinePaint)


            val nuTextY = (nuLabelHeight - 4f).coerceAtLeast(12f)
            canvas.drawText("Nu", lineX, nuTextY, textPaint)

            }
        }

        val labelHours = listOf(0, 6, 12, 18, 23).map { (it + startHour) % 24 }
        for ((index, hour) in labelHours.withIndex()) {
            val label = String.format("%02d:00", hour)
            val rawHour = listOf(0, 6, 12, 18, 23)[index]
            val x = (rawHour + 0.5f) * barWidth + horizontalPadding
            canvas.drawText(label, x, baseHeight - 6f, labelPaint)
        }

        return bitmap
    }
}
