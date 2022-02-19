package com.adkom666.shrednotes.util

import android.os.Handler
import android.os.Looper
import android.view.MenuItem

private const val DISABLEMENT_MILLIS = 666L

/**
 * Disable menu item for a while, for example, to avoid multi clicks.
 */
fun MenuItem.temporarilyDisable() {
    if (isEnabled) {
        isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            isEnabled = true
        }, DISABLEMENT_MILLIS)
    }
}
