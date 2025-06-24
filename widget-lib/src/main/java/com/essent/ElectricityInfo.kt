package nl.essent.selfservice

import java.util.*

data class ElectricityInfo(
    val currentPrice: String,
    val timeRange: String,
    val hourlyPrices: List<Double>,
    val startHour: Int,
    val hourlyTimestamps: List<Calendar>
)
