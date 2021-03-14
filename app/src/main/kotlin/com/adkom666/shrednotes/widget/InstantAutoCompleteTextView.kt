package com.adkom666.shrednotes.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * The kind of [AppCompatAutoCompleteTextView], which shows the dropdown list immediately after
 * focusing, regardless of the length of the text.
 *
 * @param context the [Context] the view is running in, through which it can access the current
 * theme, resources, etc.
 * @param attrs the attributes of the XML tag that is inflating the view.
 * @param defStyleAttr an attribute in the current theme that contains a reference to a style
 * resource that supplies default values for the view. Can be 0 to not look for defaults.
 */
class InstantAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun enoughToFilter(): Boolean = true

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && adapter != null) {
            performFiltering(text, 0)
            showDropDown()
        }
    }
}
