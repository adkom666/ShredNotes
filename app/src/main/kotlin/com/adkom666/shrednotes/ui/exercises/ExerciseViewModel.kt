package com.adkom666.shrednotes.ui.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.model.Exercise
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Exercise screen model.
 *
 * @property exerciseRepository exercise storage management.
 */
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    /**
     * Exercise state.
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
         * Finishing work with the exercise.
         */
        sealed class Finishing : State() {

            /**
             * Work with the exercise was declined.
             */
            object Declined : Finishing()

            /**
             * Work with the exercise is done.
             */
            object Done : Finishing()
        }
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
             * Exercise name is empty or blank.
             */
            object MissingExerciseName : Error()

            /**
             * Another exercise with [exerciseName] already exists.
             *
             * @property exerciseName duplicate name of exercise.
             */
            data class ExerciseAlreadyExists(val exerciseName: String) : Error()

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
         * Show exercise name.
         *
         * @property value ready-made exercise name.
         */
        data class ExerciseName(val value: String) : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

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

    private val initialExercise: Exercise?
        get() = _initialExercise

    private var _initialExercise: Exercise? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _signalChannel: Channel<Signal> = Channel(Channel.UNLIMITED)

    /**
     * Prepare for working with the [exercise].
     *
     * @param exercise initial exercise.
     */
    fun prepare(exercise: Exercise?) {
        Timber.d("Prepare: exercise=$exercise")
        _initialExercise = exercise
        initialExercise?.let { existentExercise ->
            give(Signal.ExerciseName(existentExercise.name))
        }
    }

    /**
     * Start working.
     */
    fun start() {
        Timber.d("Start")
        setState(State.Working)
    }

    /**
     * Save the current exercise under the [exerciseName]. Creating a new exercise or updating a
     * preset one.
     */
    fun save(exerciseName: String) {
        Timber.d("Save: exerciseName=$exerciseName")
        val exercise = createExercise(exerciseName)
        save(exercise)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun save(exercise: Exercise) = when {
        exercise == initialExercise ->
            setState(State.Finishing.Declined)
        exercise.name.isBlank() ->
            report(Message.Error.MissingExerciseName)
        else -> {
            setState(State.Waiting)
            viewModelScope.launch {
                tryToSave(exercise)
            }
        }
    }

    private suspend fun tryToSave(exercise: Exercise) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (exerciseRepository.saveIfNoSuchNameSuspending(exercise)) {
                setState(State.Finishing.Done)
            } else {
                setState(State.Working)
                report(Message.Error.ExerciseAlreadyExists(exercise.name))
            }
        } catch (e: Exception) {
            Timber.e(e)
            setState(State.Working)
            reportAbout(e)
        }
    }

    private fun createExercise(
        exerciseName: String
    ): Exercise = initialExercise?.copy(
        name = exerciseName
    ) ?: Exercise(
        name = exerciseName
    )

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
        _messageChannel.trySend(message)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.trySend(signal)
    }
}
