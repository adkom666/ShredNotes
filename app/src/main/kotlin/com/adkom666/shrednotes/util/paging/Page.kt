package com.adkom666.shrednotes.util.paging

/**
 * Portion of the items from some global list.
 *
 * @property items list of items.
 * @property offset position of the first item in the global list (starting with 0).
 */
data class Page<T>(val items: List<T>, val offset: Int)
