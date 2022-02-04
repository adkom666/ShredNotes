package com.adkom666.shrednotes.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityRecordsBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.setOnSafeClickListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

/**
 * Records screen.
 */
class RecordsActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_TARGET_PARAMETER =
            "${BuildConfig.APPLICATION_ID}.extras.statistics.records.target_parameter"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.statistics.records.date_range_picker"

        /**
         * Creating an intent to open the records screen.
         *
         * @param context [Context] to create an intent.
         * @param targetParameter target parameter for calculating records.
         * @return intent to open the records screen.
         */
        fun newIntent(
            context: Context,
            targetParameter: RecordsTargetParameter
        ): Intent {
            val intent = Intent(context, RecordsActivity::class.java)
            intent.putExtra(EXTRA_TARGET_PARAMETER, targetParameter)
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityRecordsBinding
        get() = requireNotNull(_binding)

    private val model: RecordsViewModel
        get() = requireNotNull(_model)

    private val recordAdapter: RecordAdapter
        get() = requireNotNull(_recordAdapter)

    private val adapter: ListAdapter<*, *>
        get() = when (val currentRecordAdapter = recordAdapter) {
            is RecordAdapter.TopExerciseAdapter -> currentRecordAdapter.value
            is RecordAdapter.TopNoteAdapter -> currentRecordAdapter.value
        }

    private var _binding: ActivityRecordsBinding? = null
    private var _model: RecordsViewModel? = null
    private var _recordAdapter: RecordAdapter? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        val targetParameter =
            intent?.extras?.getSerializable(EXTRA_TARGET_PARAMETER)
                    as RecordsTargetParameter
        Timber.d("Target parameter is $targetParameter")

        _recordAdapter = createRecordAdapter(targetParameter)

        adjustRecordsRecycler()
        prepareButtonMore()
        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenFlows()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            model.prepare(targetParameter)
        }
    }

    private fun createRecordAdapter(
        targetParameter: RecordsTargetParameter
    ): RecordAdapter = when (targetParameter) {
        RecordsTargetParameter.BPM ->
            RecordAdapter.TopNoteAdapter(NoteAdapter())
        RecordsTargetParameter.NOTE_COUNT ->
            RecordAdapter.TopExerciseAdapter(ExerciseInfoAdapter())
    }

    private fun adjustRecordsRecycler() {
        val llm = LinearLayoutManager(this)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.recordsRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.recordsRecycler.addItemDecoration(decoration)
        binding.recordsRecycler.adapter = adapter
    }

    private fun prepareButtonMore() {
        binding.moreButton.isVisible = model.areMoreRecordsPresent
    }

    private fun setupButtonListeners() {
        binding.dateRange.pickDateRangeImageButton.setOnSafeClickListener {
            Timber.d("Click: pickDateRangeImageButton")
            showDateRangePicker(
                supportFragmentManager,
                TAG_DATE_RANGE_PICKER,
                model.dateRange,
                ::changeDateRange
            )
        }
        binding.dateRange.clearDateRangeImageButton.setOnClickListener {
            Timber.d("Click: clearDateRangeImageButton")
            model.dateRange = INFINITE_DATE_RANGE
        }
        binding.okButton.setOnClickListener {
            Timber.d("Click: OK")
            model.onOkButtonClick()
        }
        binding.moreButton.setOnSafeClickListener {
            Timber.d("Click: More")
            model.onMoreButtonClick()
        }
    }

    private fun restoreFragmentListeners() {
        restoreDateRangeListener(
            supportFragmentManager,
            TAG_DATE_RANGE_PICKER,
            ::changeDateRange
        )
    }

    private fun changeDateRange(dateRange: DateRange) {
        model.dateRange = dateRange
    }

    private fun observeLiveData() {
        model.stateAsLiveData.observe(this, StateObserver())
        model.recordsAsLiveData.observe(this, RecordsObserver())
    }

    private fun listenFlows() {
        lifecycleScope.launchWhenStarted {
            model.messageFlow.collect(::show)
        }
        lifecycleScope.launchWhenCreated {
            model.signalFlow.collect(::process)
        }
    }

    private fun show(message: RecordsViewModel.Message) = when (message) {
        is RecordsViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: RecordsViewModel.Message.Error) = when (message) {
        is RecordsViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        RecordsViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: RecordsViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is RecordsViewModel.Signal.Subtitle ->
                setSubtitle(signal.value)
            is RecordsViewModel.Signal.ActualDateRange ->
                setDateRange(signal.value)
            is RecordsViewModel.Signal.HasMoreRecords ->
                setButtonMoreVisibility(signal.value)
        }
    }

    private fun setSubtitle(subtitleValue: RecordsViewModel.Signal.Subtitle.Value) {
        val resId = subtitleValue.getStringResId()
        binding.recordsSubtitleTextView.text = getString(resId)
    }

    private fun setDateRange(dateRange: DateRange) {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private fun setButtonMoreVisibility(isVisible: Boolean) {
        binding.moreButton.isVisible = isVisible
    }

    @StringRes
    private fun RecordsViewModel.Signal.Subtitle.Value.getStringResId(): Int = when (this) {
        RecordsViewModel.Signal.Subtitle.Value.BPM ->
            R.string.title_statistics_bpm
        RecordsViewModel.Signal.Subtitle.Value.NOTE_COUNT ->
            R.string.title_statistics_note_count
    }

    private inner class StateObserver : Observer<RecordsViewModel.State> {

        override fun onChanged(state: RecordsViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                RecordsViewModel.State.Waiting ->
                    setWaiting()
                is RecordsViewModel.State.Working ->
                    setWorking(state.isUiLocked)
                RecordsViewModel.State.Finishing ->
                    finish()
            }
        }

        private fun setWaiting() {
            binding.progressBar.isVisible = true
            binding.recordsScroll.isVisible = false
        }

        private fun setWorking(isUiLocked: Boolean) {
            binding.progressBar.isVisible = isUiLocked
            binding.recordsScroll.isVisible = true
            val isUiUnlocked = isUiLocked.not()
            binding.dateRange.pickDateRangeImageButton.isEnabled = isUiUnlocked
            binding.dateRange.clearDateRangeImageButton.isEnabled = isUiUnlocked
            binding.moreButton.isEnabled = isUiUnlocked
        }
    }

    private inner class RecordsObserver : Observer<RecordsViewModel.Records?> {

        override fun onChanged(records: RecordsViewModel.Records?) {
            Timber.d("records=$records")
            records?.let { setRecords(it) }
        }

        private fun setRecords(records: RecordsViewModel.Records) {
            val areRecordsPresent = when (records) {
                is RecordsViewModel.Records.Bpm ->
                    records.value.topNotes.let { topNotes ->
                        if (topNotes.isNotEmpty()) {
                            (adapter as NoteAdapter).submitList(topNotes)
                            true
                        } else {
                            false
                        }
                    }
                is RecordsViewModel.Records.NoteCount ->
                    records.value.topExerciseNames.map {
                        "${it.exerciseName} (${it.noteCount})"
                    }.let { topExerciseNames ->
                        if (topExerciseNames.isNotEmpty()) {
                            (adapter as ExerciseInfoAdapter).submitList(topExerciseNames)
                            true
                        } else {
                            false
                        }
                    }
            }
            setRecordsVisible(areRecordsPresent)
        }

        private fun setRecordsVisible(isVisible: Boolean) {
            binding.noRecordsTextView.isVisible = isVisible.not()
            binding.recordsRecycler.isVisible = isVisible
        }
    }
}

/**
 * Wrapped adapter for interacting with the records.
 */
private sealed class RecordAdapter {

    /**
     * [NoteAdapter] wrapper.
     *
     * @property value wrapped [NoteAdapter] instance.
     */
    data class TopNoteAdapter(val value: NoteAdapter) : RecordAdapter()

    /**
     * [ExerciseInfoAdapter] wrapper.
     *
     * @property value wrapped [ExerciseInfoAdapter] instance.
     */
    data class TopExerciseAdapter(val value: ExerciseInfoAdapter) : RecordAdapter()
}
