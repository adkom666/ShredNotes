package com.adkom666.shrednotes.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityStatisticsWeekdaysBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.statistics.Weekday
import com.adkom666.shrednotes.statistics.WeekdaysStatistics
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.adkom666.shrednotes.util.toast
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Statistics by days of week screen.
 */
@ExperimentalCoroutinesApi
class WeekdaysStatisticsActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_TARGET_PARAMETER =
            "${BuildConfig.APPLICATION_ID}.extras.statistics.weekdays.target_parameter"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.statistics.weekdays.date_range_picker"

        /**
         * Creating an intent to open the screen of statistics by days of week.
         *
         * @param context [Context] to create an intent.
         * @param targetParameter target parameter for calculating statistics.
         * @return intent to open the screen of statistics by days of week.
         */
        fun newIntent(
            context: Context,
            targetParameter: WeekdaysStatisticsTargetParameter
        ): Intent {
            val intent = Intent(context, WeekdaysStatisticsActivity::class.java)
            intent.putExtra(EXTRA_TARGET_PARAMETER, targetParameter)
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityStatisticsWeekdaysBinding
        get() = requireNotNull(_binding)

    private val model: WeekdaysStatisticsViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityStatisticsWeekdaysBinding? = null
    private var _model: WeekdaysStatisticsViewModel? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityStatisticsWeekdaysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        prepareChart()
        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val targetParameter =
                intent?.extras?.getSerializable(EXTRA_TARGET_PARAMETER)
                        as WeekdaysStatisticsTargetParameter
            Timber.d("Target parameter is $targetParameter")
            model.prepare(targetParameter)
        }
        lifecycleScope.launchWhenCreated {
            model.start()
        }
    }

    private fun prepareChart() {
        val blackColor = ContextCompat.getColor(this, R.color.black)
        val cardColor = ContextCompat.getColor(this, R.color.normal_card_color)

        binding.averageAmongMaxBpmPieChart.setEntryLabelColor(blackColor)
        binding.averageAmongMaxBpmPieChart.setHoleColor(cardColor)
        binding.averageAmongMaxBpmPieChart.setTransparentCircleColor(cardColor)
        binding.averageAmongMaxBpmPieChart.holeRadius = 45f
        binding.averageAmongMaxBpmPieChart.transparentCircleRadius = 50f

        binding.averageAmongMaxBpmPieChart.description.isEnabled = false
        binding.averageAmongMaxBpmPieChart.legend.isEnabled = false
        binding.averageAmongMaxBpmPieChart.setNoDataText(getString(R.string.message_no_data))
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

    private fun show(message: WeekdaysStatisticsViewModel.Message) = when (message) {
        is WeekdaysStatisticsViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: WeekdaysStatisticsViewModel.Message.Error) = when (message) {
        is WeekdaysStatisticsViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        WeekdaysStatisticsViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: WeekdaysStatisticsViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is WeekdaysStatisticsViewModel.Signal.Subtitle ->
                setSubtitle(signal.value)
            is WeekdaysStatisticsViewModel.Signal.ActualDateRange ->
                setDateRange(signal.value)
            is WeekdaysStatisticsViewModel.Signal.Statistics ->
                setStatistics(signal.value)
        }
    }

    private fun setSubtitle(subtitleValue: WeekdaysStatisticsViewModel.Signal.Subtitle.Value) {
        val resId = subtitleValue.getStringResId()
        binding.statisticsSubtitleTextView.text = getString(resId)
    }

    private fun setDateRange(dateRange: DateRange) {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private fun setStatistics(statistics: WeekdaysStatistics) {
        binding.averageAmongMaxBpmPieChart.clear()
        val entries = pieEntriesOf(statistics)
        if (entries.isNotEmpty()) {
            val dataSet = pieDataSetOf(entries)
            binding.averageAmongMaxBpmPieChart.data = PieData(dataSet)
        }
        binding.averageAmongMaxBpmPieChart.invalidate()
    }

    private fun pieEntriesOf(statistics: WeekdaysStatistics): List<PieEntry> {
        val entries = mutableListOf<PieEntry>()
        Weekday.values().forEach { weekday ->
            statistics.valueMap[weekday]?.let { value ->
                if (value > 0f) {
                    val weekdayLabel = getString(weekday.toLabelResId())
                    val entry = PieEntry(value, weekdayLabel)
                    entries.add(entry)
                }
            }
        }
        return entries
    }

    private fun pieDataSetOf(entries: List<PieEntry>): PieDataSet {
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.asList()
        val blackColor = ContextCompat.getColor(this, R.color.black)
        dataSet.valueTextColor = blackColor
        dataSet.setValueTextSize(R.dimen.pie_chart_value_text_size)
        return dataSet
    }

    private fun PieDataSet.setValueTextSize(@DimenRes resId: Int) {
        val density = resources.displayMetrics.density
        if (density > 0f) {
            val textSizeInPixels = resources.getDimension(resId)
            val textSizeInDp = textSizeInPixels / density
            valueTextSize = textSizeInDp
        } else if (BuildConfig.DEBUG) {
            error("Illegal display density: density=$density")
        }
    }

    @StringRes
    private fun Weekday.toLabelResId(): Int = when (this) {
        Weekday.SUNDAY -> R.string.label_sunday
        Weekday.MONDAY -> R.string.label_monday
        Weekday.TUESDAY -> R.string.label_tuesday
        Weekday.WEDNESDAY -> R.string.label_wednesday
        Weekday.THURSDAY -> R.string.label_thursday
        Weekday.FRIDAY -> R.string.label_friday
        Weekday.SATURDAY -> R.string.label_saturday
    }

    @StringRes
    private fun WeekdaysStatisticsViewModel.Signal.Subtitle.Value.getStringResId(): Int =
        when (this) {
            WeekdaysStatisticsViewModel.Signal.Subtitle.Value.AVERAGE_AMONG_MAX_BPM ->
                R.string.statistics_subtitle_average_among_max_bpm
            WeekdaysStatisticsViewModel.Signal.Subtitle.Value.AVERAGE_NOTE_COUNT ->
                R.string.statistics_subtitle_average_note_count
        }

    private inner class StateObserver : Observer<WeekdaysStatisticsViewModel.State> {

        override fun onChanged(state: WeekdaysStatisticsViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                WeekdaysStatisticsViewModel.State.Waiting ->
                    setWaiting()
                WeekdaysStatisticsViewModel.State.Working ->
                    setWorking()
                WeekdaysStatisticsViewModel.State.Finishing ->
                    finish()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.statisticsScroll.isVisible = isActive.not()
        }
    }
}
