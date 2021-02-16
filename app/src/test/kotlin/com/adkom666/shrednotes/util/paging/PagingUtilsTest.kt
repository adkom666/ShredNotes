package com.adkom666.shrednotes.util.paging

import org.junit.Assert.*
import org.junit.Test

class PagingUtilsTest {

    @Test
    fun testSafeOffset() {
        val offset = safeOffset(
            requestedOffset = 666,
            pageSize = 13,
            count = 1000
        )
        assertEquals(666, offset)
    }

    @Test
    fun testUnsafeOffset() {
        val offset = safeOffset(
            requestedOffset = 666,
            pageSize = 13,
            count = 100
        )
        assertEquals(87, offset)
    }
}
