package com.adkom666.shrednotes.util.paging

import junit.framework.TestCase

class PagingUtilsTest : TestCase() {

    fun testSafeOffset() {
        val offset = safeOffset(
            requestedOffset = 666,
            pageSize = 13,
            count = 1000
        )
        assertEquals(666, offset)
    }

    fun testUnsafeOffset() {
        val offset = safeOffset(
            requestedOffset = 666,
            pageSize = 13,
            count = 100
        )
        assertEquals(87, offset)
    }
}
