package com.adkom666.shrednotes.util.fab

import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.util.Selector
import com.adkom666.shrednotes.util.measureHeight
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Decorator for three floating action buttons above [recycler]. One of the FABs is the parent,
 * and the other two are child ones. Child FABs are hidden behind the parent one and appear from
 * under it if there is at least one selected item in the [recycler]. When one scroll through the
 * [recycler], all the FABs disappear.
 *
 * @property selector information about the selected items in [recycler].
 * @property recycler [RecyclerView] to decorate.
 * @param parentFab parent [FloatingActionButton] to decorate.
 * @param bottomChildFab bottom child [FloatingActionButton] to decorate.
 * @param topChildFab top child [FloatingActionButton] to decorate.
 * @property parentFabConstructiveImageRes reference to image for the parent FAB when no item in
 * [recycler] is selected.
 * @property parentFabDestructiveImageRes reference to image for the parent FAB when at least one
 * item in [recycler] is selected.
 * @property childFabMargin the margin between the two child FABs in pixels.
 * @property bottomChildFabMargin the margin between the bottom child FAB and the parent one in
 * pixels.
 */
class FabDashboard(
    private val selector: Selector,
    private val recycler: RecyclerView,
    parentFab: FloatingActionButton,
    bottomChildFab: FloatingActionButton,
    topChildFab: FloatingActionButton,
    @DrawableRes
    private val parentFabConstructiveImageRes: Int = android.R.drawable.ic_menu_add,
    @DrawableRes
    private val parentFabDestructiveImageRes: Int = android.R.drawable.ic_menu_delete,
    @Px
    private val childFabMargin: Float = 0f,
    @Px
    private val bottomChildFabMargin: Float = 0f
) {
    private val parentFabDecorator: VariableScrollAwareFab = VariableScrollAwareFab(parentFab)
    private var bottomChildFabDecorator: ChildScrollAwareFab? = null
    private var topChildFabDecorator: ChildScrollAwareFab? = null

    private val onSelectorActivenessChangeListener = object : Selector.OnActivenessChangeListener {
        override fun onActivenessChange(isActive: Boolean) = changeDecorators(isActive)
    }

    init {
        parentFabDecorator.attachTo(recycler)

        val parentFabHeight = parentFab.measureHeight()
        val additionalMargin = bottomChildFabMargin - childFabMargin

        bottomChildFabDecorator = ChildScrollAwareFab(
            fab = bottomChildFab,
            index = 0,
            parentFabHeight = parentFabHeight,
            margin = childFabMargin,
            additionalMargin = additionalMargin
        )

        topChildFabDecorator = ChildScrollAwareFab(
            fab = topChildFab,
            index = 1,
            parentFabHeight = parentFabHeight,
            margin = childFabMargin,
            additionalMargin = additionalMargin
        )

        setupDecorators(selector.state is Selector.State.Active)

        selector.addOnActivenessChangeListener(onSelectorActivenessChangeListener)
    }

    /**
     * Call it before dispose decorated FABs and recyclerView.
     */
    fun dispose() {
        parentFabDecorator.detachFrom(recycler)
        bottomChildFabDecorator?.detachFrom(recycler)
        topChildFabDecorator?.detachFrom(recycler)

        selector.removeOnActivenessChangeListener(onSelectorActivenessChangeListener)
    }

    private fun setupDecorators(isSelectorActive: Boolean) {
        if (isSelectorActive) {
            parentFabDecorator.setImageResource(parentFabDestructiveImageRes)
            bottomChildFabDecorator?.setupAsVisibleAndAttachTo(recycler)
            topChildFabDecorator?.setupAsVisibleAndAttachTo(recycler)
        } else {
            parentFabDecorator.setImageResource(parentFabConstructiveImageRes)
            bottomChildFabDecorator?.setupAsHidden()
            topChildFabDecorator?.setupAsHidden()
        }
    }

    private fun changeDecorators(isSelectorActive: Boolean) {
        if (isSelectorActive) {
            parentFabDecorator.changeImageResourceAnimating(parentFabDestructiveImageRes)
            bottomChildFabDecorator?.showAnimatingAndAttachTo(recycler)
            topChildFabDecorator?.showAnimatingAndAttachTo(recycler)
        } else {
            parentFabDecorator.changeImageResourceAnimating(parentFabConstructiveImageRes)
            bottomChildFabDecorator?.hideAnimatingAndDetachFrom(recycler)
            topChildFabDecorator?.hideAnimatingAndDetachFrom(recycler)
        }
    }
}
