package com.adkom666.shrednotes.ui.statistics

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import com.adkom666.shrednotes.statistics.CommonStatistics
import com.adkom666.shrednotes.statistics.CommonStatisticsAggregator
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.ExecutiveViewModel
import com.adkom666.shrednotes.util.getNullableDays
import com.adkom666.shrednotes.util.putNullableDays
import com.adkom666.shrednotes.util.time.Days
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import kotlin.properties.Delegates.observable

/**
 * Common statistics screen model.
 *
 * @property statisticsAggregator source of common statistics.
 * @property preferences project's [SharedPreferences].
 */
@ExperimentalTime
class CommonStatisticsViewModel
@Inject constructor(
    private val statisticsAggregator: CommonStatisticsAggregator,
    private val preferences: SharedPreferences
) : ExecutiveViewModel() {

    private companion object {

        private const val KEY_DOES_DATE_RANGE_HAVE_DATE_FROM =
            "statistics.common.does_date_range_have_date_from"

        private const val KEY_DATE_RANGE_DATE_FROM_INCLUSIVE =
            "statistics.common.date_range_date_from_inclusive"

        private const val KEY_DOES_DATE_RANGE_HAVE_DATE_TO =
            "statistics.common.does_date_range_have_date_to"

        private const val KEY_DATE_RANGE_DATE_TO_EXCLUSIVE =
            "statistics.common.date_range_date_to_exclusive"
    }

    /**
     * Common statistics state.
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
         * Finishing work with the common statistics.
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
         * Show actual date range.
         *
         * @property value date range.
         */
        data class ActualDateRange(val value: DateRange) : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current statistics in the UI thread.
     */
    val statisticsAsLiveData: LiveData<CommonStatistics?>
        get() = distinctUntilChanged(_statisticsAsLiveData)

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
     * The date range over which statistics should be shown.
     */
    var dateRange: DateRange by observable(
        preferences.getDateRange()
    ) { _, old, new ->
        Timber.d("Change date range: old=$old, new=$new")
        if (new != old) {
            preferences.edit {
                putDateRange(new)
            }
            execute {
                setState(State.Waiting)
                give(Signal.ActualDateRange(new))
                aggregateStatistics(new)
                setState(State.Working)
            }
        }
    }

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _statisticsAsLiveData: MutableLiveData<CommonStatistics?> = MutableLiveData(null)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _signalChannel: Channel<Signal> = Channel(Channel.UNLIMITED)

    /**
     * Prepare for working with the common statistics.
     */
    fun prepare() {
        Timber.d("Prepare")
        give(Signal.ActualDateRange(dateRange))
        statisticsAggregator.clearCache()
    }

    /**
     * Start working.
     */
    fun start() {
        Timber.d("Start")
        execute {
            setState(State.Waiting)
            aggregateStatistics(dateRange)
            setState(State.Working)
        }
    }

    /**
     * Call this method to handle the 'OK' button click.
     */
    fun onOkButtonClick() {
        Timber.d("On 'OK' button click")
        setState(State.Finishing)
    }

    private suspend fun aggregateStatistics(dateRange: DateRange) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val statistics = statisticsAggregator.aggregate(dateRange)
            setStatistics(statistics)
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

    private fun setStatistics(statistics: CommonStatistics) {
        Timber.d("Set statistics: statistics=$statistics")
        _statisticsAsLiveData.postValue(statistics)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.offer(signal)
    }

    private fun SharedPreferences.getDateRange(): DateRange {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_DATE_RANGE_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_DATE_RANGE_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        return DateRange(
            fromInclusive = dateFromInclusive,
            toExclusive = dateToExclusive
        )
    }

    private fun SharedPreferences.Editor.putDateRange(dateRange: DateRange) {
        putNullableDays(
            key = KEY_DATE_RANGE_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_DATE_RANGE_HAVE_DATE_FROM,
            value = dateRange.fromInclusive
        )
        putNullableDays(
            key = KEY_DATE_RANGE_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_DATE_RANGE_HAVE_DATE_TO,
            value = dateRange.toExclusive
        )
    }
}
