package com.adkom666.shrednotes.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView.ItemDecoration] to set the top margin for the first item.
 *
 * @property marginTop top margin in pixels.
 */
class FirstItemDecoration(private val marginTop: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemIndex = parent.getChildAdapterPosition(view)
        if (itemIndex == 0) {
            outRect.top = marginTop
        }
    }
}
