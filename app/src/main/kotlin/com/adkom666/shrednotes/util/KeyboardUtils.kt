package com.adkom666.shrednotes.util

import android.app.Activity
import android.content.Context
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

/**
 * Hide the soft keyboard from [Fragment].
 */
fun Fragment.hideKeyboard() {
    val targetView = activity?.currentFocus ?: view ?: context?.let { View(it) }
    targetView?.let {
        context?.hideKeyboard(it)
    }
}

/**
 * Hide the soft keyboard from [Activity].
 */
@Suppress("unused")
fun Activity.hideKeyboard() {
    val view = currentFocus ?: View(this)
    hideKeyboard(view)
}

/**
 * Hide the soft keyboard by [Context] using [View] from the window that is making the request.
 *
 * @param view [View] from the window that is making the request.
 */
fun Context.hideKeyboard(view: View) = view.windowToken?.let { hideKeyboard(it) }

/**
 * Hide the soft keyboard by [Context] using [windowToken] of the window that is making the request.
 *
 * @param windowToken the token of the window that is making the request.
 */
fun Context.hideKeyboard(windowToken: IBinder) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}
