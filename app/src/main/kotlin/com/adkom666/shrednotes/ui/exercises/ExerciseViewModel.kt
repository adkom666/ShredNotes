package com.adkom666.shrednotes.ui.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.model.Exercise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Exercise screen model.
 *
 * @property exerciseRepository exercise storage management.
 */
@ExperimentalCoroutinesApi
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = 3
    }

    /**
     * Exercise state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         */
        object Waiting : State()

        /**
         * Initiate the necessary objects before interacting with the user.
         *
         * @property exerciseName ready-made exercise name.
         */
        data class Init(val exerciseName: String?) : State()

        /**
         * Interacting with the user.
         */
        object Normal : State()

        /**
         * Work with the exercise was declined.
         */
        object Declined : State()

        /**
         * Work with the exercise is done.
         */
        object Done : State()
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
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

    private val initialExercise: Exercise?
        get() = _initialExercise

    private var _initialExercise: Exercise? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    /**
     * Prepare for working with the [exercise].
     *
     * @param exercise initial exercise.
     */
    fun prepare(exercise: Exercise?) {
        Timber.d("Prepare: exercise=$exercise")
        _initialExercise = exercise
        setState(State.Init(initialExercise?.name))
    }

    /**
     * Tell the model that all the information received from it has been used.
     */
    fun ok() {
        Timber.d("OK!")
        setState(State.Normal)
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
        exercise == initialExercise -> setState(State.Declined)
        exercise.name.isBlank() -> report(Message.Error.MissingExerciseName)
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
                setState(State.Done)
            } else {
                setState(State.Normal)
                report(Message.Error.ExerciseAlreadyExists(exercise.name))
            }
        } catch (e: Exception) {
            setState(State.Normal)
            e.localizedMessage?.let {
                report(Message.Error.Clarified(it))
            } ?: report(Message.Error.Unknown)
        }
    }

    private fun createExercise(
        exerciseName: String
    ): Exercise = initialExercise?.copy(
        name = exerciseName
    ) ?: Exercise(
        name = exerciseName
    )

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }
}
