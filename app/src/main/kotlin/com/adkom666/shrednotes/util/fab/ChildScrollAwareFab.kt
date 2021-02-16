package com.adkom666.shrednotes.util.fab

import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * [ChildFab] which disappears when attached [RecyclerView] is scrolling.
 *
 * @param fab see [ChildFab].
 * @param index see [ChildFab].
 * @param parentFabHeight see [ChildFab].
 * @param margin see [ChildFab].
 * @param additionalMargin see [ChildFab].
 */
class ChildScrollAwareFab(
    fab: FloatingActionButton,
    index: Int,
    @Px
    parentFabHeight: Int,
    @Px
    margin: Float,
    @Px
    additionalMargin: Float
) {
    private val fabAsChild: ChildFab = ChildFab(
        fab,
        index,
        parentFabHeight,
        margin,
        additionalMargin
    )

    private val fabAsScrollAware: ScrollAwareFab = ScrollAwareFab(fab)

    /**
     * Perform [ChildFab.setupAsVisible] and attach FAB to [recyclerView] by
     * [ScrollAwareFab.attachTo].
     *
     * @param recyclerView [RecyclerView] to attach.
     */
    fun setupAsVisibleAndAttachTo(recyclerView: RecyclerView) {
        fabAsChild.setupAsVisible()
        fabAsScrollAware.attachTo(recyclerView)
    }

    /**
     * Perform [ChildFab.setupAsHidden].
     */
    fun setupAsHidden() {
        fabAsChild.setupAsHidden()
    }

    /**
     * Perform [ChildFab.showAnimating] and attach FAB to [recyclerView] by
     * [ScrollAwareFab.attachTo].
     *
     * @param recyclerView [RecyclerView] to attach.
     */
    fun showAnimatingAndAttachTo(recyclerView: RecyclerView) {
        fabAsChild.showAnimating()
        fabAsScrollAware.attachTo(recyclerView)
    }

    /**
     * Detach FAB from [recyclerView] by [ScrollAwareFab.detachFrom] and perform
     * [ChildFab.hideAnimating].
     *
     * @param recyclerView [RecyclerView] to detach.
     */
    fun hideAnimatingAndDetachFrom(recyclerView: RecyclerView) {
        fabAsScrollAware.detachFrom(recyclerView)
        fabAsChild.hideAnimating()
    }

    /**
     * Detach FAB from [recyclerView] by [ScrollAwareFab.detachFrom].
     *
     * @param recyclerView [RecyclerView] to detach.
     */
    fun detachFrom(recyclerView: RecyclerView) {
        fabAsScrollAware.detachFrom(recyclerView)
    }
}
