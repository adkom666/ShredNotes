package com.adkom666.shrednotes.ui.notes

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.databinding.DialogFilterNotesBinding
import com.adkom666.shrednotes.util.dialog.KeyboardlessDialogFragment
import com.adkom666.shrednotes.util.ensureNoTextInput
import com.adkom666.shrednotes.util.numberOrNull
import com.adkom666.shrednotes.util.performIfFoundByTag
import com.adkom666.shrednotes.util.time.Days
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber
import java.text.DateFormat
import java.util.Locale

typealias FilterListener = (filter: NoteFilter) -> Unit

/**
 * Dialog fragment for configuring the notes filter.
 */
class NoteFilterDialogFragment : KeyboardlessDialogFragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @param initialFilter the filter whose parameters will initialize the fragment.
         * @param isFilterEnabled whether the [initialFilter] parameters are currently applied.
         * @return new instance as [NoteFilterDialogFragment].
         */
        fun newInstance(
            initialFilter: NoteFilter,
            isFilterEnabled: Boolean
        ): NoteFilterDialogFragment {
            val fragment = NoteFilterDialogFragment()
            val arguments = Bundle()
            arguments.putParcelable(ARG_INITIAL_FILTER, initialFilter)
            arguments.putBoolean(ARG_IS_FILTER_ENABLED, isFilterEnabled)
            fragment.arguments = arguments
            return fragment
        }

        private const val ARG_INITIAL_FILTER =
            "${BuildConfig.APPLICATION_ID}.args.note_filter.initial_filter"

        private const val ARG_IS_FILTER_ENABLED =
            "${BuildConfig.APPLICATION_ID}.args.note_filter.is_filter_enabled"

        private const val KEY_DATE_FROM_INCLUSIVE =
            "${BuildConfig.APPLICATION_ID}.keys.note_filter.date_from_inclusive"

        private const val KEY_DATE_TO_INCLUSIVE =
            "${BuildConfig.APPLICATION_ID}.keys.note_filter.date_to_inclusive"

        private const val KEY_DATE_RANGE_TEXT =
            "${BuildConfig.APPLICATION_ID}.keys.note_filter.date_range_text"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.note_filter.date_range_picker"

        private const val PLACEHOLDER_DATE_FROM = "-∞"
        private const val PLACEHOLDER_DATE_TO = "+∞"
    }

    private val binding: DialogFilterNotesBinding
        get() = requireNotNull(_binding)

    private val initialFilter: NoteFilter
        get() = requireNotNull(_initialFilter)

    private val isFilterEnabled: Boolean
        get() = requireNotNull(_isFilterEnabled)

    private var _binding: DialogFilterNotesBinding? = null

    private var _initialFilter: NoteFilter? = null
    private var _isFilterEnabled: Boolean? = null

    private var onApplyFilterListener: FilterListener? = null
    private var onDisableFilterListener: FilterListener? = null
    private var onCancelFilterListener: FilterListener? = null

    private var dateFromInclusive: Days? = null
    private var dateToInclusive: Days? = null

    private val dateFormat = DateFormat.getDateInstance(
        DateFormat.SHORT,
        Locale.getDefault()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val arguments = requireArguments()
        _initialFilter = arguments.getParcelable(ARG_INITIAL_FILTER)
        _isFilterEnabled = arguments.getBoolean(ARG_IS_FILTER_ENABLED)

        savedInstanceState?.let { state ->
            dateFromInclusive = state.getParcelable(KEY_DATE_FROM_INCLUSIVE)
            dateToInclusive = state.getParcelable(KEY_DATE_TO_INCLUSIVE)
        } ?: run {
            dateFromInclusive = initialFilter.dateFromInclusive
            dateToInclusive = initialFilter.dateToExclusive?.yesterday
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { state ->
            restoreDateRange(state)
        } ?: run {
            initBpmRange()
            invalidateDateRange()
        }
        setListeners()
        restoreFragmentListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_DATE_FROM_INCLUSIVE, dateFromInclusive)
        outState.putParcelable(KEY_DATE_TO_INCLUSIVE, dateToInclusive)
        outState.putString(KEY_DATE_RANGE_TEXT, binding.dateRangeTextView.text.toString())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFilterNotesBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppTheme_MaterialAlertDialog_Filter
        )
        builder
            .setTitle(R.string.dialog_filter_notes_title)
            .setView(binding.root)
            .setPositiveButton(R.string.button_title_apply) { _, _ ->
                onApplyFilterListener?.invokeWithCurrentFilter()
            }
            .setNeutralButton(R.string.button_title_cancel) { _, _ ->
                onCancelFilterListener?.invokeWithCurrentFilter()
            }
        if (isFilterEnabled) {
            builder
                .setNegativeButton(R.string.button_title_disable) { _, _ ->
                    onDisableFilterListener?.invokeWithCurrentFilter()
                }
        }
        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelFilterListener?.invokeWithCurrentFilter()
    }

    /**
     * Set [listener] to use on positive button click.
     *
     * @param listener the [FilterListener] to use on positive button click.
     */
    fun setOnApplyFilterListener(listener: FilterListener) {
        onApplyFilterListener = listener
    }

    /**
     * Set [listener] to use on negative button click.
     *
     * @param listener the [FilterListener] to use on negative button click.
     */
    fun setOnDisableFilterListener(listener: FilterListener) {
        onDisableFilterListener = listener
    }

    /**
     * Set [listener] to use when the dialog is cancelled.
     *
     * @param listener the [FilterListener] to use when the dialog is cancelled.
     */
    fun setOnCancelFilterListener(listener: FilterListener) {
        onCancelFilterListener = listener
    }

    private fun initBpmRange() {
        initialFilter.bpmFromInclusive?.let {
            binding.bpmFromEditText.setText(it.toString())
        }
        initialFilter.bpmToInclusive?.let {
            binding.bpmToEditText.setText(it.toString())
        }
    }

    private fun invalidateDateRange() {
        binding.dateRangeTextView.text = dateRangeString(dateFromInclusive, dateToInclusive)
    }

    private fun restoreDateRange(savedInstanceState: Bundle) {
        savedInstanceState.getString(KEY_DATE_RANGE_TEXT)?.let {
            binding.dateRangeTextView.text = it
        }
    }

    private fun setListeners() {

        fun clearDateRange() {
            dateFromInclusive = null
            dateToInclusive = null
            invalidateDateRange()
        }

        fun clearBpmRange() {
            binding.bpmFromEditText.ensureNoTextInput()
            binding.bpmFromEditText.text?.clear()
            binding.bpmToEditText.ensureNoTextInput()
            binding.bpmToEditText.text?.clear()
        }

        binding.pickDateRangeImageButton.setOnClickListener {
            Timber.d("Click: pickDateRangeImageButton")
            val builder = MaterialDatePicker.Builder.dateRangePicker()
            builder.setTheme(R.style.AppTheme_MaterialAlertDialog_DatePicker)
            val selection = androidx.core.util.Pair.create<Long?, Long?>(
                dateFromInclusive?.epochMillis,
                dateToInclusive?.epochMillis
            )
            builder.setSelection(selection)
            val picker = builder.build()
            picker.addDateListener()
            picker.show(childFragmentManager, TAG_DATE_RANGE_PICKER)
        }

        binding.clearDateRangeImageButton.setOnClickListener {
            Timber.d("Click: clearDateRangeImageButton")
            clearDateRange()
        }

        binding.clearBpmRangeImageButton.setOnClickListener {
            Timber.d("Click: clearBpmRangeImageButton")
            clearBpmRange()
        }

        binding.clearAllButton.setOnClickListener {
            Timber.d("Click: clearAllButton")
            clearDateRange()
            clearBpmRange()
        }
    }

    private fun restoreFragmentListeners() {
        childFragmentManager.performIfFoundByTag<MaterialDatePicker<*>>(TAG_DATE_RANGE_PICKER) {
            it.addDateListener()
        }
    }

    private fun dateRangeString(from: Days?, to: Days?): String {
        val formatFrom = from?.format() ?: PLACEHOLDER_DATE_FROM
        val formatTo = to?.format() ?: PLACEHOLDER_DATE_TO
        return "$formatFrom — $formatTo"
    }

    private fun currentFilter(): NoteFilter = NoteFilter(
        dateFromInclusive = dateFromInclusive,
        dateToExclusive = dateToInclusive?.tomorrow,
        bpmFromInclusive = binding.bpmFromEditText.numberOrNull(),
        bpmToInclusive = binding.bpmToEditText.numberOrNull()
    )

    private fun FilterListener.invokeWithCurrentFilter() {
        val filter = currentFilter()
        this(filter)
    }

    private fun MaterialDatePicker<*>.addDateListener() {
        addOnPositiveButtonClickListener { selection ->
            Timber.d("Date range selected: selection=$selection")
            if (selection is androidx.core.util.Pair<*, *>) {
                val from = selection.first as? Long
                val to = selection.second as? Long
                dateFromInclusive = from?.let { Days(it) }
                dateToInclusive = to?.let { Days(it) }
                Timber.d("dateFromInclusive=$dateFromInclusive")
                Timber.d("dateToInclusive=$dateToInclusive")
                invalidateDateRange()
            }
        }
    }

    private fun Days.format(): String? = dateFormat.format(date)
}
