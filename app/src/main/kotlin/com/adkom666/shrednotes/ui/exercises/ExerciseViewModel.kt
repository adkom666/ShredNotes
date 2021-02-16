package com.adkom666.shrednotes.ui.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.model.Exercise
import kotlinx.coroutines.launch
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
         * The exercise is ready for interaction.
         *
         * @param exercise ready-made exercise.
         */
        data class Ready(val exercise: Exercise) : State()

        /**
         * Error when working with the exercise.
         */
        sealed class Error : State() {

            /**
             * Exercise name is empty or blank.
             */
            object MissingExerciseName : Error()

            /**
             * Another exercise with [exerciseName] already exists.
             *
             * @param exerciseName duplicate name of exercise.
             */
            data class ExerciseAlreadyExists(val exerciseName: String) : Error()

            /**
             * The details of this error are described in the [message].
             *
             * @param message the details of the error.
             */
            data class Clarified(val message: String) : Error()

            /**
             * Unknown error.
             */
            object Unknown : Error()
        }

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
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = _stateAsLiveData

    private var exercise: Exercise
        get() = requireNotNull(_exercise)
        set(value) {
            _exercise = value
        }

    private val initialExercise: Exercise?
        get() = _initialExercise

    private var _initialExercise: Exercise? = null
    private var _exercise: Exercise? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    /**
     * Prepare working with the [exercise].
     *
     * @param exercise initial exercise.
     */
    fun prepare(exercise: Exercise?) {
        _initialExercise = exercise
        _exercise = exercise ?: Exercise()
    }

    /**
     * Start working with the exercise.
     */
    fun start() {
        setState(State.Ready(this.exercise))
    }

    /**
     * Save the current exercise under the [exerciseName]. Creating a new exercise or updating a
     * preset one.
     */
    fun save(exerciseName: String) {
        exercise = exercise.copy(name = exerciseName)
        if (exercise == initialExercise) {
            setState(State.Declined)
            return
        }
        if (exercise.name.isEmpty()) {
            setState(State.Error.MissingExerciseName)
            return
        }
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                if (exerciseRepository.saveIfNoSuchNameSuspending(exercise)) {
                    setState(State.Done)
                } else {
                    setState(State.Error.ExerciseAlreadyExists(exercise.name))
                }
            } catch (e: Exception) {
                e.localizedMessage?.let {
                    setState(State.Error.Clarified(it))
                } ?: setState(State.Error.Unknown)
            }
        }
    }

    private fun setState(state: State) {
        _stateAsLiveData.value = state
    }
}
