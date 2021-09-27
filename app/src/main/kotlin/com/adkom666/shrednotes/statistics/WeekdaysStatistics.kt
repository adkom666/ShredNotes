package com.adkom666.shrednotes.statistics

typealias WeekdayValues = Map<Weekday, Float>

/**
 * Statistical quantities by days of week.
 *
 * @property valueMap target quantities corresponding to the days of week.
 */
data class WeekdaysStatistics(
    val valueMap: WeekdayValues
)
