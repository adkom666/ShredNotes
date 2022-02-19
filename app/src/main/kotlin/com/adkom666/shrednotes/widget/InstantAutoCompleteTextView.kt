package com.adkom666.shrednotes.widget

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.R
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.lifecycle.Lifecycle
import java.lang.ref.WeakReference

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

    private val activityRef: WeakReference<AppCompatActivity> = WeakReference(getActivity(context))

    override fun enoughToFilter(): Boolean = true

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && adapter != null
            && activityRef.get()?.lifecycle?.currentState == Lifecycle.State.RESUMED
        ) {
            performFiltering(text, 0)
            showDropDown()
        }
    }

    private fun getActivity(initialContext: Context): AppCompatActivity? {
        var context = initialContext
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}
