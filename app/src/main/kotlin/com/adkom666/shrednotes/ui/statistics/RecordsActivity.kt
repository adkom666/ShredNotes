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
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityRecordsBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.FirstItemDecoration
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Record screen.
 */
@ExperimentalCoroutinesApi
class RecordsActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_TARGET_PARAMETER =
            "${BuildConfig.APPLICATION_ID}.extras.records_target_parameter"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.records.date_range_picker"

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

    private var _binding: ActivityRecordsBinding? = null
    private var _model: RecordsViewModel? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        adjustRecordsRecycler()
        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val targetParameter =
                intent?.extras?.getSerializable(EXTRA_TARGET_PARAMETER)
                        as RecordsTargetParameter
            Timber.d("Target parameter is $targetParameter")
            model.prepare(targetParameter)
        }
        lifecycleScope.launchWhenCreated {
            model.start()
        }
    }

    private fun adjustRecordsRecycler() {
        val llm = LinearLayoutManager(this)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.recordsRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.recordsRecycler.addItemDecoration(decoration)
    }

    private fun setupButtonListeners() {
        binding.dateRange.pickDateRangeImageButton.setOnClickListener {
            Timber.d("Click: pickDateRangeImageButton")
            showDateRangePicker(
                supportFragmentManager,
                TAG_DATE_RANGE_PICKER,
                { model.dateRange },
                ::changeDateRange
            )
        }
        binding.dateRange.clearDateRangeImageButton.setOnClickListener {
            Timber.d("Click: clearDateRangeImageButton")
            model.dateRange = INFINITE_DATE_RANGE
        }
        binding.okButton.setOnClickListener {
            Timber.d("OK clicked")
            model.onOkButtonClick()
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
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
        lifecycleScope.launch {
            model.signalChannel.consumeEach(::process)
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
            is RecordsViewModel.Signal.Records ->
                setRecords(signal)
        }
    }

    private fun setSubtitle(subtitleValue: RecordsViewModel.Signal.Subtitle.Value) {
        val resId = subtitleValue.getStringResId()
        binding.recordsSubtitleTextView.text = getString(resId)
    }

    private fun setDateRange(dateRange: DateRange) {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private fun setRecords(records: RecordsViewModel.Signal.Records) {
        val adapter = when (records) {
            is RecordsViewModel.Signal.Records.Bpm ->
                records.value.topNotes.let {
                    if (it.isNotEmpty()) NoteAdapter(it) else null
                }
            is RecordsViewModel.Signal.Records.NoteCount ->
                records.value.topExerciseNames.map {
                    "${it.exerciseName} (${it.noteCount})"
                }.let {
                    if (it.isNotEmpty()) ExerciseInfoAdapter(it) else null
                }
        }
        adapter?.let {
            setRecordsVisible(true)
            binding.recordsRecycler.adapter = it
        } ?: setRecordsVisible(false)
    }

    private fun setRecordsVisible(isVisible: Boolean) {
        binding.noRecordsTextView.isVisible = isVisible.not()
        binding.recordsRecycler.isVisible = isVisible
    }

    @StringRes
    private fun RecordsViewModel.Signal.Subtitle.Value.getStringResId(): Int = when (this) {
        RecordsViewModel.Signal.Subtitle.Value.BPM ->
            R.string.statistics_subtitle_bpm
        RecordsViewModel.Signal.Subtitle.Value.NOTE_COUNT ->
            R.string.statistics_subtitle_note_count
    }

    private inner class StateObserver : Observer<RecordsViewModel.State> {

        override fun onChanged(state: RecordsViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                RecordsViewModel.State.Waiting ->
                    setWaiting()
                RecordsViewModel.State.Working ->
                    setWorking()
                RecordsViewModel.State.Finishing ->
                    finish()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.recordsScroll.isVisible = isActive.not()
        }
    }
}
