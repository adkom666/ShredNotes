package com.adkom666.shrednotes.util.selection

/**
 * Information about the presence of selected items.
 */
interface Selection {

    companion object {
        @Suppress("Reformat")
        val DUMMY = object : Selection {
            override val isActive: Boolean = false
            override fun addOnActivenessChangeListener(listener: OnActivenessChangeListener) = Unit
            override fun removeOnActivenessChangeListener(listener: OnActivenessChangeListener) = Unit
        }
    }

    /**
     * If at least one selected item exists.
     */
    val isActive: Boolean

    /**
     * Addition of the listener which detects when at least one selected item appears or disappears.
     *
     * @param listener listener to add.
     */
    fun addOnActivenessChangeListener(listener: OnActivenessChangeListener)

    /**
     * Removing of the listener which detects when at least one selected item appears or disappears.
     *
     * @param listener listener to remove.
     */
    fun removeOnActivenessChangeListener(listener: OnActivenessChangeListener)
}
