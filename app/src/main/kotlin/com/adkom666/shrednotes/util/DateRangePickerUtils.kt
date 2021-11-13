package com.adkom666.shrednotes.util

import androidx.fragment.app.FragmentManager
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.localTimestampOrNull
import com.google.android.material.datepicker.MaterialDatePicker
import timber.log.Timber

/**
 * Display the date range picker dialog, adding the fragment to the given [fragmentManager].
 * This is a convenience for explicitly creating a transaction, adding the fragment to it with
 * the given [tag], and committing it. This does not add the transaction to the fragment back
 * stack. When the fragment is dismissed, a new transaction will be executed to remove it from
 * the activity.
 *
 * @param fragmentManager the [FragmentManager] this fragment will be added to.
 * @param tag the tag for this fragment, as per [androidx.fragment.app.FragmentTransaction.add].
 * @param dateRangeProvider this function should return the actual date range to initialize the
 * dialog.
 * @param onPositiveButtonClick this callback gets the actual date range after the dialog is closed.
 */
fun showDateRangePicker(
    fragmentManager: FragmentManager,
    tag: String,
    dateRangeProvider: () -> DateRange,
    onPositiveButtonClick: (DateRange) -> Unit
) {
    val builder = MaterialDatePicker.Builder.dateRangePicker()
    builder.setTheme(R.style.AppTheme_MaterialAlertDialog_DatePicker)
    val dateRange = dateRangeProvider()
    Timber.d("Initial date range: dateRange=$dateRange")
    val selection = androidx.core.util.Pair.create(
        dateRange.fromInclusive.localTimestampOrNull(),
        dateRange.toExclusive?.yesterday.localTimestampOrNull()
    )
    builder.setSelection(selection)
    val picker = builder.build()
    picker.addDateListener(onPositiveButtonClick)
    picker.show(fragmentManager, tag)
}

/**
 * Restoring a date range listener if the [fragmentManager] has a dialogue fragment with the [tag].
 *
 * @param fragmentManager the [FragmentManager] this fragment is added to.
 * @param tag the tag for this fragment, as per [androidx.fragment.app.FragmentTransaction.add].
 * @param onPositiveButtonClick this callback gets the actual date range after the dialog is closed.
 */
fun restoreDateRangeListener(
    fragmentManager: FragmentManager,
    tag: String,
    onPositiveButtonClick: (DateRange) -> Unit
) {
    fragmentManager.performIfFoundByTag<MaterialDatePicker<*>>(tag) {
        it.addDateListener(onPositiveButtonClick)
    }
}

private fun MaterialDatePicker<*>.addDateListener(
    onPositiveButtonClick: (DateRange) -> Unit
) {
    addOnPositiveButtonClickListener { selection ->
        if (selection is androidx.core.util.Pair<*, *>) {
            val from = selection.first as? Long
            val to = selection.second as? Long
            val dateFromInclusive = from?.let { Days(it) }
            val dateToInclusive = to?.let { Days(it) }
            val dateRange = DateRange(
                fromInclusive = dateFromInclusive,
                toExclusive = dateToInclusive?.tomorrow
            )
            Timber.d("Date range selected: dateRange=$dateRange")
            onPositiveButtonClick(dateRange)
        }
    }
}
