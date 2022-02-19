package com.adkom666.shrednotes.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityStatisticsCommonBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.statistics.CommonStatistics
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.setOnSafeClickListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.collect
import timber.log.Timber

/**
 * Common statistics screen.
 */
@ExperimentalTime
class CommonStatisticsActivity : AppCompatActivity() {

    companion object {

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.statistics.common.date_range_picker"

        private val LINE_SEPARATOR = System.getProperty("line.separator")

        /**
         * Creating an intent to open the common statistics screen.
         *
         * @param context [Context] to create an intent.
         * @return intent to open the common statistics screen.
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, CommonStatisticsActivity::class.java)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityStatisticsCommonBinding
        get() = requireNotNull(_binding)

    private val model: CommonStatisticsViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityStatisticsCommonBinding? = null
    private var _model: CommonStatisticsViewModel? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityStatisticsCommonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenFlows()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            model.prepare()
        }
        model.start()
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
        model.statisticsAsLiveData.observe(this, StatisticsObserver())
    }

    private fun listenFlows() {
        lifecycleScope.launchWhenStarted {
            model.messageFlow.collect(::show)
        }
        lifecycleScope.launchWhenCreated {
            model.signalFlow.collect(::process)
        }
    }

    private fun show(message: CommonStatisticsViewModel.Message) = when (message) {
        is CommonStatisticsViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: CommonStatisticsViewModel.Message.Error) = when (message) {
        is CommonStatisticsViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        CommonStatisticsViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: CommonStatisticsViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is CommonStatisticsViewModel.Signal.ActualDateRange ->
                setDateRange(signal.value)
        }
    }

    private fun setDateRange(dateRange: DateRange) {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private inner class StateObserver : Observer<CommonStatisticsViewModel.State> {

        override fun onChanged(state: CommonStatisticsViewModel.State?) {
            Timber.d("State is $state")
            state?.let { setState(it) }
        }

        private fun setState(state: CommonStatisticsViewModel.State) = when (state) {
            CommonStatisticsViewModel.State.Waiting ->
                setWaiting()
            is CommonStatisticsViewModel.State.Working ->
                setWorking(state.isUiLocked)
            CommonStatisticsViewModel.State.Finishing ->
                finish()
        }

        private fun setWaiting() {
            binding.progressBar.isVisible = true
            binding.statisticsCard.isVisible = false
        }

        private fun setWorking(isUiLocked: Boolean) {
            binding.progressBar.isVisible = isUiLocked
            binding.statisticsCard.isVisible = true
            val isUiUnlocked = isUiLocked.not()
            binding.dateRange.pickDateRangeImageButton.isEnabled = isUiUnlocked
            binding.dateRange.clearDateRangeImageButton.isEnabled = isUiUnlocked
        }
    }

    private inner class StatisticsObserver : Observer<CommonStatistics?> {

        override fun onChanged(statistics: CommonStatistics?) {
            Timber.d("statistics=$statistics")
            statistics?.let { setStatistics(it) }
        }

        private fun setStatistics(statistics: CommonStatistics) {
            val lineNotes = getString(
                R.string.text_statistics_common_notes,
                statistics.notes
            )
            val lineRelatedExercises = getString(
                R.string.text_statistics_common_related_exercises,
                statistics.relatedExercises
            )
            val lineDaysIdeally = getString(
                R.string.text_statistics_common_days_ideally,
                statistics.daysIdeally
            )
            val lineActiveDays = getString(
                R.string.text_statistics_common_active_days,
                statistics.activeDays
            )
            var text = lineNotes + LINE_SEPARATOR +
                    lineRelatedExercises + LINE_SEPARATOR +
                    lineDaysIdeally + LINE_SEPARATOR +
                    lineActiveDays

            statistics.activeDaysShare?.let { share ->
                val lineActiveDaysShare = getString(
                    R.string.text_statistics_common_active_days_share,
                    share * 100
                )
                text += LINE_SEPARATOR
                text += lineActiveDaysShare
            }

            binding.statisticsTextView.text = text

            val lineAllNotes = getString(
                R.string.text_statistics_common_all_notes,
                statistics.allNotes
            )
            val lineAllExercises = getString(
                R.string.text_statistics_common_all_exercises,
                statistics.allExercises
            )
            val textForAllTheTime = lineAllNotes + LINE_SEPARATOR + lineAllExercises
            binding.statisticsForAllTheTimeTextView.text = textForAllTheTime
        }
    }
}
