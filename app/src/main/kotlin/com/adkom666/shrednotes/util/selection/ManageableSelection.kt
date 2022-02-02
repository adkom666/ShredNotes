package com.adkom666.shrednotes.util.selection

import com.adkom666.shrednotes.common.Id
import kotlin.properties.Delegates.observable

/**
 * Selectable identifiable items along with tools for manipulating the selection and the selection
 * itself.
 *
 * @property itemCount total count of items to select.
 */
class ManageableSelection(private var itemCount: Int = 0) :
    SelectableItems,
    SelectionDashboard,
    Selection {

    /**
     * Information about selected items.
     */
    sealed class State {

        /**
         * At least one item is selected.
         */
        sealed class Active : State() {

            /**
             * Accumulating the identifiers of the selected items.
             *
             * @property selectedItemIdSet set of identifiers of the selected items.
             */
            data class Inclusive(
                val selectedItemIdSet: MutableSet<Id> = mutableSetOf()
            ) : Active()

            /**
             * Accumulating the identifiers of the unselected items.
             *
             * @property unselectedItemIdSet set of identifiers of the unselected items.
             */
            data class Exclusive(
                val unselectedItemIdSet: MutableSet<Id> = mutableSetOf()
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

    private var _state: State by observable(
        initialValue = State.Inactive
    ) { _, old: State, new: State ->
        if (old == State.Inactive && new != State.Inactive ||
            old != State.Inactive && new == State.Inactive
        ) {
            onActivenessChangeListenerList.forEach {
                it.onActivenessChange(new is State.Active)
            }
        }
    }

    private val onActivenessChangeListenerList:
            MutableList<OnActivenessChangeListener> = mutableListOf()

    override val selectedItemCount: Int
        get() = when (val state = _state) {
            is State.Active.Exclusive -> itemCount - state.unselectedItemIdSet.size
            is State.Active.Inclusive -> state.selectedItemIdSet.size
            State.Inactive -> 0
        }

    override val isActive: Boolean
        get() = _state is State.Active

    override fun longClick(itemId: Id, changeSelection: (Boolean) -> Unit) {
        if (_state == State.Inactive) {
            _state = clickWhenActive(State.Active.Inclusive(), itemId, changeSelection)
        }
    }

    override fun click(
        itemId: Id,
        changeSelection: (Boolean) -> Unit,
        clickWhenInactive: () -> Unit
    ) {
        _state = click(_state, itemId, clickWhenInactive, changeSelection)
    }

    override fun isSelected(itemId: Id): Boolean {
        return isSelected(_state, itemId)
    }

    override fun selectAll() {
        val exclusiveState = State.Active.Exclusive()
        _state = if (!isDeactivationNeed(exclusiveState)) {
            exclusiveState
        } else {
            // Nothing to select
            State.Inactive
        }
    }

    override fun deselectAll() {
        if (_state is State.Active) {
            _state = State.Inactive
        }
    }

    override fun addOnActivenessChangeListener(listener: OnActivenessChangeListener) {
        if (onActivenessChangeListenerList.contains(listener).not()) {
            onActivenessChangeListenerList.add(listener)
        }
    }

    override fun removeOnActivenessChangeListener(listener: OnActivenessChangeListener) {
        onActivenessChangeListenerList.remove(listener)
    }

    /**
     * Prepare for selections. Must be called at the beginning of the work.
     *
     * @param itemCount total count of items to select.
     */
    fun init(itemCount: Int) {
        this.itemCount = itemCount
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
        itemId: Id,
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
        itemId: Id,
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

    private fun changeSelectionWhenInclusive(state: State.Active.Inclusive, itemId: Id): Boolean {
        val isSelected = state.selectedItemIdSet.contains(itemId)
        if (isSelected) {
            state.selectedItemIdSet.remove(itemId)
        } else {
            state.selectedItemIdSet.add(itemId)
        }
        return isSelected.not()
    }

    private fun changeSelectionWhenExclusive(state: State.Active.Exclusive, itemId: Id): Boolean {
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

    private fun isSelected(state: State, itemId: Id): Boolean = when (state) {
        is State.Active.Inclusive -> state.selectedItemIdSet.contains(itemId)
        is State.Active.Exclusive -> state.unselectedItemIdSet.contains(itemId).not()
        else -> false
    }
}
