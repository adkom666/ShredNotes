package com.adkom666.shrednotes.util.time

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date

/**
 * A time that ignores information about seconds, milliseconds, etc.
 *
 * @property initialEpochMillis wrapped time as milliseconds since January 1, 1970, 00:00:00 GMT.
 */
@Parcelize
class Minutes(private val initialEpochMillis: Long) : Time(), Parcelable {

    /**
     * @param initialDate wrapped [Date].
     */
    constructor(initialDate: Date = Date()) : this(initialDate.time)

    /**
     * The time truncated to days, in milliseconds, since January 1, 1970, 00: 00: 00 GMT.
     */
    override val epochMillis: Long
        get() = initialEpochMillis.roundToMinutes()

    private fun Long.roundToMinutes(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
