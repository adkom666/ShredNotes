package com.adkom666.shrednotes.util.time

import java.util.Calendar

/**
 * This function is guaranteed to return a nonnull value: a timestamp for the current timezone or
 * the smallest possible value if [this] is null.
 *
 * @param calendar [Calendar] to take into account the current timezone offset.
 * @return timestamp for the current timezone or the smallest possible value if [this] is null.
 */
fun Days?.timestampOrMin(calendar: Calendar): Long {
    return timestampOrNull(calendar) ?: Long.MIN_VALUE
}

/**
 * This function is guaranteed to return a nonnull value: timestamp for the current timezone or
 * maximum possible value if [this] is null.
 *
 * @param calendar [Calendar] to take into account the current timezone offset.
 * @return timestamp for the current timezone or maximum possible value if [this] is null.
 */
fun Days?.timestampOrMax(calendar: Calendar): Long {
    return timestampOrNull(calendar) ?: Long.MAX_VALUE
}

/**
 * Getting the timestamp for the current timezone or null value if [this] is null.
 *
 * @param calendar [Calendar] to take into account the current timezone offset.
 * @return timestamp for the current timezone or null if [this] is null.
 */
fun Days?.timestampOrNull(calendar: Calendar): Long? {
    val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
    return this?.epochMillis?.let { it + zoneOffset }
}
