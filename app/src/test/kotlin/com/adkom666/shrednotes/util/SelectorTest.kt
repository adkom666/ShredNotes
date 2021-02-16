package com.adkom666.shrednotes.util

import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SelectorTest {

    private companion object {
        private const val ITEM_COUNT = 666
        private const val ID_1 = 14666L
        private const val ID_2 = 14667L
        private val ALL_ID = listOf(ID_1, ID_2)
    }

    private val selector: Selector
        get() = requireNotNull(_selector)

    private var _selector: Selector? = null

    @Before
    fun setUp() {
        _selector = Selector(ITEM_COUNT)
    }

    @Test
    fun testSelection() {
        val changeSelection = { isSelected: Boolean ->
            assertTrue(isSelected)
        }
        selector.longClick(ID_1, changeSelection)
        assertThat(selector.state, instanceOf(Selector.State.Active.Inclusive::class.java))
        val clickWhenInactive = { assertFalse(true) }
        selector.click(ID_2, clickWhenInactive, changeSelection)
        when (val selectorState = selector.state) {
            is Selector.State.Active.Inclusive -> {
                assertTrue(selectorState.selectedItemIdSet.containsAll(ALL_ID))
                assertEquals(2, selectorState.selectedItemIdSet.size)
            }
            else -> assertFalse(true)
        }
    }

    @Test
    fun testSelectedItemCountOnInclusiveSelection() {
        assertEquals(0, selector.selectedItemCount)
        selector.longClick(ID_1) {}
        assertEquals(1, selector.selectedItemCount)
        selector.longClick(ID_2) {}
        assertEquals(1, selector.selectedItemCount)
        selector.click(ID_2, {}, {})
        assertEquals(2, selector.selectedItemCount)
    }

    @Test
    fun testSelectedItemCountOnExclusiveSelection() {
        assertEquals(0, selector.selectedItemCount)
        selector.selectAll()
        assertEquals(ITEM_COUNT, selector.selectedItemCount)
        selector.longClick(ID_1) {}
        assertEquals(ITEM_COUNT, selector.selectedItemCount)
        selector.click(ID_1, {}, {})
        assertEquals(ITEM_COUNT - 1, selector.selectedItemCount)
        selector.click(ID_2, {}, {})
        assertEquals(ITEM_COUNT - 2, selector.selectedItemCount)
    }

    @Test
    fun testInactiveAfterDeselection() {
        selector.longClick(ID_1) {}
        selector.click(ID_2, {}, {})
        assertThat(selector.state, instanceOf(Selector.State.Active.Inclusive::class.java))
        selector.click(ID_1, {}, {})
        assertThat(selector.state, instanceOf(Selector.State.Active.Inclusive::class.java))
        selector.click(ID_2, {}, {})
        assertEquals(selector.state, Selector.State.Inactive)
    }

    @Test
    fun testDeselection() {
        selector.longClick(ID_1) { isSelected ->
            assertTrue(isSelected)
        }
        assertThat(selector.state, instanceOf(Selector.State.Active.Inclusive::class.java))
        selector.selectAll()
        assertThat(selector.state, instanceOf(Selector.State.Active.Exclusive::class.java))
        val clickWhenInactive = { assertFalse(true) }
        val changeSelection = { isSelected: Boolean ->
            assertFalse(isSelected)
        }
        selector.click(ID_1, clickWhenInactive, changeSelection)
        selector.click(ID_2, clickWhenInactive, changeSelection)
        when (val selectorState = selector.state) {
            is Selector.State.Active.Exclusive -> {
                assertTrue(selectorState.unselectedItemIdSet.containsAll(ALL_ID))
                assertEquals(2, selectorState.unselectedItemIdSet.size)
            }
            else -> assertFalse(true)
        }
    }

    @Test
    fun testReset() {
        assertEquals(selector.state, Selector.State.Inactive)
        selector.longClick(ID_1) {}
        assertThat(selector.state, instanceOf(Selector.State.Active::class.java))
        selector.selectAll()
        selector.click(ID_1, {}, {})
        selector.click(ID_2, {}, {})
        selector.reset(ITEM_COUNT)
        assertEquals(selector.state, Selector.State.Inactive)
        selector.click(ID_1, { assertTrue(true) }, {})
    }

    @Test
    fun testSelectedItemCountAfterReset() {
        selector.longClick(ID_1) {}
        selector.click(ID_2, {}, {})
        selector.reset(ITEM_COUNT)
        assertEquals(0, selector.selectedItemCount)
    }

    @Test
    fun testOnActivenessChangeListener() {
        val listener = object : Selector.OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                assertTrue(isActive)
            }
        }
        selector.addOnActivenessChangeListener(listener)
        selector.longClick(ID_1) {}
    }
}
