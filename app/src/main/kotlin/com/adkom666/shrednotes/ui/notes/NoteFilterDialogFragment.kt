package com.adkom666.shrednotes.ui.notes

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.databinding.DialogFilterNotesBinding
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.dialog.KeyboardlessDialogFragment
import com.adkom666.shrednotes.util.ensureNoTextInput
import com.adkom666.shrednotes.util.forwardCursor
import com.adkom666.shrednotes.util.numberOrNull
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

typealias FilterListener = (filter: NoteFilter) -> Unit

/**
 * Dialog fragment for configuring the notes filter.
 */
class NoteFilterDialogFragment : KeyboardlessDialogFragment() {

    companion object {

        private const val ARG_INITIAL_FILTER =
            "${BuildConfig.APPLICATION_ID}.args.note_filter.initial_filter"

        private const val ARG_IS_FILTER_ENABLED =
            "${BuildConfig.APPLICATION_ID}.args.note_filter.is_filter_enabled"

        private const val KEY_DATE_RANGE =
            "${BuildConfig.APPLICATION_ID}.keys.note_filter.date_range"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.note_filter.date_range_picker"

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
    }

    private val binding: DialogFilterNotesBinding
        get() = requireNotNull(_binding)

    private val initialFilter: NoteFilter
        get() = requireNotNull(_initialFilter)

    private val isFilterEnabled: Boolean
        get() = requireNotNull(_isFilterEnabled)

    private val dateRange: DateRange
        get() = requireNotNull(_dateRange)

    private var _binding: DialogFilterNotesBinding? = null

    private var _initialFilter: NoteFilter? = null
    private var _isFilterEnabled: Boolean? = null

    private var _dateRange: DateRange? = null

    private var onApplyFilterListener: FilterListener? = null
    private var onDisableFilterListener: FilterListener? = null
    private var onCancelFilterListener: FilterListener? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val arguments = requireArguments()
        _initialFilter = arguments.getParcelable(ARG_INITIAL_FILTER)
        _isFilterEnabled = arguments.getBoolean(ARG_IS_FILTER_ENABLED)

        _dateRange = savedInstanceState?.getParcelable(KEY_DATE_RANGE) ?: DateRange(
            initialFilter.dateFromInclusive,
            initialFilter.dateToExclusive?.yesterday
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_DATE_RANGE, dateRange)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFilterNotesBinding.inflate(layoutInflater)

        init(savedInstanceState)

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

    private fun init(savedInstanceState: Bundle?) {
        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            invalidateDateRangeText()
            initBpmRangeText()
        }
        setListeners()
        restoreFragmentListeners()
    }

    private fun invalidateDateRangeText() {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private fun initBpmRangeText() {
        initialFilter.bpmFromInclusive?.let {
            binding.bpmFromEditText.setText(it.toString())
            binding.bpmFromEditText.forwardCursor()
            binding.bpmFromEditText.clearFocus()
        }
        initialFilter.bpmToInclusive?.let {
            binding.bpmToEditText.setText(it.toString())
            binding.bpmToEditText.forwardCursor()
            binding.bpmToEditText.clearFocus()
        }
    }

    private fun setListeners() {

        fun clearDateRange() {
            changeDateRange(INFINITE_DATE_RANGE)
            invalidateDateRangeText()
        }

        fun clearBpmRangeText() {
            binding.bpmFromEditText.ensureNoTextInput()
            binding.bpmFromEditText.text?.clear()
            binding.bpmToEditText.ensureNoTextInput()
            binding.bpmToEditText.text?.clear()
        }

        binding.dateRange.pickDateRangeImageButton.setOnClickListener {
            Timber.d("Click: pickDateRangeImageButton")
            showDateRangePicker(
                childFragmentManager,
                TAG_DATE_RANGE_PICKER,
                { dateRange },
                ::changeDateRange
            )
        }

        binding.dateRange.clearDateRangeImageButton.setOnClickListener {
            Timber.d("Click: clearDateRangeImageButton")
            clearDateRange()
        }

        binding.clearBpmRangeImageButton.setOnClickListener {
            Timber.d("Click: clearBpmRangeImageButton")
            clearBpmRangeText()
        }

        binding.clearAllButton.setOnClickListener {
            Timber.d("Click: clearAllButton")
            clearDateRange()
            clearBpmRangeText()
        }
    }

    private fun restoreFragmentListeners() {
        restoreDateRangeListener(
            childFragmentManager,
            TAG_DATE_RANGE_PICKER,
            ::changeDateRange
        )
    }

    private fun changeDateRange(dateRange: DateRange) {
        _dateRange = dateRange
        invalidateDateRangeText()
    }

    private fun currentFilter(): NoteFilter = NoteFilter(
        dateFromInclusive = dateRange.fromInclusive,
        dateToExclusive = dateRange.toInclusive?.tomorrow,
        bpmFromInclusive = binding.bpmFromEditText.numberOrNull(),
        bpmToInclusive = binding.bpmToEditText.numberOrNull()
    )

    private fun FilterListener.invokeWithCurrentFilter() {
        val filter = currentFilter()
        this(filter)
    }
}
