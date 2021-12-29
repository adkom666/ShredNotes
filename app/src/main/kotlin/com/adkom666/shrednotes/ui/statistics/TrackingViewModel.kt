package com.adkom666.shrednotes.ui.statistics

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.statistics.MaxBpmTracking
import com.adkom666.shrednotes.statistics.NoteCountTracking
import com.adkom666.shrednotes.statistics.TrackingAggregator
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
 * Statistics tracking screen model.
 *
 * @property trackingAggregator source of statistics tracking.
 * @property preferences project's [SharedPreferences].
 */
@ExperimentalCoroutinesApi
class TrackingViewModel @Inject constructor(
    private val trackingAggregator: TrackingAggregator,
    private val preferences: SharedPreferences,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.tracking.does_max_bpm_date_range_have_date_from"

        private const val KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.tracking.max_bpm_date_range_date_from_inclusive"

        private const val KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO =
            "statistics.tracking.does_max_bpm_date_range_have_date_to"

        private const val KEY_MAX_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE =
            "statistics.tracking.max_bpm_date_range_date_to_exclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.tracking.does_note_count_date_range_have_date_from"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.tracking.note_count_date_range_date_from_inclusive"

        private const val KEY_DOES_NOTE_COUNT_DATE_RANGE_HAVE_DATE_TO =
            "statistics.tracking.does_note_count_date_range_have_date_to"

        private const val KEY_NOTE_COUNT_DATE_RANGE_DATE_TO_EXCLUSIVE =
            "statistics.tracking.note_count_date_range_date_to_exclusive"
    }

    /**
     * Statistics tracking state.
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
         * Finishing work with the statistics tracking.
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
         * Show title for statistics tracking.
         *
         * @property value ready-made title value.
         */
        data class Title(val value: Value) : Signal() {

            /**
             * Title value for statistics tracking.
             */
            enum class Value {
                TRAINING_INTENSITY,
                PROGRESS
            }
        }

        /**
         * Show actual date range.
         *
         * @property value date range.
         */
        data class ActualDateRange(val value: DateRange) : Signal()

        /**
         * Use exercise list.
         *
         * @property value [List] of all exercises to select by user. If selected exercise is null
         * then notes for all exercises will be processed.
         */
        data class ExerciseList(val value: List<Exercise?>) : Signal()

        /**
         * Show actual statistics tracking.
         */
        sealed class ActualStatistics : Signal() {

            /**
             * Note count tracking.
             *
             * @property value ready-made note count tracking.
             */
            data class TrainingIntensity(val value: NoteCountTracking) : ActualStatistics()

            /**
             * Maximum BPM per day tracking.
             *
             * @property value ready-made maximum BPM per day tracking.
             */
            data class Progress(val value: MaxBpmTracking) : ActualStatistics()
        }
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

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
                    aggregateStatistics(value, exercise)
                    setState(State.Working)
                }
            }
        }

    /**
     * The exercise for which statistics should be shown. If value is null then notes for all
     * exercises will be processed.
     */
    var exercise: Exercise?
        get() = _exercise
        set(value) {
            if (value != _exercise) {
                Timber.d("Change exercise: old=$_exercise, new=$value")
                _exercise = value
                setState(State.Waiting)
                viewModelScope.launch {
                    aggregateStatistics(dateRange, value)
                    setState(State.Working)
                }
            }
        }

    private val targetParameter: TrackingTargetParameter
        get() = requireNotNull(_targetParameter)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private var _targetParameter: TrackingTargetParameter? = null
    private var _dateRange: DateRange? = null
    private var _exercise: Exercise? = null

    private var exerciseListCache: List<Exercise?>? = null

    /**
     * Prepare for working with the statistics tracking.
     *
     * @param targetParameter target parameter for calculating statistics.
     */
    fun prepare(targetParameter: TrackingTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        _targetParameter = targetParameter
        _dateRange = loadDateRange(targetParameter)
        _exercise = null
        give(Signal.Title(targetParameter.toTitleValue()))
        give(Signal.ActualDateRange(dateRange))
        trackingAggregator.clearCache()
    }

    /**
     * Start working.
     */
    suspend fun start() {
        Timber.d("Start")
        setState(State.Waiting)
        val exerciseList = exerciseListCache
            ?: exerciseRepository.listAllSuspending().let { exercises ->
                val nullableExercises = mutableListOf<Exercise?>()
                nullableExercises.add(null)
                nullableExercises.addAll(exercises)
                nullableExercises
            }.also { exerciseListCache = it }
        give(Signal.ExerciseList(exerciseList))
        aggregateStatistics(dateRange, exercise)
        setState(State.Working)
    }

    /**
     * Call this method to handle the 'OK' button click.
     */
    fun onOkButtonClick() {
        Timber.d("On 'OK' button click")
        setState(State.Finishing)
    }

    private fun loadDateRange(
        targetParameter: TrackingTargetParameter
    ): DateRange = when (targetParameter) {
        TrackingTargetParameter.MAX_BPM ->
            preferences.getMaxBpmDateRange()
        TrackingTargetParameter.NOTE_COUNT ->
            preferences.getNoteCountDateRange()
    }

    private fun saveDateRange(
        dateRange: DateRange,
        targetParameter: TrackingTargetParameter
    ) {
        preferences.edit {
            when (targetParameter) {
                TrackingTargetParameter.MAX_BPM ->
                    putMaxBpmDateRange(dateRange)
                TrackingTargetParameter.NOTE_COUNT ->
                    putNoteCountDateRange(dateRange)
            }
        }
    }

    private suspend fun aggregateStatistics(dateRange: DateRange, exercise: Exercise?) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val recordsSignal = when (targetParameter) {
                TrackingTargetParameter.MAX_BPM ->
                    Signal.ActualStatistics.Progress(
                        trackingAggregator.aggregateMaxBpmTracking(dateRange, exercise?.id)
                    )
                TrackingTargetParameter.NOTE_COUNT ->
                    Signal.ActualStatistics.TrainingIntensity(
                        trackingAggregator.aggregateNoteCountTracking(dateRange, exercise?.id)
                    )
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

    private fun TrackingTargetParameter.toTitleValue(): Signal.Title.Value =
        when (this) {
            TrackingTargetParameter.MAX_BPM ->
                Signal.Title.Value.PROGRESS
            TrackingTargetParameter.NOTE_COUNT ->
                Signal.Title.Value.TRAINING_INTENSITY
        }

    private fun SharedPreferences.getMaxBpmDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toExclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putMaxBpmDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_MAX_BPM_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_MAX_BPM_DATE_RANGE_HAVE_DATE_TO,
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
