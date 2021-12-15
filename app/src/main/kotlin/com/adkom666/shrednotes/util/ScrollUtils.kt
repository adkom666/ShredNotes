package com.adkom666.shrednotes.util

import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/**
 * Starts a smooth scroll using the linear interpolation until the target [position] becomes a child
 * of the [RecyclerView] and then uses a decelerate interpolation to slowly approach to target
 * [position].
 *
 * @param position target position for scrolling.
 */
fun RecyclerView.startLinearSmoothScrollToPosition(position: Int) {
    val smoothScroller = LinearSmoothScroller(context)
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}
