package com.adkom666.shrednotes.util.time

import junit.framework.TestCase

class MinutesTest : TestCase() {

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }

    private val minutes: Minutes
        get() = requireNotNull(_minutes)

    private var _minutes: Minutes? = null

    public override fun setUp() {
        super.setUp()
        _minutes = Minutes()
    }

    fun testEpochMillis() {
        assertEquals(0L, minutes.epochMillis % MILLIS_PER_MINUTE)
    }

    fun testDate() {
        assertEquals(0L, minutes.date.time % MILLIS_PER_MINUTE)
    }

    fun testTestEquals() {
        val other = Minutes(minutes.epochMillis + MILLIS_PER_MINUTE - 1)
        assertTrue(minutes == other)
    }

    fun testTestNotEquals() {
        val other = Minutes(minutes.epochMillis + MILLIS_PER_MINUTE)
        assertFalse(minutes == other)
    }
}
