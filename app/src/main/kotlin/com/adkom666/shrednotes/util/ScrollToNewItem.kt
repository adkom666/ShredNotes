package com.adkom666.shrednotes.util

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

/**
 * Observer for watching new item to scroll to it.
 *
 * @property recycler target recycler view.
 */
class ScrollToNewItem(private val recycler: RecyclerView) {

    private companion object {
        private const val SCROLL_DELAY_MILLIS = 666L
    }

    private val adapterDataObserver: RecyclerView.AdapterDataObserver = AdapterDataObserver()
    private var waitForNewItem: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Waiting to scroll to the newly inserted element.
     */
    fun waitForNewItem() {
        Timber.d("Wait for new item")
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
                    recycler.scrollToPosition(positionStart)
                }, SCROLL_DELAY_MILLIS)
            }
        }
    }
}
