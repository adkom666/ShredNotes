package com.adkom666.shrednotes.util.fab

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Decorator for a floating action button that allows it to disappear when attached [RecyclerView]
 * is scrolling.
 *
 * @param fab floating action button to decorate.
 */
open class ScrollAwareFab(fab: FloatingActionButton) {

    private val hidingFabOnScrollListener: RecyclerView.OnScrollListener =
        HidingFabOnScrollListener(fab)

    /**
     * Attach the FAB to the [recyclerView] so that the FAB disappears when [recyclerView] is
     * scrolling.
     *
     * @param recyclerView [RecyclerView] to attach.
     */
    fun attachTo(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(hidingFabOnScrollListener)
    }

    /**
     * Detach the FAB from the [recyclerView] so that the FAB does not disappear when one scroll
     * through the [recyclerView].
     *
     * @param recyclerView [RecyclerView] to detach.
     */
    fun detachFrom(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(hidingFabOnScrollListener)
    }
}
