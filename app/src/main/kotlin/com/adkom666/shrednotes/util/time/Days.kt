package com.adkom666.shrednotes.util.time

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date

/**
 * A time that ignores information about hours, minutes, seconds, milliseconds, etc.
 *
 * @property initialEpochMillis wrapped time as milliseconds since January 1, 1970, 00:00:00 GMT.
 */
@Parcelize
class Days(private val initialEpochMillis: Long) : Time(), Parcelable {

    /**
     * @param initialDate wrapped [Date].
     */
    constructor(initialDate: Date = Date()) : this(initialDate.time)

    /**
     * @param initialMinutes wrapped [Minutes].
     */
    constructor(initialMinutes: Minutes) : this(initialMinutes.epochMillis)

    /**
     * The time truncated to days, in milliseconds, since January 1, 1970, 00: 00: 00 GMT.
     */
    override val epochMillis: Long
        get() = initialEpochMillis.roundToDays()

    /**
     * The time truncated to days, which was exactly one day ago, in milliseconds since January 1,
     * 1970, 00: 00: 00 GMT.
     */
    val yesterday: Days
        get() = Days(epochMillis.addDays(-1))

    /**
     * The time truncated to days, which will be exactly one day later, in milliseconds from January
     * 1, 1970, 00: 00: 00 GMT.
     */
    val tomorrow: Days
        get() = Days(epochMillis.addDays(1))

    private fun Long.roundToDays(): Long {
        val calendar = this.toLocalCalendar()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun Long.addDays(amount: Int): Long {
        val calendar = this.toLocalCalendar()
        calendar.add(Calendar.DATE, amount)
        return calendar.timeInMillis
    }
}
