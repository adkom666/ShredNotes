package com.adkom666.shrednotes.ui

/**
 * Search related content.
 */
interface Searchable {

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean

    /**
     * Current search query, if it exists.
     */
    val currentQuery: String?

    /**
     * Search for a given query.
     *
     * @param query text or null.
     * @return true if the query has been handled.
     */
    fun search(query: String?): Boolean

    /**
     * Show intermediate result of the currently typed query text.
     *
     * @param newText currently typed query text or null.
     * @return true if the action was handled.
     */
    fun preview(newText: String?): Boolean

    /**
     * Don't search anything.
     */
    fun unsearch()
}
