package com.adkom666.shrednotes.util.time

import junit.framework.TestCase
import java.util.Calendar
import java.util.Date

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
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        assertEquals(MILLIS_PER_DAY, days.epochMillis % MILLIS_PER_DAY + zoneOffset)
    }

    fun testDate() {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        assertEquals(MILLIS_PER_DAY, days.date.time % MILLIS_PER_DAY + zoneOffset)
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
