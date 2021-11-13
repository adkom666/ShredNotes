package com.adkom666.shrednotes.util.time

import java.util.Calendar
import java.util.TimeZone

/**
 * This function is guaranteed to return a nonnull value: a timestamp or the smallest possible value
 * if [this] is null.
 *
 * @return timestamp or the smallest possible value if [this] is null.
 */
fun Days?.timestampOrMin(): Long {
    return timestampOrNull() ?: Long.MIN_VALUE
}

/**
 * This function is guaranteed to return a nonnull value: timestamp or maximum possible value if
 * [this] is null.
 *
 * @return timestamp or maximum possible value if [this] is null.
 */
fun Days?.timestampOrMax(): Long {
    return timestampOrNull() ?: Long.MAX_VALUE
}

/**
 * Getting the timestamp or null value if [this] is null.
 *
 * @return timestamp or null if [this] is null.
 */
fun Days?.timestampOrNull(): Long? {
    return this?.epochMillis
}

/**
 * Getting the timestamp for local time zone or null value if [this] is null.
 *
 * @return timestamp for local time zone or null if [this] is null.
 */
fun Days?.localTimestampOrNull(): Long? = this?.epochMillis?.localTimestamp()

/**
 * Create the [Calendar] for local time zone with the current long value as time in UTC milliseconds
 * from the epoch.
 *
 * @return [Calendar] for local time zone with the current long value as time in UTC milliseconds
 * from the epoch.
 */
fun Long.toLocalCalendar(): Calendar = toCalendar(TimeZone.getDefault())

private fun Long.localTimestamp(): Long {
    val zoneOffset = TimeZone.getDefault().getOffset(this)
    return this + zoneOffset
}

private fun Long.toCalendar(timeZone: TimeZone): Calendar {
    val calendar = Calendar.getInstance(timeZone)
    calendar.timeInMillis = this
    return calendar
}
