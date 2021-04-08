package com.adkom666.shrednotes.util

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager

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
