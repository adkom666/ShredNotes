package com.adkom666.shrednotes.statistics.util

import com.adkom666.shrednotes.util.time.Days

/**
 * Actual lower limit of the range.
 *
 * @param fromDateInclusive lower range limit.
 * @param firstDate the date of the very first entry.
 * @return actual lower limit of the range.
 */
fun startDateInclusive(fromDateInclusive: Days?, firstDate: Days): Days {
    return if (
        fromDateInclusive == null ||
        fromDateInclusive < firstDate
    ) {
        firstDate
    } else {
        fromDateInclusive
    }
}

/**
 * Actual upper limit of the range.
 *
 * @param toDateExclusive upper range limit.
 * @param lastDate the date of the very last entry.
 * @return actual upper limit of the range.
 */
fun endDateInclusive(toDateExclusive: Days?, lastDate: Days): Days {
    val toDateInclusive = toDateExclusive?.yesterday
    val nowDate = Days()
    val finalDate = if (nowDate > lastDate) nowDate else lastDate
    return if (
        toDateInclusive == null ||
        toDateInclusive > finalDate
    ) {
        finalDate
    } else {
        toDateInclusive
    }
}
