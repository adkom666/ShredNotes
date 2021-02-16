package com.adkom666.shrednotes.util

import kotlin.properties.Delegates

/**
 * Information about the selected identifiable items.
 *
 * @property itemCount total count of items to select.
 */
class Selector(private var itemCount: Int) {

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

    /**
     * Information about the presence of selected items.
     */
    sealed class State {

        /**
         * At least one item is selected.
         */
        sealed class Active : State() {

            /**
             * Accumulating the identifiers of the selected items.
             *
             * @param selectedItemIdSet set of identifiers of the selected items.
             */
            data class Inclusive(
                val selectedItemIdSet: MutableSet<Long> = mutableSetOf()
            ) : Active()

            /**
             * Accumulating the identifiers of the unselected items.
             *
             * @param unselectedItemIdSet set of identifiers of the unselected items.
             */
            data class Exclusive(
                val unselectedItemIdSet: MutableSet<Long> = mutableSetOf()
            ) : Active()
        }

        /**
         * No item is selected.
         */
        object Inactive : State()
    }

    /**
     * Current state.
     */
    val state: State
        get() = _state

    /**
     * Calculation of the count of selected items depending on the current state.
     */
    val selectedItemCount: Int
        get() = when (val state = _state) {
            is State.Active.Exclusive -> itemCount - state.unselectedItemIdSet.size
            is State.Active.Inclusive -> state.selectedItemIdSet.size
            State.Inactive -> 0
        }

    private var _state: State by Delegates.observable(State.Inactive) { _, old: State, new: State ->
        if (old == State.Inactive && new != State.Inactive ||
            old != State.Inactive && new == State.Inactive
        ) {
            onActivenessChangeListenerList.forEach {
                it.onActivenessChange(new is State.Active)
            }
        }
    }

    private val onActivenessChangeListenerList: MutableList<OnActivenessChangeListener> =
        mutableListOf()

    /**
     * Represents a long click on a specific item.
     *
     * @param itemId identifier of the item that was clicked on.
     * @param changeSelection callback to execute when selection of the clicked item changes.
     */
    fun longClick(itemId: Long, changeSelection: (Boolean) -> Unit) {
        if (_state == State.Inactive) {
            _state = clickWhenActive(State.Active.Inclusive(), itemId, changeSelection)
        }
    }

    /**
     * Represents a click on a specific item.
     *
     * @param itemId identifier of the item that was clicked on.
     * @param clickWhenInactive callback to execute when there are no selected items.
     * @param changeSelection callback to execute when selection of the clicked item changes.
     */
    fun click(itemId: Long, clickWhenInactive: () -> Unit, changeSelection: (Boolean) -> Unit) {
        _state = click(_state, itemId, clickWhenInactive, changeSelection)
    }

    /**
     * Select all items if at least one is present.
     */
    fun selectAll() {
        val exclusiveState = State.Active.Exclusive()
        _state = if (!isDeactivationNeed(exclusiveState)) {
            exclusiveState
        } else {
            // Nothing to select
            State.Inactive
        }
    }

    /**
     * Deselect all items.
     */
    fun deselectAll() {
        if (_state is State.Active) {
            _state = State.Inactive
        }
    }

    /**
     * Determine whether a specific item is selected.
     *
     * @param itemId identifier of the target item.
     * @return true if an item with [itemId] is selected.
     */
    fun isSelected(itemId: Long): Boolean {
        return isSelected(_state, itemId)
    }

    /**
     * Addition of the listener which detects when at least one selected item appears or disappears.
     *
     * @param listener listener to add.
     */
    fun addOnActivenessChangeListener(listener: OnActivenessChangeListener) {
        if (onActivenessChangeListenerList.contains(listener).not()) {
            onActivenessChangeListenerList.add(listener)
        }
    }

    /**
     * Removing of the listener which detects when at least one selected item appears or disappears.
     *
     * @param listener listener to remove.
     */
    fun removeOnActivenessChangeListener(listener: OnActivenessChangeListener) {
        onActivenessChangeListenerList.remove(listener)
    }

    /**
     * Skip all the old selections and prepare for the new ones.
     *
     * @param itemCount new total count of items to select.
     */
    fun reset(itemCount: Int) {
        this.itemCount = itemCount
        _state = State.Inactive
    }

    private fun click(
        state: State,
        itemId: Long,
        clickWhenInactive: () -> Unit,
        changeSelection: (Boolean) -> Unit
    ): State = if (state is State.Active) {
        clickWhenActive(state, itemId, changeSelection)
    } else {
        clickWhenInactive()
        state
    }

    private fun clickWhenActive(
        state: State.Active,
        itemId: Long,
        onSelectionChange: (Boolean) -> Unit
    ): State {
        val isSelected = when (state) {
            is State.Active.Inclusive -> changeSelectionWhenInclusive(state, itemId)
            is State.Active.Exclusive -> changeSelectionWhenExclusive(state, itemId)
        }
        onSelectionChange(isSelected)
        return if (isDeactivationNeed(state)) {
            State.Inactive
        } else {
            state
        }
    }

    private fun changeSelectionWhenInclusive(state: State.Active.Inclusive, itemId: Long): Boolean {
        val isSelected = state.selectedItemIdSet.contains(itemId)
        if (isSelected) {
            state.selectedItemIdSet.remove(itemId)
        } else {
            state.selectedItemIdSet.add(itemId)
        }
        return isSelected.not()
    }

    private fun changeSelectionWhenExclusive(state: State.Active.Exclusive, itemId: Long): Boolean {
        val isSelected = state.unselectedItemIdSet.contains(itemId).not()
        if (isSelected) {
            state.unselectedItemIdSet.add(itemId)
        } else {
            state.unselectedItemIdSet.remove(itemId)
        }
        return isSelected.not()
    }

    private fun isDeactivationNeed(state: State): Boolean = when (state) {
        is State.Active.Inclusive -> state.selectedItemIdSet.isEmpty()
        is State.Active.Exclusive -> state.unselectedItemIdSet.count() == itemCount
        else -> false
    }

    private fun isSelected(state: State, itemId: Long): Boolean = when (state) {
        is State.Active.Inclusive -> state.selectedItemIdSet.contains(itemId)
        is State.Active.Exclusive -> state.unselectedItemIdSet.contains(itemId).not()
        else -> false
    }
}
