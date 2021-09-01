package com.adkom666.shrednotes.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.statistics.WeekdaysStatistics
import com.adkom666.shrednotes.statistics.WeekdaysStatisticsAggregator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import javax.inject.Inject

/**
 * Statistics by days of week screen model.
 */
@ExperimentalCoroutinesApi
class WeekdaysStatisticsViewModel @Inject constructor(
    private val statisticsAggregator: WeekdaysStatisticsAggregator
) : ViewModel() {

    private companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED
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
         * Show actual subtitle for statistics by days of week.
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
         * Show actual statistics by days of week.
         *
         * @property value ready-made statistics by days of week.
         */
        data class Statistics(val value: WeekdaysStatistics) : Signal()
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

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private var targetParameter: WeekdaysStatisticsTargetParameter? = null

    /**
     * Prepare for working with the statistics by days of week.
     *
     * @param targetParameter target parameter for calculating statistics.
     */
    fun prepare(targetParameter: WeekdaysStatisticsTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        this.targetParameter = targetParameter
        setState(State.Waiting)
        give(Signal.Subtitle(targetParameter.toSubtitleValue()))
        statisticsAggregator.clearCache()
    }

    /**
     * Start working.
     */
    suspend fun start() {
        Timber.d("Start")
        @Suppress("TooGenericExceptionCaught")
        try {
            val statistics = when (targetParameter) {
                WeekdaysStatisticsTargetParameter.MAX_BPM ->
                    statisticsAggregator.aggregateAverageAmongMaxBpm()
                WeekdaysStatisticsTargetParameter.NOTE_COUNT ->
                    statisticsAggregator.aggregateAverageNoteCount()
                null -> error("Target parameter is missing!")
            }
            give(Signal.Statistics(statistics))
            setState(State.Working)
        } catch (e: Exception) {
            Timber.e(e)
            setState(State.Working)
            reportAbout(e)
        }
    }

    /**
     * Call this method to handle the OK button click.
     */
    fun onOkButtonClick() {
        Timber.d("On OK button click")
        setState(State.Finishing)
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
}
