package nl.essent.selfservice
import com.essent.widget.R

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object ElectricityFetcher {

    fun fetch(): ElectricityInfo? {
        return try {
            val url = URL("https://www.essent.nl/api/public/tariffmanagement/dynamic-prices/v1/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parse(response)
            } else {
                Log.e("ElectricityFetcher", "API Error: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("ElectricityFetcher", "Fetch error", e)
            null
        }
    }

    private fun parse(jsonResponse: String): ElectricityInfo? {
        return try {
            val json = JSONObject(jsonResponse)
            val pricesArray = json.getJSONArray("prices")
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val today = Calendar.getInstance()
            val todayDate = sdf.format(today.time)
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
            val tomorrowDate = sdf.format(tomorrow.time)
            val currentHour = today.get(Calendar.HOUR_OF_DAY)

            val hourlyPrices = mutableListOf<Double>()
            val hourlyTimestamps = mutableListOf<Calendar>()
            var currentPrice: String? = null
            var timeRange: String? = null
            var startHour = 0

            val hoursToInclude = mutableListOf<Pair<String, JSONObject>>()

            fun collect(date: String, from: Int, to: Int) {
                for (i in 0 until pricesArray.length()) {
                    val priceDay = pricesArray.getJSONObject(i)
                    if (priceDay.getString("date") == date) {
                        val tariffs = priceDay.getJSONObject("electricity").getJSONArray("tariffs")
                        for (j in 0 until tariffs.length()) {
                            val tariff = tariffs.getJSONObject(j)
                            val hour = tariff.getString("startDateTime").substring(11, 13).toInt()
                            if (hour in from until to) {
                                hoursToInclude.add(date to tariff)
                            }
                        }
                    }
                }
            }

            collect(todayDate, 12, 24)

            val hasTomorrow = (0 until pricesArray.length()).any {
                pricesArray.getJSONObject(it).getString("date") == tomorrowDate
            }

            if (hasTomorrow) {
                collect(tomorrowDate, 0, 12)
            } else {
                hoursToInclude.clear()
                collect(todayDate, 0, 24)
            }

            if (hoursToInclude.isNotEmpty()) {
                startHour = hoursToInclude.first().second.getString("startDateTime").substring(11, 13).toInt()
            }

            for ((date, obj) in hoursToInclude) {
                val amount = obj.getDouble("totalAmount")
                hourlyPrices.add(amount)

                val startDate = isoFormat.parse(obj.getString("startDateTime"))!!
                val cal = Calendar.getInstance().apply { this.time = startDate }
                hourlyTimestamps.add(cal)

                val hour = obj.getString("startDateTime").substring(11, 13).toInt()
                if (date == sdf.format(today.time) && hour == currentHour) {
                    currentPrice = "€ %.3f".format(Locale.GERMANY,amount)
                    timeRange = "${obj.getString("startDateTime").substring(11, 16)} - ${obj.getString("endDateTime").substring(11, 16)}"
                }
            }

            if (currentPrice != null && timeRange != null) {
                ElectricityInfo(currentPrice, timeRange, hourlyPrices, startHour, hourlyTimestamps)
            } else null

        } catch (e: Exception) {
            Log.e("ElectricityFetcher", "JSON parse error", e)
            null
        }
    }
}
