package com.adkom666.shrednotes.util

import android.widget.EditText

/**
 * Sets the cursor at the end of the text.
 */
fun EditText.forwardCursor() = setSelection(text.length)

/**
 * Parsing of the text as a signed decimal integer.
 *
 * @return the integer value represented by the argument in decimal or null if if the text does not
 * contain a parsable integer.
 */
fun EditText.numberOrNull(): Int? = text?.toString()?.let {
    if (it.isNotBlank()) it.numberOrNull() else null
}
