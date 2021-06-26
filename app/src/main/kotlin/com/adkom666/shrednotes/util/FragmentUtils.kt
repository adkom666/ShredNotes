package com.adkom666.shrednotes.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment

/**
 * Request for the currently displayed fragment.
 *
 * @return currently displayed fragment or null.
 */
fun FragmentManager.getCurrentlyDisplayedFragment(): Fragment? {
    fragments.forEach {
        if (it.isVisible) {
            return it
        }
    }
    return null
}

/**
 * Performs [block] if the fragment with the [tag] is [ConfirmationDialogFragment].
 *
 * @param tag tag of the target fragment.
 * @param block actions to perform if the fragment with the [tag] is [ConfirmationDialogFragment].
 */
fun FragmentManager.performIfConfirmationFoundByTag(
    tag: String,
    block: (ConfirmationDialogFragment) -> Unit
) = performIfFoundByTag(tag, block)

/**
 * Performs [block] if the fragment with the [tag] is [T].
 *
 * @param tag tag of the target fragment.
 * @param block actions to perform if the fragment with the [tag] is [T].
 */
inline fun <reified T> FragmentManager.performIfFoundByTag(
    tag: String,
    block: (T) -> Unit
) {
    val fragment = findFragmentByTag(tag) as? T
    fragment?.let { block(it) }
}
