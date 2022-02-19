package com.adkom666.shrednotes.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Show the view with [message] for a short time.
 *
 * @param message the text to show; can be formatted text.
 * @param isShort should the duration of the toast be short, not long.
 */
fun Context.toast(message: String, isShort: Boolean = true) {
    Toast.makeText(this, message, isShort.getDuration()).show()
}

/**
 * Show the view with text from the resource for a short time.
 *
 * @param resId the resource id of the string resource to use; can be formatted text.
 * @param isShort should the duration of the toast be short, not long.
 */
fun Context.toast(@StringRes resId: Int, isShort: Boolean = true) {
    Toast.makeText(this, resId, isShort.getDuration()).show()
}

/**
 * Show the view with [message] for a short time from the [Fragment].
 *
 * @param message the text to show; can be formatted text.
 * @param isShort should the duration of the toast be short, not long.
 */
fun Fragment.toast(message: String, isShort: Boolean = true) {
    context?.toast(message, isShort)
}

/**
 * Show the view with text from the resource for a short time from the [Fragment].
 *
 * @param resId the resource id of the string resource to use; can be formatted text.
 * @param isShort should the duration of the toast be short, not long.
 */
fun Fragment.toast(@StringRes resId: Int, isShort: Boolean = true) {
    context?.toast(resId, isShort)
}

private fun Boolean.getDuration() = if (this) {
    Toast.LENGTH_SHORT
} else {
    Toast.LENGTH_LONG
}
