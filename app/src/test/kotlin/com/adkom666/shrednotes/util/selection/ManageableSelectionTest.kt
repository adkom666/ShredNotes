package com.adkom666.shrednotes.util.selection

import junit.framework.TestCase
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test

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

    @Test
    fun testSelection() {
        val changeSelection = { isSelected: Boolean ->
            Assert.assertTrue(isSelected)
        }
        selection.longClick(ID_1, changeSelection)
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        val clickWhenInactive = { Assert.assertFalse(true) }
        selection.click(ID_2, changeSelection, clickWhenInactive)
        when (val selectionState = selection.state) {
            is ManageableSelection.State.Active.Inclusive -> {
                Assert.assertTrue(selectionState.selectedItemIdSet.containsAll(ALL_ID))
                Assert.assertEquals(2, selectionState.selectedItemIdSet.size)
            }
            else -> Assert.assertFalse(true)
        }
    }

    @Test
    fun testSelectedItemCountOnInclusiveSelection() {
        Assert.assertEquals(0, selection.selectedItemCount)
        selection.longClick(ID_1) {}
        Assert.assertEquals(1, selection.selectedItemCount)
        selection.longClick(ID_2) {}
        Assert.assertEquals(1, selection.selectedItemCount)
        selection.click(ID_2, {}, {})
        Assert.assertEquals(2, selection.selectedItemCount)
    }

    @Test
    fun testSelectedItemCountOnExclusiveSelection() {
        Assert.assertEquals(0, selection.selectedItemCount)
        selection.selectAll()
        Assert.assertEquals(ITEM_COUNT, selection.selectedItemCount)
        selection.longClick(ID_1) {}
        Assert.assertEquals(ITEM_COUNT, selection.selectedItemCount)
        selection.click(ID_1, {}, {})
        Assert.assertEquals(ITEM_COUNT - 1, selection.selectedItemCount)
        selection.click(ID_2, {}, {})
        Assert.assertEquals(ITEM_COUNT - 2, selection.selectedItemCount)
    }

    @Test
    fun testInactiveAfterDeselection() {
        selection.longClick(ID_1) {}
        selection.click(ID_2, {}, {})
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.click(ID_1, {}, {})
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.click(ID_2, {}, {})
        Assert.assertEquals(selection.state, ManageableSelection.State.Inactive)
    }

    @Test
    fun testDeselection() {
        selection.longClick(ID_1) { isSelected ->
            Assert.assertTrue(isSelected)
        }
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Inclusive::class.java)
        )
        selection.selectAll()
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active.Exclusive::class.java)
        )
        val clickWhenInactive = { Assert.assertFalse(true) }
        val changeSelection = { isSelected: Boolean ->
            Assert.assertFalse(isSelected)
        }
        selection.click(ID_1, changeSelection, clickWhenInactive)
        selection.click(ID_2, changeSelection, clickWhenInactive)
        when (val selectionState = selection.state) {
            is ManageableSelection.State.Active.Exclusive -> {
                Assert.assertTrue(selectionState.unselectedItemIdSet.containsAll(ALL_ID))
                Assert.assertEquals(2, selectionState.unselectedItemIdSet.size)
            }
            else -> Assert.assertFalse(true)
        }
    }

    @Test
    fun testReset() {
        Assert.assertEquals(selection.state, ManageableSelection.State.Inactive)
        selection.longClick(ID_1) {}
        Assert.assertThat(
            selection.state,
            CoreMatchers.instanceOf(ManageableSelection.State.Active::class.java)
        )
        selection.selectAll()
        selection.click(ID_1, {}, {})
        selection.click(ID_2, {}, {})
        selection.reset(ITEM_COUNT)
        Assert.assertEquals(selection.state, ManageableSelection.State.Inactive)
        selection.click(ID_1, {}) { Assert.assertTrue(true) }
    }

    @Test
    fun testSelectedItemCountAfterReset() {
        selection.longClick(ID_1) {}
        selection.click(ID_2, {}, {})
        selection.reset(ITEM_COUNT)
        Assert.assertEquals(0, selection.selectedItemCount)
    }

    @Test
    fun testOnActivenessChangeListener() {
        val listener = object : OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                Assert.assertTrue(isActive)
            }
        }
        selection.addOnActivenessChangeListener(listener)
        selection.longClick(ID_1) {}
    }
}
