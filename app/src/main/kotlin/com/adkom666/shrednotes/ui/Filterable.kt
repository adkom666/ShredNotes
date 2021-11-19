package com.adkom666.shrednotes.ui

typealias OnFilterEnablingChangedListener = () -> Unit

/**
 * Filterable content.
 */
interface Filterable {

    /**
     * Whether the filter is applied to the content.
     */
    val isFilterEnabled: Boolean

    /**
     * Callback to notify whether the filter enabling is changed.
     */
    var onFilterEnablingChangedListener: OnFilterEnablingChangedListener?

    /**
     * Call filter for configuration.
     */
    fun filter()
}
