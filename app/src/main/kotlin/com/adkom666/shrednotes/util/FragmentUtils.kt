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
