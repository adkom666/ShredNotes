package com.adkom666.shrednotes.util.selection

/**
 * Tools for manipulating the selection.
 */
interface SelectionDashboard {

    companion object {
        val DUMMY = object : SelectionDashboard {
            override fun selectAll() = Unit
            override fun deselectAll() = Unit
        }
    }

    /**
     * Select all items if at least one is present.
     */
    fun selectAll()

    /**
     * Deselect all items.
     */
    fun deselectAll()
}
