package com.adkom666.shrednotes.util.paging

import kotlin.math.max

/**
 * Calculates the offset in the list that does not exceed the size of this list.
 *
 * @param requestedOffset offset to return if it is safe.
 * @param pageSize size of the page whose offset is calculated for.
 * @param count all items count.
 */
fun safeOffset(requestedOffset: Int, pageSize: Int, count: Int): Int {
    return if (requestedOffset < count) {
        requestedOffset
    } else {
        max(count - pageSize, 0)
    }
}
