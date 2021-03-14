package com.adkom666.shrednotes.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date

/**
 * A date that ignores information about seconds, milliseconds, etc.
 *
 * @property initialDate wrapped [Date].
 */
@Parcelize
class TruncatedToMinutesDate(private val initialDate: Date = Date()) : Parcelable {

    /**
     * @param initialEpochMillis wrapped date and time as milliseconds since January 1, 1970,
     * 00:00:00 GMT.
     */
    constructor(initialEpochMillis: Long) : this(Date(initialEpochMillis))

    /**
     * Truncated to minutes date and time as milliseconds since January 1, 1970, 00:00:00 GMT.
     * Always a multiple of 60 000.
     */
    val epochMillis: Long
        get() = roundToMinutes(initialDate.time)

    /**
     * Truncated to minutes date and time as [Date]. Its 'time' field' is always a multiple of
     * 60 000.
     */
    val date: Date
        get() = Date(epochMillis)

    override fun equals(other: Any?): Boolean {
        return other is TruncatedToMinutesDate && epochMillis == other.epochMillis
    }

    override fun hashCode(): Int {
        return epochMillis.hashCode()
    }

    override fun toString(): String {
        return epochMillis.toString()
    }

    private fun roundToMinutes(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
