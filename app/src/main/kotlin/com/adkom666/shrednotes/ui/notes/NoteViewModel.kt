package com.adkom666.shrednotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * Note screen model.
 *
 * @property noteRepository note storage management.
 */
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    /**
     * Note state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         */
        object Waiting : State()

        /**
         * The note is ready for interaction.
         *
         * @param exerciseList [List] of all exercises.
         * @param note ready-made note.
         */
        data class Ready(val exerciseList: List<Exercise>, val note: Note?) : State()

        /**
         * Error when working with the note.
         */
        sealed class Error : State() {

            /**
             * Note exercise name is empty or blank.
             */
            object MissingNoteExerciseName : Error()

            /**
             * Note BPM is empty or blank.
             */
            object MissingNoteBpm : Error()

            /**
             * Note BPM is not a number between 13 and 666.
             */
            object WrongNoteBpm : Error()

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
         * Consider creating an exercise with the name described in [note] to save the note.
         *
         * @property note all the necessary information to create a note along with the exercise.
         */
        data class ConsiderSaveWithExercise(val note: Note) : State()

        /**
         * Work with the note was declined.
         */
        object Declined : State()

        /**
         * Work with the note is done.
         */
        object Done : State()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = _stateAsLiveData

    private val initialNote: Note?
        get() = _initialNote

    private var _initialNote: Note? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    /**
     * Start working with the [note].
     *
     * @param note initial note.
     */
    fun start(note: Note?) {
        _initialNote = note
        viewModelScope.launch {
            val exerciseList = exerciseRepository.allExercisesSuspending()
            setState(State.Ready(exerciseList, initialNote))
        }
    }

    /**
     * Initiate a note storing.
     *
     * @param noteExerciseName training exercise name.
     * @param noteBpmString training BPM.
     */
    fun save(
        noteExerciseName: String,
        noteBpmString: String
    ) = when {
        noteExerciseName.isBlank() -> setState(State.Error.MissingNoteExerciseName)
        noteBpmString.isBlank() -> setState(State.Error.MissingNoteBpm)
        else -> try {
            val noteBpm = noteBpmString.toInt()
            if (isNoteBpmInvalid(noteBpm)) {
                setState(State.Error.WrongNoteBpm)
            } else {
                val note = createNote(noteExerciseName, noteBpm)
                save(note)
            }
        } catch (e: NumberFormatException) {
            setState(State.Error.WrongNoteBpm)
        }
    }

    /**
     * Initiate saving a note with the creation of an exercise with the name specified in the note.
     *
     * @param note [Note] to save.
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun saveWithExercise(note: Note) = when {
        note.exerciseName.isBlank() -> setState(State.Error.MissingNoteExerciseName)
        isNoteBpmInvalid(note.bpm) -> setState(State.Error.WrongNoteBpm)
        note == initialNote -> setState(State.Declined)
        else -> {
            setState(State.Waiting)
            viewModelScope.launch {
                tryToSaveWithExercise(note)
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun save(note: Note) = when (note) {
        initialNote -> setState(State.Declined)
        else -> {
            setState(State.Waiting)
            viewModelScope.launch {
                tryToSave(note)
            }
        }
    }

    private suspend fun tryToSave(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (noteRepository.saveIfExerciseNamePresentSuspending(note, exerciseRepository)) {
                setState(State.Done)
            } else {
                setState(State.ConsiderSaveWithExercise(note))
            }
        } catch (e: Exception) {
            handle(e)
        }
    }

    private suspend fun tryToSaveWithExercise(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            noteRepository.saveWithExerciseSuspending(note, exerciseRepository)
            setState(State.Done)
        } catch (e: Exception) {
            handle(e)
        }
    }

    private fun createNote(noteExerciseName: String, noteBpm: Int): Note {
        return initialNote?.copy(
            exerciseName = noteExerciseName,
            bpm = noteBpm
        ) ?: Note(
            dateTime = Date(),
            exerciseName = noteExerciseName,
            bpm = noteBpm
        )
    }

    private fun isNoteBpmInvalid(bpm: Int): Boolean {
        return bpm < NOTE_BPM_MIN || NOTE_BPM_MAX < bpm
    }

    private fun handle(e: Exception) {
        e.localizedMessage?.let {
            setState(State.Error.Clarified(it))
        } ?: setState(State.Error.Unknown)
    }

    private fun setState(state: State) {
        _stateAsLiveData.value = state
    }
}
