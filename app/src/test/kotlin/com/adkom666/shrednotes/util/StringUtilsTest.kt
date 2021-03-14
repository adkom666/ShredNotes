package com.adkom666.shrednotes.util

import junit.framework.TestCase

class StringUtilsTest : TestCase() {

    fun testQueryVsUntrimmedQuery() {
        val query = "query"
        val otherQuery = " query\t"
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testQueryVsNull() {
        val query = "query"
        val otherQuery = null
        assertTrue(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testNullVsQuery() {
        val query = null
        val otherQuery = "query"
        assertTrue(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testNullVsNull() {
        val query = null
        val otherQuery = null
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testNullVsEmptyQuery() {
        val query = null
        val otherQuery = ""
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testEmptyQueryVsNull() {
        val query = null
        val otherQuery = ""
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testNullVsBlankQuery() {
        val query = null
        val otherQuery = " \t "
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }

    fun testBlankQueryVsNull() {
        val query = " \t "
        val otherQuery = null
        assertFalse(query containsDifferentTrimmedTextIgnoreCaseThan otherQuery)
    }
}
