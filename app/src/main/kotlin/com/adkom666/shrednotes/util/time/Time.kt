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
}
