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
         * @property exerciseName ready-made exercise name.
         */
        data class Ready(val exerciseName: String?) : State()

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
             * @property exerciseName duplicate name of exercise.
             */
            data class ExerciseAlreadyExists(val exerciseName: String) : Error()

            /**
             * The details of this error are described in the [message].
             *
             * @property message the details of the error.
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

    private val initialExercise: Exercise?
        get() = _initialExercise

    private var _initialExercise: Exercise? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    /**
     * Start working with the [exercise].
     *
     * @param exercise initial exercise.
     */
    fun start(exercise: Exercise?) {
        _initialExercise = exercise
        setState(State.Ready(initialExercise?.name))
    }

    /**
     * Save the current exercise under the [exerciseName]. Creating a new exercise or updating a
     * preset one.
     */
    fun save(exerciseName: String) {
        val exercise = initialExercise?.copy(
            name = exerciseName
        ) ?: Exercise(
            name = exerciseName
        )
        saveIfValid(exercise)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun saveIfValid(exercise: Exercise) = when {
        exercise == initialExercise -> setState(State.Declined)
        exercise.name.isBlank() -> setState(State.Error.MissingExerciseName)
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
                setState(State.Error.ExerciseAlreadyExists(exercise.name))
            }
        } catch (e: Exception) {
            e.localizedMessage?.let {
                setState(State.Error.Clarified(it))
            } ?: setState(State.Error.Unknown)
        }
    }

    private fun setState(state: State) {
        _stateAsLiveData.value = state
    }
}
