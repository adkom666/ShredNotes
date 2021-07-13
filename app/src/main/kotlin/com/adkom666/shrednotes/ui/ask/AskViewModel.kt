package com.adkom666.shrednotes.ui.ask

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Ask section model.
 */
class AskViewModel @Inject constructor() : ViewModel() {

    /**
     * Ask state.
     */
    sealed class State {

        /**
         * Loading donation value.
         */
        object Loading : State()

        /**
         * Prepare for interacting with the user and tell me by [ok] call.
         *
         * @property price money to donate.
         */
        data class Preparation(val price: String) : State()

        /**
         * Waiting for the donation.
         */
        object Asking : State()

        /**
         * Donation in action.
         */
        object Donation : State()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Loading)

    init {
        Timber.d("Init")
        setState(State.Loading)
        viewModelScope.launch {
            delay(1666L)
            setState(State.Preparation("$666.666"))
        }
    }

    /**
     * Tell the model that all the information received from it has been used.
     */
    fun ok() {
        Timber.d("OK!")
        setState(State.Asking)
    }

    /**
     * Initiate donation.
     */
    fun donate() {
        Timber.d("Donate")
        setState(State.Donation)
        viewModelScope.launch {
            delay(6666L)
            setState(State.Asking)
        }
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }
}
