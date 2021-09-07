package com.adkom666.shrednotes.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.statistics.BpmRecords
import com.adkom666.shrednotes.statistics.NoteCountRecords
import com.adkom666.shrednotes.statistics.RecordsAggregator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import javax.inject.Inject

/**
 * Records screen model.
 *
 * @property recordsAggregator source of records.
 */
@ExperimentalCoroutinesApi
class RecordsViewModel @Inject constructor(
    private val recordsAggregator: RecordsAggregator
) : ViewModel() {

    private companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val BPM_RECORDS_LIMIT = 5
        private const val NOTE_COUNT_RECORDS_LIMIT = 5
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

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private var targetParameter: RecordsTargetParameter? = null

    /**
     * Prepare for working with the records.
     *
     * @param targetParameter target parameter for calculating records.
     */
    fun prepare(targetParameter: RecordsTargetParameter) {
        Timber.d("Prepare: targetParameter=$targetParameter")
        this.targetParameter = targetParameter
        setState(State.Waiting)
        give(Signal.Subtitle(targetParameter.toSubtitleValue()))
        recordsAggregator.clearCache()
    }

    /**
     * Start working.
     */
    suspend fun start() {
        Timber.d("Start")
        @Suppress("TooGenericExceptionCaught")
        try {
            val recordsSignal = when (targetParameter) {
                RecordsTargetParameter.BPM ->
                    Signal.Records.Bpm(
                        recordsAggregator.aggregateBpmRecords(BPM_RECORDS_LIMIT)
                    )
                RecordsTargetParameter.NOTE_COUNT ->
                    Signal.Records.NoteCount(
                        recordsAggregator.aggregateNoteCountRecords(NOTE_COUNT_RECORDS_LIMIT)
                    )
                null -> error("Target parameter is missing!")
            }
            give(recordsSignal)
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

    private fun RecordsTargetParameter.toSubtitleValue(): Signal.Subtitle.Value = when (this) {
        RecordsTargetParameter.BPM ->
            Signal.Subtitle.Value.BPM
        RecordsTargetParameter.NOTE_COUNT ->
            Signal.Subtitle.Value.NOTE_COUNT
    }
}
