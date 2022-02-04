package com.adkom666.shrednotes.util

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

/**
 * Observer for watching new item to scroll to it.
 *
 * @property recycler target recycler view.
 * @property lifecycle [Lifecycle] of the [recycler]'s owner.
 * @property scrollDelayMillis delay before scroll in milliseconds.
 */
class ScrollToNewItem(
    private val recycler: RecyclerView,
    private val lifecycle: Lifecycle,
    private val scrollDelayMillis: Long
) {
    private val adapterDataObserver: RecyclerView.AdapterDataObserver = AdapterDataObserver()
    private var waitForNewItem: Boolean = false

    /**
     * Waiting to scroll to the newly inserted element.
     */
    fun waitForNewItem() {
        Timber.d("Wait for new item (now waitForNewItem=$waitForNewItem)")
        if (!waitForNewItem) {
            recycler.adapter?.registerAdapterDataObserver(adapterDataObserver)
            waitForNewItem = true
        }
    }

    /**
     * Skip waiting to scroll to the newly inserted element.
     */
    fun dontWaitForNewItem() {
        Timber.d("Don't wait for new item")
        if (waitForNewItem) {
            recycler.adapter?.unregisterAdapterDataObserver(adapterDataObserver)
            waitForNewItem = false
        }
    }

    private inner class AdapterDataObserver : RecyclerView.AdapterDataObserver() {

        private val handler = Handler(Looper.getMainLooper())

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            Timber.d(
                """On item range inserted:
                    |positionStart=$positionStart,
                    |itemCount=$itemCount""".trimMargin()
            )
            super.onItemRangeInserted(positionStart, itemCount)
            if (itemCount == 1) {
                Timber.d("New item waited")
                recycler.adapter?.unregisterAdapterDataObserver(this)
                waitForNewItem = false
                handler.postDelayed({
                    Timber.d("Scroll to position: position=$positionStart")
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        recycler.startLinearSmoothScrollToPosition(positionStart)
                    }
                }, scrollDelayMillis)
            }
        }
    }
}
