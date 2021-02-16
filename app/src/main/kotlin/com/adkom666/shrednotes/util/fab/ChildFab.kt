package com.adkom666.shrednotes.util.fab

import android.view.View
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import com.adkom666.shrednotes.util.measureHeight
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Decorator for a floating action button that allows it to appear from under the parent one and
 * hide under it.
 *
 * @property fab floating action button to decorate.
 * @param index starts with 0, which corresponds to the FAB closest to the parent FAB. The index of
 * the next FAB is 1, etc.
 * @param parentFabHeight height of the parent FAB in pixels.
 * @param margin the margin between the two FABs in pixels.
 * @param additionalMargin additional margin in the direction from the parent FAB in pixels.
 */
class ChildFab(
    private val fab: FloatingActionButton,
    index: Int,
    @Px
    parentFabHeight: Int,
    @Px
    margin: Float,
    @Px
    additionalMargin: Float
) {
    @Px
    private val hiddenStateTranslation: Float = calcHiddenStateTranslation(
        index,
        fab.measureHeight(),
        parentFabHeight,
        margin,
        additionalMargin
    )

    /**
     * Setup the FAB above the parent one.
     */
    fun setupAsVisible() {
        setup(hidden = false)
    }

    /**
     * Setup the FAB behind the parent one.
     */
    fun setupAsHidden() {
        setup(hidden = true)
    }

    /**
     * Raise the FAB above the parent one with animation.
     */
    fun showAnimating() {
        fab.clearAnimation()
        fab.translationY = hiddenStateTranslation
        fab.show()
        ViewCompat.animate(fab)
            .setDuration(FAB_ANIMATION_DURATION_MILLIS)
            .translationY(0f)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View?) {
                    super.onAnimationEnd(view)
                    fab.translationY = 0f
                    fab.show()
                }
            })
            .start()
    }

    /**
     * Hide the FAB behind the parent one with animation.
     */
    fun hideAnimating() {
        fab.clearAnimation()
        fab.translationY = 0f
        ViewCompat.animate(fab)
            .setDuration(FAB_ANIMATION_DURATION_MILLIS)
            .translationY(hiddenStateTranslation)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View?) {
                    super.onAnimationEnd(view)
                    fab.translationY = hiddenStateTranslation
                    fab.hide()
                }
            })
            .start()
    }

    private fun setup(hidden: Boolean) {
        fab.clearAnimation()
        if (hidden) {
            fab.translationY = hiddenStateTranslation
            fab.hide()
        } else {
            fab.translationY = 0f
            fab.show()
        }
    }

    @Px
    private fun calcHiddenStateTranslation(
        index: Int, // Bottom child index is 0
        @Px
        fabHeight: Int,
        @Px
        parentFabHeight: Int,
        @Px
        margin: Float,
        @Px
        additionalMargin: Float
    ): Float {
        return (index + 1) * margin +
                (index + 0.5f) * fabHeight +
                0.5f * parentFabHeight +
                additionalMargin
    }
}
