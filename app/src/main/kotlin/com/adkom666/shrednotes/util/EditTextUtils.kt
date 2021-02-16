package com.adkom666.shrednotes.util

import android.widget.EditText

/**
 * Sets the cursor at the end of the text.
 */
fun EditText.forwardCursor() = setSelection(text.length)
