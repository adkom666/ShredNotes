package com.adkom666.shrednotes.util.dialog

import androidx.fragment.app.DialogFragment
import com.adkom666.shrednotes.util.hideKeyboard

/**
 * A dialog fragment that, when displayed, ensures that there is no soft keyboard on the screen.
 */
open class KeyboardlessDialogFragment : DialogFragment() {

    override fun onStart() {
        super.onStart()
        hideKeyboard()
        activity?.currentFocus?.clearFocus()
    }
}
