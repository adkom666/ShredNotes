package com.adkom666.shrednotes.statistics

/**
 * Statistical quantities by days of week.
 *
 * @property valueMap target quantities corresponding to the days of week.
 */
data class WeekdaysStatistics(
    val valueMap: Map<Weekday, Float>
)
