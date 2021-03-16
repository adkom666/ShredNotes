package com.adkom666.shrednotes.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

/**
 * Hide the soft keyboard from [Fragment].
 */
fun Fragment.hideKeyboard() {
    activity?.hideKeyboard()
}

/**
 * Hide the soft keyboard from [Activity].
 */
fun Activity.hideKeyboard() {
    val view = currentFocus ?: View(this)
    hideKeyboard(view)
}

/**
 * Hide the soft keyboard by [Context] using [View] from the the window that is making the request.
 *
 * @param view [View] from the the window that is making the request.
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
