package com.adkom666.shrednotes.util.selection

/**
 * Listener to detect when at least one selected item appears or disappears.
 */
interface OnActivenessChangeListener {

    /**
     * Callback to execute when selection appears or disappears.
     *
     * @param isActive true if at least one item is selected.
     */
    fun onActivenessChange(isActive: Boolean)
}
