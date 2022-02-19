package com.adkom666.shrednotes.util.fab

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Decorator for a floating action button that hides it when one scroll through the [RecyclerView],
 * for which it is set as a [RecyclerView.OnScrollListener].
 *
 * @property fab floating action button to decorate.
 */
class HidingFabOnScrollListener(
    private val fab: FloatingActionButton
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy != 0 && fab.isOrWillBeHidden.not()) {
            fab.hide()
        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState != RecyclerView.SCROLL_STATE_DRAGGING && fab.isOrWillBeShown.not()) {
            fab.show()
        }
    }
}
