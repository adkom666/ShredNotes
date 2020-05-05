package com.adkom666.shrednotes.ui

/**
 * Filterable content.
 */
interface Filterable {

    /**
     * Whether the filter is applied to the content.
     */
    val isFilterEnabled: Boolean

    /**
     * Call filter for configuration.
     *
     * @param onFilter callback to notify whether the filter is enabled.
     */
    fun filter(onFilter: (filterEnabled: Boolean) -> Unit)
}
