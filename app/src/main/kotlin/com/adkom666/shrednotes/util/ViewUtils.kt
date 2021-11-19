package com.adkom666.shrednotes.util

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager

private const val CLICKABILITY_DISABLEMENT_MILLIS = 666L

/**
 * Measuring the height of a view after it has been drawn.
 *
 * @return measured height of this view in pixels.
 */
fun View.measureHeight(): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val deviceWidth = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.width()
    } else {
        val size = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(size)
        size.x
    }

    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
        deviceWidth,
        View.MeasureSpec.AT_MOST
    )

    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
        0,
        View.MeasureSpec.UNSPECIFIED
    )

    measure(widthMeasureSpec, heightMeasureSpec)

    return measuredHeight
}

/**
 * After calling this method, there are no consequences for the view of entering text, including the
 * cursor and keyboard.
 */
fun View.ensureNoTextInput() {
    if (hasFocus()) {
        context?.hideKeyboard(windowToken)
        clearFocus()
    }
}

/**
 * Register a callback to be invoked once when this view is clicked. The next click is allowed after
 * a while. This mechanism avoids performing [listener] multiple times. If this view is not
 * clickable, it becomes clickable.
 *
 * @param listener the callback that will run.
 */
fun View.setOnSafeClickListener(listener: View.OnClickListener?) {
    setOnClickListener { view ->
        view.temporarilyDisableClickability()
        listener?.onClick(view)
    }
}

private fun View.temporarilyDisableClickability() {
    if (isClickable) {
        isClickable = false
        postDelayed({
            isClickable = true
        }, CLICKABILITY_DISABLEMENT_MILLIS)
    }
}
