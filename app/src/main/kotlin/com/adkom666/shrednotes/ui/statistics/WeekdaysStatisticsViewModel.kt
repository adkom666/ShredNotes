package com.adkom666.shrednotes.ui.statistics

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.statistics.WeekdaysStatistics
import com.adkom666.shrednotes.statistics.WeekdaysStatisticsAggregator
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.getNullableDays
import com.adkom666.shrednotes.util.putNullableDays
import com.adkom666.shrednotes.util.time.Days
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Statistics by days of week screen model.
 *
 * @property statisticsAggregator source of statistics by days of week.
 * @property preferences project's [SharedPreferences].
 */
@ExperimentalCoroutinesApi
class WeekdaysStatisticsViewModel @Inject constructor(
    private val statisticsAggregator: WeekdaysStatisticsAggregator,
    private val preferences: SharedPreferences
) : ViewModel() {

    private companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.weekdays.does_max_bpm_date_range_have_date_from"

        private const val KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.weekdays.max_bpm_date_range_date_from_inclusive"

        private const val KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO =
            "statistics.weekdays.does_max_bpm_date_range_have_date_to"

        private const val KEY_MAX_BPM_DATE_RANGE_DATE_TO_INCLUSIVE =
            "statistics.weekdays.max_bpm_date_range_date_to_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.weekdays.does_note_count_date_range_have_date_from"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.weekdays.note_count_date_range_date_from_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO =
            "statistics.weekdays.does_note_count_date_range_have_date_to"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_INCLUSIVE =
            "statistics.weekdays.note_count_date_range_date_to_inclusive"
    }

    /**
     * Statistics by days of week state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         */
        object Waiting : State()

        /**
         * Interacting with the user.
         */
        object Working : State()

        /**
         * Finishing work with the statistics by days of week.
         */
        object Finishing : State()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Error message.
         */
        sealed class Error : Message() {

            /**
             * The details of this error are described in the [details].
             *
             * @property details the details of the error.
             */
            data class Clarified(val details: String) : Error()

            /**
             * Unknown error.
             */
            object Unknown : Error()
        }
    }

    /**
     * Information signal.
     */
    sealed class Signal {

        /**
         * Show subtitle for statistics by days of week.
         *
         * @property value ready-made subtitle value.
         */
        data class Subtitle(val value: Value) : Signal() {

            /**
             * Subtitle value for statistics by days of week.
             */
            enum class Value {
                AVERAGE_AMONG_MAX_BPM,
                AVERAGE_NOTE_COUNT
            }
        }

        /**
         * Show actual date range.
         *
         * @property value date range.
         */
        data class ActualDateRange(val value: DateRange) : Signal()

        /**
         * Show actual statistics by days of week.
         *
         * @property value ready-made statistics by days of week.
         */
        data class ActualStatistics(val value: WeekdaysStatistics) : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = Transformations.distinctUntilChanged(_stateAsLiveData)

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

    /**
     * Consume information signals from this channel in the UI thread.
     */
    val signalChannel: ReceiveChannel<Signal>
        get() = _signalChannel.openSubscription()

    /**
     * The date range over which statistics should be shown.
     */
    var dateRange: DateRange
        get() = requireNotNull(_dateRange)
        set(value) {
            if (value != _dateRange) {
                Timber.d("Change date range: old=$_dateRange, new=$value")
                _dateRange = value
                saveDateRange(value, targetParameter)
                setState(State.Waiting)
                give(Signal.ActualDateRange(value))
                viewModelScope.launch {
                    aggregateStatistics(value)
                    setState(State.Working)
                }
            }
        }

    private val targetParameter: WeekdaysStatisticsTargetParameter
        get() = requireNotNull(_targetParameter)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private var _targetParameter: WeekdaysStatisticsTargetParameter? = null
    private var _dateRange: DateRange? = null

    /**
     * Prepare for working with the statistics by days of week.
     *
     * @param targetParameter target parameter for calculating statistics.
     */
    fun prepare(targetParameter: WeekdaysStatisticsTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        _targetParameter = targetParameter
        _dateRange = loadDateRange(targetParameter)
        setState(State.Waiting)
        give(Signal.Subtitle(targetParameter.toSubtitleValue()))
        give(Signal.ActualDateRange(dateRange))
        statisticsAggregator.clearCache()
    }

    /**
     * Start working.
     */
    suspend fun start() {
        Timber.d("Start")
        aggregateStatistics(dateRange)
        setState(State.Working)
    }

    /**
     * Call this method to handle the OK button click.
     */
    fun onOkButtonClick() {
        Timber.d("On OK button click")
        setState(State.Finishing)
    }

    private fun loadDateRange(
        targetParameter: WeekdaysStatisticsTargetParameter
    ): DateRange = when (targetParameter) {
        WeekdaysStatisticsTargetParameter.MAX_BPM ->
            preferences.getMaxBpmDateRange()
        WeekdaysStatisticsTargetParameter.NOTE_COUNT ->
            preferences.getNoteCountDateRange()
    }

    private fun saveDateRange(
        dateRange: DateRange,
        targetParameter: WeekdaysStatisticsTargetParameter
    ) {
        preferences.edit {
            when (targetParameter) {
                WeekdaysStatisticsTargetParameter.MAX_BPM ->
                    putMaxBpmDateRange(dateRange)
                WeekdaysStatisticsTargetParameter.NOTE_COUNT ->
                    putNoteCountDateRange(dateRange)
            }
        }
    }

    private suspend fun aggregateStatistics(dateRange: DateRange) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val statistics = when (targetParameter) {
                WeekdaysStatisticsTargetParameter.MAX_BPM ->
                    statisticsAggregator.aggregateAverageAmongMaxBpm(dateRange)
                WeekdaysStatisticsTargetParameter.NOTE_COUNT ->
                    statisticsAggregator.aggregateAverageNoteCount(dateRange)
            }
            give(Signal.ActualStatistics(statistics))
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
        }
    }

    private fun reportAbout(e: Exception) {
        e.localizedMessage?.let {
            report(Message.Error.Clarified(it))
        } ?: report(Message.Error.Unknown)
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.offer(signal)
    }

    private fun WeekdaysStatisticsTargetParameter.toSubtitleValue(): Signal.Subtitle.Value =
        when (this) {
            WeekdaysStatisticsTargetParameter.MAX_BPM ->
                Signal.Subtitle.Value.AVERAGE_AMONG_MAX_BPM
            WeekdaysStatisticsTargetParameter.NOTE_COUNT ->
                Signal.Subtitle.Value.AVERAGE_NOTE_COUNT
        }

    private fun SharedPreferences.getMaxBpmDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toInclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putMaxBpmDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO,
            value = dateRange.toInclusive
        )
    }

    private fun SharedPreferences.getNoteCountDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toInclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putNoteCountDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO,
            value = dateRange.toInclusive
        )
    }
}
