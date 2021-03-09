package com.adkom666.shrednotes.util.selection

import com.adkom666.shrednotes.common.Id

/**
 * Items to select.
 */
interface SelectableItems {

    companion object {
        @Suppress("Reformat")
        val DUMMY = object : SelectableItems {
            override val selectedItemCount: Int = 0
            override fun longClick(itemId: Id, changeSelection: (Boolean) -> Unit) = Unit
            override fun click(
                itemId: Long,
                changeSelection: (Boolean) -> Unit,
                clickWhenInactive: () -> Unit
            ) = Unit
            override fun isSelected(itemId: Long): Boolean = false
        }
    }

    /**
     * Calculation of the count of selected items.
     */
    val selectedItemCount: Int

    /**
     * Represents a long click on a specific item.
     *
     * @param itemId identifier of the item that was clicked on.
     * @param changeSelection callback to execute when selection of the clicked item changes.
     */
    fun longClick(itemId: Id, changeSelection: (Boolean) -> Unit)

    /**
     * Represents a click on a specific item.
     *
     * @param itemId identifier of the item that was clicked on.
     * @param changeSelection callback to execute when selection of the clicked item changes.
     * @param clickWhenInactive callback to execute when there are no selected items.
     */
    fun click(itemId: Id, changeSelection: (Boolean) -> Unit, clickWhenInactive: () -> Unit)

    /**
     * Determine whether a specific item is selected.
     *
     * @param itemId identifier of the target item.
     * @return true if an item with [itemId] is selected.
     */
    fun isSelected(itemId: Id): Boolean
}
