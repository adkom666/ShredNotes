package com.adkom666.shrednotes.ui.statistics

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import com.adkom666.shrednotes.statistics.BpmRecords
import com.adkom666.shrednotes.statistics.NoteCountRecords
import com.adkom666.shrednotes.statistics.RecordsAggregator
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.ExecutiveViewModel
import com.adkom666.shrednotes.util.getNullableDays
import com.adkom666.shrednotes.util.putNullableDays
import com.adkom666.shrednotes.util.time.Days
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates.observable

/**
 * Records screen model.
 *
 * @property recordsAggregator source of records.
 * @property preferences project's [SharedPreferences].
 */
class RecordsViewModel @Inject constructor(
    private val recordsAggregator: RecordsAggregator,
    private val preferences: SharedPreferences
) : ExecutiveViewModel() {

    private companion object {

        private const val RECORDS_PORTION = 3

        private const val KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.records.does_bpm_date_range_have_date_from"

        private const val KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.records.bpm_date_range_date_from_inclusive"

        private const val KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO =
            "statistics.records.does_bpm_date_range_have_date_to"

        private const val KEY_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE =
            "statistics.records.bpm_date_range_date_to_exclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.records.does_note_count_date_range_have_date_from"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.records.note_count_date_range_date_from_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO =
            "statistics.records.does_note_count_date_range_have_date_to"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_EXCLUSIVE =
            "statistics.records.note_count_date_range_date_to_exclusive"
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
         *
         * @property isUiLocked true if the user interface should be locked.
         */
        data class Working(val isUiLocked: Boolean) : State()

        /**
         * Finishing work with the records.
         */
        object Finishing : State()
    }

    /**
     * Wrapped records.
     */
    sealed class Records {

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
         * Show subtitle for records.
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
         * Show if there are more records.
         *
         * @property value true if more records are present.
         */
        data class HasMoreRecords(val value: Boolean) : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current records in the UI thread.
     */
    val recordsAsLiveData: LiveData<Records?>
        get() = distinctUntilChanged(_recordsAsLiveData)

    /**
     * Collect information messages from this flow in the UI thread.
     */
    val messageFlow: Flow<Message>
        get() = _messageChannel.receiveAsFlow()

    /**
     * Collect information signals from this flow in the UI thread.
     */
    val signalFlow: Flow<Signal>
        get() = _signalChannel.receiveAsFlow()

    /**
     * The date range over which records should be shown.
     */
    var dateRange: DateRange
        get() = requireNotNull(_dateRange)
        set(value) {
            if (value != _dateRange) {
                Timber.d("Change date range: old=$_dateRange, new=$value")
                _dateRange = value
                saveDateRange(value, targetParameter)
                execute {
                    setState(State.Working(isUiLocked = true))
                    give(Signal.ActualDateRange(value))
                    accumulatedBpmRecords = BpmRecords(emptyList())
                    accumulatedNoteCountRecords = NoteCountRecords(emptyList())
                    readRecordCount(value)
                    val isSuccess = accumulateRecords(
                        dateRange = value,
                        startPosition = 0,
                        limit = recordPosition
                    )
                    if (isSuccess) {
                        publishRecords()
                    }
                    setState(State.Working(isUiLocked = false))
                }
            }
        }

    /**
     * True if not all records have been loaded.
     */
    var areMoreRecordsPresent: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                give(Signal.HasMoreRecords(value))
            }
        }

    private val targetParameter: RecordsTargetParameter
        get() = requireNotNull(_targetParameter)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _recordsAsLiveData: MutableLiveData<Records?> = MutableLiveData(null)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _signalChannel: Channel<Signal> = Channel(Channel.UNLIMITED)

    private var _targetParameter: RecordsTargetParameter? = null
    private var _dateRange: DateRange? = null

    private var recordPosition: Int by observable(0) { _, old, new ->
        Timber.d("Change recordPosition: old=$old, new=$new")
        if (new != old) {
            invalidateMoreRecordsPresence()
        }
    }

    private var recordCount: Int? by observable(null) { _, old, new ->
        Timber.d("Change recordCount: old=$old, new=$new")
        if (new != old) {
            invalidateMoreRecordsPresence()
        }
    }

    private var accumulatedBpmRecords: BpmRecords = BpmRecords(emptyList())
    private var accumulatedNoteCountRecords: NoteCountRecords = NoteCountRecords(emptyList())

    /**
     * Prepare for working with the records.
     *
     * @param targetParameter target parameter for calculating records.
     */
    fun prepare(targetParameter: RecordsTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        _targetParameter = targetParameter
        _dateRange = loadDateRange(targetParameter)
        give(Signal.Subtitle(targetParameter.toSubtitleValue()))
        give(Signal.ActualDateRange(dateRange))
        recordsAggregator.clearCache()
        execute {
            setState(State.Waiting)
            recordPosition = 0
            readRecordCount(dateRange)
            val isSuccess = accumulateRecords(
                dateRange = dateRange,
                startPosition = recordPosition,
                limit = RECORDS_PORTION
            )
            if (isSuccess) {
                publishRecords()
                recordPosition += RECORDS_PORTION
            }
            setState(State.Working(isUiLocked = false))
        }
    }

    /**
     * Call this method to handle the 'OK' button click.
     */
    fun onOkButtonClick() {
        Timber.d("On 'OK' button click")
        setState(State.Finishing)
    }

    /**
     * Call this method to handle the 'More' button click.
     */
    fun onMoreButtonClick() {
        Timber.d("On 'More' button click")
        execute {
            setState(State.Working(isUiLocked = true))
            val isSuccess = accumulateRecords(
                dateRange = dateRange,
                startPosition = recordPosition,
                limit = RECORDS_PORTION
            )
            if (isSuccess) {
                publishRecords()
                recordPosition += RECORDS_PORTION
            }
            setState(State.Working(isUiLocked = false))
        }
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

    private suspend fun readRecordCount(dateRange: DateRange) {
        @Suppress("TooGenericExceptionCaught")
        try {
            recordCount = when (targetParameter) {
                RecordsTargetParameter.BPM ->
                    recordsAggregator.bpmRecordCount(dateRange)
                RecordsTargetParameter.NOTE_COUNT ->
                    recordsAggregator.noteCountRecordCount(dateRange)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun accumulateRecords(
        dateRange: DateRange,
        startPosition: Int,
        limit: Int
    ): Boolean {
        @Suppress("TooGenericExceptionCaught")
        return try {
            accumulateRecordsUnsafe(
                dateRange = dateRange,
                startPosition = startPosition,
                limit = limit
            )
            true
        } catch (e: Exception) {
            Timber.e(e)
            reportAbout(e)
            false
        }
    }

    private suspend fun accumulateRecordsUnsafe(
        dateRange: DateRange,
        startPosition: Int,
        limit: Int
    ) {
        when (targetParameter) {
            RecordsTargetParameter.BPM ->
                accumulatedBpmRecords += recordsAggregator.aggregateBpmRecords(
                    dateRange = dateRange,
                    startPosition = startPosition,
                    limit = limit
                )
            RecordsTargetParameter.NOTE_COUNT ->
                accumulatedNoteCountRecords += recordsAggregator.aggregateNoteCountRecords(
                    dateRange = dateRange,
                    startPosition = startPosition,
                    limit = limit
                )
        }
    }

    private fun publishRecords() {
        val records = when (targetParameter) {
            RecordsTargetParameter.BPM ->
                Records.Bpm(accumulatedBpmRecords)
            RecordsTargetParameter.NOTE_COUNT ->
                Records.NoteCount(accumulatedNoteCountRecords)
        }
        setRecords(records)
    }

    private fun invalidateMoreRecordsPresence() {
        areMoreRecordsPresent = recordCount?.let { recordPosition < it } ?: false
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

    private fun setRecords(records: Records) {
        Timber.d("Set records: records=$records")
        _recordsAsLiveData.postValue(records)
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

    private operator fun BpmRecords.plus(other: BpmRecords): BpmRecords {
        return BpmRecords(topNotes = this.topNotes + other.topNotes)
    }

    private operator fun NoteCountRecords.plus(other: NoteCountRecords): NoteCountRecords {
        return NoteCountRecords(topExerciseNames = this.topExerciseNames + other.topExerciseNames)
    }

    private fun SharedPreferences.getBpmDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toExclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putBpmDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_BPM_DATE_RANGE_HAVE_DATE_TO,
            value = dateRange.toExclusive
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
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toExclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putNoteCountDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO,
            value = dateRange.toExclusive
        )
    }
}
