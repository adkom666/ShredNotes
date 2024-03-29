package com.adkom666.shrednotes.util.time

import java.util.Date

/**
 * Time based on milliseconds since January 1, 1970, 00:00:00 GMT.
 */
abstract class Time {

    /**
     * Time as milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    abstract val epochMillis: Long

    /**
     * Time as [Date].
     */
    val date: Date
        get() = Date(epochMillis)

    override fun equals(other: Any?): Boolean {
        return other is Time && epochMillis == other.epochMillis
    }

    override fun hashCode(): Int {
        return epochMillis.hashCode()
    }

    override fun toString(): String {
        return epochMillis.toString()
    }

    /**
     * Compares this object with the specified object for order.
     *
     * @param other the object to compare.
     * @return zero if this object is equal to the specified other object, a negative number if it's
     * less than other, or a positive number if it's greater than other.
     */
    operator fun compareTo(other: Time): Int {
        val difference = epochMillis - other.epochMillis
        return when {
            difference in Int.MIN_VALUE..Int.MAX_VALUE -> difference.toInt()
            difference < Int.MIN_VALUE -> Int.MIN_VALUE
            difference > Int.MAX_VALUE -> Int.MAX_VALUE
            else -> 0
        }
    }
}
