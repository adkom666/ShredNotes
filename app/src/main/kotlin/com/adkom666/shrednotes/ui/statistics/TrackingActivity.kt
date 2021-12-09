package com.adkom666.shrednotes.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.ActivityTrackingBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.statistics.MaxBpmTracking
import com.adkom666.shrednotes.statistics.NoteCountTracking
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.DateRangeFormat
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.restoreDateRangeListener
import com.adkom666.shrednotes.util.setOnSafeClickListener
import com.adkom666.shrednotes.util.showDateRangePicker
import com.adkom666.shrednotes.util.time.Days
import com.adkom666.shrednotes.util.time.localTimestamp
import com.adkom666.shrednotes.util.toast
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Statistics tracking screen.
 */
@ExperimentalCoroutinesApi
class TrackingActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_TARGET_PARAMETER =
            "${BuildConfig.APPLICATION_ID}.extras.statistics.tracking.target_parameter"

        private const val TAG_DATE_RANGE_PICKER =
            "${BuildConfig.APPLICATION_ID}.tags.statistics.tracking.date_range_picker"

        private const val CHART_ANIMATION_DURATION_MILLIS = 1_000

        /**
         * Creating an intent to open the screen of statistics tracking.
         *
         * @param context [Context] to create an intent.
         * @param targetParameter target parameter for calculating statistics.
         * @return intent to open the screen of statistics tracking.
         */
        fun newIntent(
            context: Context,
            targetParameter: TrackingTargetParameter
        ): Intent {
            val intent = Intent(context, TrackingActivity::class.java)
            intent.putExtra(EXTRA_TARGET_PARAMETER, targetParameter)
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityTrackingBinding
        get() = requireNotNull(_binding)

    private val model: TrackingViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityTrackingBinding? = null
    private var _model: TrackingViewModel? = null

    private val dateRangeFormat: DateRangeFormat = DateRangeFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        setupExerciseListener()
        prepareChart()
        setupButtonListeners()
        restoreFragmentListeners()
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            val targetParameter =
                intent?.extras?.getSerializable(EXTRA_TARGET_PARAMETER)
                        as TrackingTargetParameter
            Timber.d("Target parameter is $targetParameter")
            model.prepare(targetParameter)
        }
        lifecycleScope.launchWhenCreated {
            model.start()
        }
    }

    private fun setupExerciseListener() {
        binding.exerciseSpinner.onItemSelectedListener = OnExerciseSelectedListener()
    }

    private fun prepareChart() {
        val illustrationColor = ContextCompat.getColor(this, R.color.illustration_color)
        val contentColor = ContextCompat.getColor(this, R.color.content_color)

        binding.trackingChart.xAxis.textColor = contentColor
        binding.trackingChart.xAxis.axisLineColor = contentColor
        binding.trackingChart.xAxis.gridColor = contentColor
        binding.trackingChart.xAxis.granularity = 1f
        binding.trackingChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.trackingChart.xAxis.valueFormatter = ChartAbscissaFormatter()

        binding.trackingChart.axisLeft.textColor = contentColor
        binding.trackingChart.axisLeft.axisLineColor = contentColor
        binding.trackingChart.axisLeft.gridColor = contentColor
        binding.trackingChart.axisLeft.axisMinimum = 0f
        binding.trackingChart.axisLeft.granularity = 1f

        binding.trackingChart.axisRight.textColor = contentColor
        binding.trackingChart.axisRight.axisLineColor = contentColor
        binding.trackingChart.axisRight.gridColor = contentColor
        binding.trackingChart.axisRight.axisMinimum = 0f
        binding.trackingChart.axisRight.granularity = 1f

        binding.trackingChart.description.isEnabled = false
        binding.trackingChart.legend.isEnabled = false
        binding.trackingChart.setNoDataText(getString(R.string.message_no_data))
        binding.trackingChart.setNoDataTextColor(illustrationColor)

        binding.trackingChart.isDoubleTapToZoomEnabled = false

        binding.trackingChart.animateY(CHART_ANIMATION_DURATION_MILLIS)
    }

    private fun setupButtonListeners() {
        binding.dateRange.pickDateRangeImageButton.setOnSafeClickListener {
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
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
        lifecycleScope.launch {
            model.signalChannel.consumeEach(::process)
        }
    }

    private fun show(message: TrackingViewModel.Message) = when (message) {
        is TrackingViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: TrackingViewModel.Message.Error) = when (message) {
        is TrackingViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        TrackingViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: TrackingViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is TrackingViewModel.Signal.Title ->
                setTitle(signal.value)
            is TrackingViewModel.Signal.ActualDateRange ->
                setDateRange(signal.value)
            is TrackingViewModel.Signal.ExerciseList ->
                setExerciseList(signal.value)
            is TrackingViewModel.Signal.ActualStatistics ->
                setStatistics(signal)
        }
    }

    private fun setTitle(titleValue: TrackingViewModel.Signal.Title.Value) {
        val resId = titleValue.getStringResId()
        binding.statisticsTitleTextView.text = getString(resId)
    }

    private fun setDateRange(dateRange: DateRange) {
        binding.dateRange.dateRangeTextView.text = dateRangeFormat.format(dateRange)
    }

    private fun setExerciseList(exercises: List<Exercise?>) {
        binding.exerciseSpinner.adapter = ExerciseSpinnerAdapter(exercises)
        val position = exercises.indexOf(model.exercise)
        if (position >= 0 && position < exercises.size) {
            binding.exerciseSpinner.setSelection(position)
        }
    }

    private fun setStatistics(
        statistics: TrackingViewModel.Signal.ActualStatistics
    ) = when (statistics) {
        is TrackingViewModel.Signal.ActualStatistics.Progress ->
            setProgress(statistics.value)
        is TrackingViewModel.Signal.ActualStatistics.TrainingIntensity ->
            setTrainingIntensity(statistics.value)
    }

    private fun setTrainingIntensity(noteCountTracking: NoteCountTracking) = setTracking {
        barEntriesOf(noteCountTracking)
    }

    private fun setProgress(maxBpmTracking: MaxBpmTracking) = setTracking {
        barEntriesOf(maxBpmTracking)
    }

    private fun setTracking(barEntryEmitter: () -> List<BarEntry>) {
        binding.trackingChart.clear()
        val entries = barEntryEmitter()
        if (entries.isNotEmpty()) {
            val dataSet = barDataSetOf(entries)
            binding.trackingChart.data = BarData(dataSet)
        }
        binding.trackingChart.invalidate()
    }

    private fun barEntriesOf(
        maxBpmTracking: MaxBpmTracking
    ): List<BarEntry> = maxBpmTracking.points.map { point ->
        BarEntry(
            point.days.order().toFloat(),
            point.maxBpm.toFloat()
        )
    }

    private fun barEntriesOf(
        noteCountTracking: NoteCountTracking
    ): List<BarEntry> = noteCountTracking.points.map { point ->
        BarEntry(
            point.days.order().toFloat(),
            point.noteCount.toFloat()
        )
    }

    private fun barDataSetOf(entries: List<BarEntry>): BarDataSet {
        val dataSet = BarDataSet(entries, "")
        dataSet.setDrawValues(false)
        dataSet.color = ContextCompat.getColor(this, R.color.sky)
        return dataSet
    }

    @StringRes
    private fun TrackingViewModel.Signal.Title.Value.getStringResId(): Int =
        when (this) {
            TrackingViewModel.Signal.Title.Value.TRAINING_INTENSITY ->
                R.string.title_statistics_tracking_training_intensity
            TrackingViewModel.Signal.Title.Value.PROGRESS ->
                R.string.title_statistics_tracking_progress
        }

    private inner class OnExerciseSelectedListener : AdapterView.OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            Timber.d("Selected exercise: position=$position")
            (parent?.adapter as? ExerciseSpinnerAdapter)?.let { adapter ->
                model.exercise = adapter.getItem(position)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            Timber.d("Exercise not selected")
        }
    }

    private inner class StateObserver : Observer<TrackingViewModel.State> {

        override fun onChanged(state: TrackingViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                TrackingViewModel.State.Waiting ->
                    setWaiting()
                TrackingViewModel.State.Working ->
                    setWorking()
                TrackingViewModel.State.Finishing ->
                    finish()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.statisticsCard.isVisible = isActive.not()
        }
    }
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L

private typealias Order = Long

private fun Float.roundToOrder(): Order = roundToLong()

private fun Days.order(): Order {
    return epochMillis.localTimestamp() / MILLIS_PER_DAY
}

private fun Order.toDays(): Days {
    return Days(this * MILLIS_PER_DAY)
}

private class ChartAbscissaFormatter : ValueFormatter() {

    private val dateFormat: DateFormat = DateFormat.getDateInstance(
        DateFormat.SHORT,
        Locale.getDefault()
    )

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val dayOrder = value.roundToOrder()
        return dateFormat.format(dayOrder.toDays().date)
    }
}

private class ExerciseSpinnerAdapter(
    private val exercises: List<Exercise?>
) : BaseAdapter() {

    override fun getCount(): Int = exercises.size

    override fun getItem(position: Int): Exercise? = exercises[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View = getView(
        R.layout.item_exercise_name,
        position,
        convertView,
        parent
    )

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View = getView(
        R.layout.item_exercise_name_dropdown,
        position,
        convertView,
        parent
    )

    private fun getView(
        @LayoutRes
        resId: Int,
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView
            ?: LayoutInflater.from(parent?.context).inflate(
                resId,
                parent,
                false
            )
        val textView = view.findViewById<TextView>(R.id.exercise_name_text_view)
        textView.text = exercises[position]?.name ?: ""
        return view
    }
}
