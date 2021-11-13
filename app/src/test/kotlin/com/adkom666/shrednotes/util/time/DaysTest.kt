package com.adkom666.shrednotes.util.time

import junit.framework.TestCase
import java.util.TimeZone

class DaysTest : TestCase() {

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }

    private val days: Days
        get() = requireNotNull(_days)

    private var _days: Days? = null

    public override fun setUp() {
        super.setUp()
        _days = Days()
    }

    fun testEpochMillis() {
        val zoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val epochMillis = days.epochMillis + zoneOffset
        assertEquals(0L, epochMillis % MILLIS_PER_DAY)
    }

    fun testDate() {
        val zoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val epochMillis = days.date.time + zoneOffset
        assertEquals(0L, epochMillis % MILLIS_PER_DAY)
    }

    fun testTestEquals() {
        val other = Days(days.epochMillis + MILLIS_PER_DAY - 1L)
        assertTrue(days == other)
    }

    fun testTestNotEquals() {
        val other = Days(days.epochMillis + MILLIS_PER_DAY)
        assertFalse(days == other)
    }

    fun testYesterday() {
        assertEquals(days.epochMillis, days.yesterday.epochMillis + MILLIS_PER_DAY)
    }

    fun testTomorrow() {
        assertEquals(days.epochMillis, days.tomorrow.epochMillis - MILLIS_PER_DAY)
    }
}
