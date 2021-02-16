package com.adkom666.shrednotes.util

import org.junit.Assert.*
import org.junit.Test

class StringUtilsTest {

    @Test
    fun testQueryVsUntrimmedQuery() {
        val query = "query"
        val otherQuery = " query\t"
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testQueryVsNull() {
        val query = "query"
        val otherQuery = null
        assertTrue(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testNullVsQuery() {
        val query = null
        val otherQuery = "query"
        assertTrue(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testNullVsNull() {
        val query = null
        val otherQuery = null
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testNullVsEmptyQuery() {
        val query = null
        val otherQuery = ""
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testEmptyQueryVsNull() {
        val query = null
        val otherQuery = ""
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testNullVsBlankQuery() {
        val query = null
        val otherQuery = " \t "
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    @Test
    fun testBlankQueryVsNull() {
        val query = " \t "
        val otherQuery = null
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }
}
