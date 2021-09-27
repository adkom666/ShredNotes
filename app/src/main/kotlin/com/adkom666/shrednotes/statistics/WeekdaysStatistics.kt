package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.util.Weekday

typealias WeekdayValues = Map<Weekday, Float>

/**
 * Statistical quantities by days of week.
 *
 * @property valueMap target quantities corresponding to the days of week.
 */
data class WeekdaysStatistics(
    val valueMap: WeekdayValues
)
