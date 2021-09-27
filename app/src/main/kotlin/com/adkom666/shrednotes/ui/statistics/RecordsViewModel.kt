package com.adkom666.shrednotes.ui.statistics

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.statistics.BpmRecords
import com.adkom666.shrednotes.statistics.NoteCountRecords
import com.adkom666.shrednotes.statistics.RecordsAggregator
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
 * Records screen model.
 *
 * @property recordsAggregator source of records.
 * @property preferences project's [SharedPreferences].
 */
@ExperimentalCoroutinesApi
class RecordsViewModel @Inject constructor(
    private val recordsAggregator: RecordsAggregator,
    private val preferences: SharedPreferences
) : ViewModel() {

    private companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val BPM_RECORDS_LIMIT = 5
        private const val NOTE_COUNT_RECORDS_LIMIT = 5

        private const val KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.records.does_bpm_date_range_have_date_from"

        private const val KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.records.bpm_date_range_date_from_inclusive"

        private const val KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO =
            "statistics.records.does_bpm_date_range_have_date_to"

        private const val KEY_BPM_DATE_RANGE_DATE_TO_INCLUSIVE =
            "statistics.records.bpm_date_range_date_to_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.records.does_note_count_date_range_have_date_from"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.records.note_count_date_range_date_from_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO =
            "statistics.records.does_note_count_date_range_have_date_to"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_INCLUSIVE =
            "statistics.records.note_count_date_range_date_to_inclusive"
    }

    /**
     * Records state.
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
         * Finishing work with the records.
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
         * Show actual subtitle for records.
         *
         * @property value ready-made subtitle value.
         */
        data class Subtitle(val value: Value) : Signal() {

            /**
             * Subtitle value for records.
             */
            enum class Value {
                BPM,
                NOTE_COUNT
            }
        }

        /**
         * Show actual date range.
         *
         * @property value date range.
         */
        data class ActualDateRange(val value: DateRange) : Signal()

        /**
         * Show actual records.
         */
        sealed class Records : Signal() {

            /**
             * BPM records.
             *
             * @property value ready-made BPM records.
             */
            data class Bpm(val value: BpmRecords) : Records()

            /**
             * Note count records.
             *
             * @property value ready-made note count records.
             */
            data class NoteCount(val value: NoteCountRecords) : Records()
        }
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
     * The date range over which records should be shown.
     */
    var dateRange: DateRange
        get() = requireNotNull(_dateRange)
        set(value) {
            if (value != _dateRange) {
                Timber.d("Change date range: old=$_dateRange, new=$value")
                _dateRange = value
                targetParameter?.let {
                    saveDateRange(value, it)
                }
                setState(State.Waiting)
                give(Signal.ActualDateRange(value))
                viewModelScope.launch {
                    aggregateStatistics(value)
                    setState(State.Working)
                }
            }
        }

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private var targetParameter: RecordsTargetParameter? = null
    private var _dateRange: DateRange? = null

    /**
     * Prepare for working with the records.
     *
     * @param targetParameter target parameter for calculating records.
     */
    fun prepare(targetParameter: RecordsTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        this.targetParameter = targetParameter
        _dateRange = loadDateRange(targetParameter)
        setState(State.Waiting)
        give(Signal.Subtitle(targetParameter.toSubtitleValue()))
        give(Signal.ActualDateRange(dateRange))
        recordsAggregator.clearCache()
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
        targetParameter: RecordsTargetParameter
    ): DateRange = when (targetParameter) {
        RecordsTargetParameter.BPM ->
            preferences.getBpmDateRange()
        RecordsTargetParameter.NOTE_COUNT ->
            preferences.getNoteCountDateRange()
    }

    private fun saveDateRange(
        dateRange: DateRange,
        targetParameter: RecordsTargetParameter
    ) {
        preferences.edit {
            when (targetParameter) {
                RecordsTargetParameter.BPM ->
                    putBpmDateRange(dateRange)
                RecordsTargetParameter.NOTE_COUNT ->
                    putNoteCountDateRange(dateRange)
            }
        }
    }

    private suspend fun aggregateStatistics(dateRange: DateRange) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val recordsSignal = when (targetParameter) {
                RecordsTargetParameter.BPM ->
                    Signal.Records.Bpm(
                        recordsAggregator.aggregateBpmRecords(
                            dateRange,
                            BPM_RECORDS_LIMIT
                        )
                    )
                RecordsTargetParameter.NOTE_COUNT ->
                    Signal.Records.NoteCount(
                        recordsAggregator.aggregateNoteCountRecords(
                            dateRange,
                            NOTE_COUNT_RECORDS_LIMIT
                        )
                    )
                null -> error("Target parameter is missing!")
            }
            give(recordsSignal)
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

    private fun RecordsTargetParameter.toSubtitleValue(): Signal.Subtitle.Value = when (this) {
        RecordsTargetParameter.BPM ->
            Signal.Subtitle.Value.BPM
        RecordsTargetParameter.NOTE_COUNT ->
            Signal.Subtitle.Value.NOTE_COUNT
    }

    private fun SharedPreferences.getBpmDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toInclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putBpmDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO,
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
