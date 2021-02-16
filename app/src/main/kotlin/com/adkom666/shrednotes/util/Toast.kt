package com.adkom666.shrednotes.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Show the view with [message] for a short time.
 *
 * @param message the text to show; can be formatted text.
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show the view with text from the resource for a short time.
 *
 * @param resId the resource id of the string resource to use; can be formatted text.
 */
fun Context.toast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

/**
 * Show the view with [message] for a short time from the [Fragment].
 *
 * @param message the text to show; can be formatted text.
 */
fun Fragment.toast(message: String) {
    context?.toast(message)
}

/**
 * Show the view with text from the resource for a short time from the [Fragment].
 *
 * @param resId the resource id of the string resource to use; can be formatted text.
 */
fun Fragment.toast(@StringRes resId: Int) {
    context?.toast(resId)
}
