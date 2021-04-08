package com.adkom666.shrednotes.util.dialog

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.DialogFragment
import com.adkom666.shrednotes.util.hideKeyboard

/**
 * A dialog fragment that, when displayed, ensures that there is no soft keyboard on the screen.
 */
open class KeyboardlessDialogFragment : DialogFragment() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onStart() {
        super.onStart()
        handler.post {
            hideKeyboard()
            activity?.currentFocus?.clearFocus()
        }
    }
}
