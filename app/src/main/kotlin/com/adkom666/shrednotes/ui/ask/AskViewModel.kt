package com.adkom666.shrednotes.ui.ask

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.ask.DonationResult
import com.adkom666.shrednotes.ask.Donor
import com.adkom666.shrednotes.ask.OnDonationFinishListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Ask section model.
 *
 * @property donor donor of money.
 */
@ExperimentalCoroutinesApi
class AskViewModel @Inject constructor(
    private val donor: Donor
) : ViewModel() {

    private companion object {
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED
    }

    /**
     * Ask state.
     */
    sealed class State {

        /**
         * Loading donation value.
         */
        object Loading : State()

        /**
         * Donation is not available, because it price is unknown.
         */
        object UnknownDonationPrice : State()

        /**
         * Waiting for the donation.
         */
        object Asking : State()

        /**
         * Donation is not available, but all information is displayed (possibly donation in
         * action).
         */
        object NotAsking : State()
    }

    /**
     * Information signal.
     */
    sealed class Signal {

        /**
         * Show money to donate.
         *
         * @property value message text about money to donate.
         */
        data class DonationPrice(val value: String) : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Consume information signals from this channel in the UI thread.
     */
    val signalChannel: ReceiveChannel<Signal>
        get() = _signalChannel.openSubscription()

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Loading)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    private val onDonationFinishListener: OnDonationFinishListener =
        object : OnDonationFinishListener {

            override fun onDonationFinish(donationResult: DonationResult) {
                Timber.d("Donation finished: donationResult=$donationResult")
                if (donationResult == DonationResult.DISPOSED) {
                    setState(State.UnknownDonationPrice)
                } else {
                    setState(State.Asking)
                }
            }
        }

    override fun onCleared() {
        super.onCleared()
        donor.setDisposableOnDonationFinishListener(null)
    }

    /**
     * Call this method before start work to set the correct state and donation price.
     *
     * @param context [Context].
     */
    fun prepareDonor(context: Context) {
        Timber.d("Prepare donor")
        if (donor.isPrepared) {
            invalidateStateAndPrice()
            if (donor.isActive) {
                donor.setDisposableOnDonationFinishListener(onDonationFinishListener)
            }
        } else {
            forcePrepareDonor(context)
        }
    }

    /**
     * Call this method to start work to set the correct state and donation price if [prepareDonor]
     * failed and the state is [State.UnknownDonationPrice], respectively.
     *
     * @param context [Context].
     */
    fun forcePrepareDonor(context: Context) {
        Timber.d("Force prepare donor")
        setState(State.Loading)
        viewModelScope.launch {
            donor.prepare(context)
            invalidateStateAndPrice()
        }
    }

    /**
     * Initiate donation.
     *
     * @param activity an activity reference from which the donation will be launched.
     */
    fun donate(activity: Activity) {
        Timber.d("Donate")
        setState(State.NotAsking)
        donor.setDisposableOnDonationFinishListener(onDonationFinishListener)
        viewModelScope.launch {
            donor.donate(activity)
        }
    }

    private fun invalidateStateAndPrice() {
        donor.price?.let { price ->
            give(Signal.DonationPrice(price))
            if (donor.isActive) {
                setState(State.NotAsking)
            } else {
                setState(State.Asking)
            }
        } ?: setState(State.UnknownDonationPrice)
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.offer(signal)
    }
}
