package com.adkom666.shrednotes.util.selection

import junit.framework.TestCase
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat

class ManageableSelectionTest : TestCase() {

    private companion object {
        private const val ITEM_COUNT = 666
        private const val ID_1 = 14666L
        private const val ID_2 = 14667L
        private val ALL_ID = listOf(ID_1, ID_2)
    }

    private val selection: ManageableSelection
        get() = requireNotNull(_selection)

    private var _selection: ManageableSelection? = null

    public override fun setUp() {
        super.setUp()
        _selection = ManageableSelection(ITEM_COUNT)
    }

    fun testSelection() {
        val changeSelection = { isSelected: Boolean ->
            assertTrue(isSelected)
        }
        selection.longClick(ID_1, changeSelection)
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        val clickWhenInactive = { assertFalse(true) }
        selection.click(ID_2, changeSelection, clickWhenInactive)
        when (val selectionState = selection.state) {
            is ManageableSelection.State.Active.Inclusive -> {
                assertTrue(selectionState.selectedItemIdSet.containsAll(ALL_ID))
                assertEquals(2, selectionState.selectedItemIdSet.size)
            }
            else -> assertFalse(true)
        }
    }

    fun testSelectedItemCountOnInclusiveSelection() {
        assertEquals(0, selection.selectedItemCount)
        selection.longClick(ID_1) {}
        assertEquals(1, selection.selectedItemCount)
        selection.longClick(ID_2) {}
        assertEquals(1, selection.selectedItemCount)
        selection.click(ID_2, {}, {})
        assertEquals(2, selection.selectedItemCount)
    }

    fun testSelectedItemCountOnExclusiveSelection() {
        assertEquals(0, selection.selectedItemCount)
        selection.selectAll()
        assertEquals(ITEM_COUNT, selection.selectedItemCount)
        selection.longClick(ID_1) {}
        assertEquals(ITEM_COUNT, selection.selectedItemCount)
        selection.click(ID_1, {}, {})
        assertEquals(ITEM_COUNT - 1, selection.selectedItemCount)
        selection.click(ID_2, {}, {})
        assertEquals(ITEM_COUNT - 2, selection.selectedItemCount)
    }

    fun testInactiveAfterDeselection() {
        selection.longClick(ID_1) {}
        selection.click(ID_2, {}, {})
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.click(ID_1, {}, {})
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.click(ID_2, {}, {})
        assertEquals(selection.state, ManageableSelection.State.Inactive)
    }

    fun testDeselection() {
        selection.longClick(ID_1) { isSelected ->
            assertTrue(isSelected)
        }
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.selectAll()
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Exclusive::class.java)
        )
        val clickWhenInactive = { assertFalse(true) }
        val changeSelection = { isSelected: Boolean ->
            assertFalse(isSelected)
        }
        selection.click(ID_1, changeSelection, clickWhenInactive)
        selection.click(ID_2, changeSelection, clickWhenInactive)
        when (val selectionState = selection.state) {
            is ManageableSelection.State.Active.Exclusive -> {
                assertTrue(selectionState.unselectedItemIdSet.containsAll(ALL_ID))
                assertEquals(2, selectionState.unselectedItemIdSet.size)
            }
            else -> assertFalse(true)
        }
    }

    fun testReset() {
        assertEquals(selection.state, ManageableSelection.State.Inactive)
        selection.longClick(ID_1) {}
        assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active::class.java)
        )
        selection.selectAll()
        selection.click(ID_1, {}, {})
        selection.click(ID_2, {}, {})
        selection.reset(ITEM_COUNT)
        assertEquals(selection.state, ManageableSelection.State.Inactive)
        selection.click(ID_1, {}) { assertTrue(true) }
    }

    fun testSelectedItemCountAfterReset() {
        selection.longClick(ID_1) {}
        selection.click(ID_2, {}, {})
        selection.reset(ITEM_COUNT)
        assertEquals(0, selection.selectedItemCount)
    }

    fun testOnActivenessChangeListener() {
        val listener = object : OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                assertTrue(isActive)
            }
        }
        selection.addOnActivenessChangeListener(listener)
        selection.longClick(ID_1) {}
    }
}
