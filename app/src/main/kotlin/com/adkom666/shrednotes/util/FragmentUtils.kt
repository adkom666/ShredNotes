package com.adkom666.shrednotes.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

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
) = performIfFoundByTag(tag) { fragment ->
    val confirmation = fragment as? ConfirmationDialogFragment
    confirmation?.let { block(it) }
}

private fun FragmentManager.performIfFoundByTag(
    tag: String,
    block: (Fragment) -> Unit
) {
    val fragment = findFragmentByTag(tag)
    fragment?.let { block(it) }
}
