package com.adkom666.shrednotes.util

import com.adkom666.shrednotes.util.time.Days
import java.text.DateFormat
import java.util.Locale

/**
 * Helps you to format date ranges for default locale.
 */
class DateRangeFormat {

    private companion object {
        private const val PLACEHOLDER_DATE_FROM = "-∞"
        private const val PLACEHOLDER_DATE_TO = "+∞"
    }

    private val dateFormat: DateFormat = DateFormat.getDateInstance(
        DateFormat.SHORT,
        Locale.getDefault()
    )

    /**
     * Formats a [dateRange] into a string.
     *
     * @param dateRange the value to be formatted into a time string.
     * @return the formatted date range string.
     */
    fun format(dateRange: DateRange): String {
        val formatFrom = dateRange.fromInclusive?.format() ?: PLACEHOLDER_DATE_FROM
        val formatTo = dateRange.toExclusive?.yesterday?.format() ?: PLACEHOLDER_DATE_TO
        return "$formatFrom — $formatTo"
    }

    private fun Days.format(): String? = dateFormat.format(date)
}
