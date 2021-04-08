package com.adkom666.shrednotes.data.model

import com.adkom666.shrednotes.util.time.Days
import junit.framework.TestCase

class NoteFilterTest : TestCase() {

    fun testIsNotDefined() {
        val filter = NoteFilter()
        assertFalse(filter.isDefined)
    }

    fun testIsDefined() {
        val filter = NoteFilter(dateFromInclusive = Days())
        assertTrue(filter.isDefined)
    }

    fun testEqualsWhenBothAreNotDefined() {
        val filter = NoteFilter()
        val otherFilter = NoteFilter()
        assertEquals(filter, otherFilter)
    }

    fun testEqualsWhenBothAreDefined() {
        val filter = NoteFilter(dateFromInclusive = Days())
        // This test fails if it's midnight here.
        val otherFilter = NoteFilter(dateFromInclusive = Days())
        assertEquals(filter, otherFilter)
    }

    fun testNotEqualsWhenOneIsDefinedAndOtherIsNotDefined() {
        val filter = NoteFilter(dateFromInclusive = Days())
        val otherFilter = NoteFilter()
        assertTrue(filter != otherFilter)
    }

    fun testEqualsByBpm() {
        val filter = NoteFilter(bpmFromInclusive = 666)
        val otherFilter = NoteFilter(bpmFromInclusive = 666)
        assertEquals(filter, otherFilter)
    }

    fun testNotEqualsByBpm() {
        val filter = NoteFilter(bpmFromInclusive = 666)
        val otherFilter = NoteFilter(bpmFromInclusive = 665)
        assertTrue(filter != otherFilter)
    }
}
