package com.adkom666.shrednotes.util.fab

import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * [ScrollAwareFab], which allows you to control its image.
 *
 * @property fab floating action button to decorate.
 */
class VariableScrollAwareFab(private val fab: FloatingActionButton) : ScrollAwareFab(fab) {

    private companion object {
        private const val ANIMATION_SCALE = 0.8f
        private const val ANIMATION_ROTATION = 360f
    }

    /**
     * Set the initial image before working with FAB.
     *
     * @param resId image resource identifier.
     */
    fun setImageResource(@DrawableRes resId: Int) {
        fab.setImageResource(resId)
        // Due to bug https://issuetracker.google.com/issues/117476935
        fab.hide()
        fab.show()
    }

    /**
     * Change the image by working with FAB.
     *
     * @param resId image resource identifier.
     */
    fun changeImageResourceAnimating(@DrawableRes resId: Int) {
        fab.clearAnimation()
        fab.rotation = 0f
        ViewCompat.animate(fab)
            .setDuration(FAB_ANIMATION_DURATION_MILLIS)
            .rotation(ANIMATION_ROTATION)
            .scaleX(ANIMATION_SCALE)
            .scaleY(ANIMATION_SCALE)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View?) {
                    super.onAnimationEnd(view)
                    setImageResource(resId)
                    fab.rotation = 0f
                }
            })
            .start()
    }
}
