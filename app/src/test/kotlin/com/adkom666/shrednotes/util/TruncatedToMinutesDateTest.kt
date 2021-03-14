package com.adkom666.shrednotes.util

import junit.framework.TestCase

class TruncatedToMinutesDateTest : TestCase() {

    private val date: TruncatedToMinutesDate
        get() = requireNotNull(_date)

    private var _date: TruncatedToMinutesDate? = null

    public override fun setUp() {
        super.setUp()
        _date = TruncatedToMinutesDate()
    }

    fun testEpochMillis() {
        assertEquals(0L, date.epochMillis % 60_000L)
    }

    fun testDate() {
        assertEquals(0L, date.date.time % 60_000L)
    }

    fun testTestEquals() {
        val other = TruncatedToMinutesDate(date.epochMillis + 59_999L)
        assertTrue(date == other)
    }

    fun testTestNotEquals() {
        val other = TruncatedToMinutesDate(date.epochMillis + 60_000L)
        assertFalse(date == other)
    }
}
