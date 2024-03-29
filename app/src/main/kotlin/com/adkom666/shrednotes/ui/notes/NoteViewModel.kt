package com.adkom666.shrednotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.ExecutiveViewModel
import com.adkom666.shrednotes.util.numberOrNull
import com.adkom666.shrednotes.util.time.Minutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Note screen model.
 *
 * @property noteRepository note storage management.
 * @property exerciseRepository exercise storage management.
 */
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val exerciseRepository: ExerciseRepository
) : ExecutiveViewModel() {

    /**
     * Note state.
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
         * Finishing work with the note.
         */
        sealed class Finishing : State() {

            /**
             * Work with the note was declined.
             */
            object Declined : Finishing()

            /**
             * Work with the note is done.
             */
            object Done : Finishing()
        }
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Suggest creating an exercise named [noteExerciseName].
         *
         * @property noteExerciseName name of the training exercise to create.
         */
        data class SuggestSaveWithExercise(val noteExerciseName: String) : Message()

        /**
         * Error message.
         */
        sealed class Error : Message() {

            /**
             * Note exercise name is empty or blank.
             */
            object MissingNoteExerciseName : Error()

            /**
             * Note BPM is empty or blank.
             */
            object MissingNoteBpm : Error()

            /**
             * Note BPM is not a number between NOTE_BPM_MIN and NOTE_BPM_MAX.
             */
            object WrongNoteBpm : Error()

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
         * Show actual date and time of training.
         *
         * @property value date and time of training.
         */
        data class ActualNoteDateTime(val value: Minutes) : Signal()

        /**
         * Show training exercise name.
         *
         * @property value training exercise name.
         */
        data class NoteExerciseName(val value: String) : Signal()

        /**
         * Show training BPM.
         *
         * @property value training BPM.
         */
        data class NoteBpmString(val value: String) : Signal()
    }

    /**
     * Date and time of training.
     */
    var noteDateTime: Minutes
        get() = requireNotNull(_noteDateTime)
        set(value) {
            if (value != _noteDateTime) {
                Timber.d("Set noteDateTime=$value")
                _noteDateTime = value
                give(Signal.ActualNoteDateTime(value))
            }
        }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current exercises list in the UI thread.
     */
    val exercisesAsLiveData: LiveData<List<Exercise>?>
        get() = distinctUntilChanged(_exercisesAsLiveData)

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

    private val initialNote: Note?
        get() = _initialNote

    private var _initialNote: Note? = null
    private var _noteDateTime: Minutes? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _exercisesAsLiveData: MutableLiveData<List<Exercise>?> = MutableLiveData(null)
    private val _messageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private val _signalChannel: Channel<Signal> = Channel(Channel.UNLIMITED)

    /**
     * All the necessary information to create a note along with the exercise.
     */
    private var noteToSaveWithExercise: Note? = null

    private var exerciseListCache: List<Exercise>? = null

    /**
     * Prepare for working with the [note].
     *
     * @param note initial note.
     */
    fun prepare(note: Note?) {
        Timber.d("Prepare: note=$note")
        _initialNote = note
        _noteDateTime = note?.dateTime ?: Minutes()
        give(Signal.ActualNoteDateTime(noteDateTime))
        initialNote?.let { existentNote ->
            give(Signal.NoteExerciseName(existentNote.exerciseName))
            give(Signal.NoteBpmString(existentNote.bpm.toString()))
        }
    }

    /**
     * Start working.
     */
    fun start() {
        execute {
            Timber.d("Start")
            setState(State.Waiting)
            val exerciseList = exerciseListCache
                ?: exerciseRepository.listAllSuspending()
                    .also { exerciseListCache = it }
            setExercises(exerciseList)
            setState(State.Working)
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
    ) {
        Timber.d(
            """Save:
                |noteExerciseName=$noteExerciseName,
                |noteBpmString=$noteBpmString""".trimMargin()
        )
        when {
            noteExerciseName.isBlank() ->
                report(Message.Error.MissingNoteExerciseName)
            noteBpmString.isBlank() ->
                report(Message.Error.MissingNoteBpm)
            else -> noteBpmString.numberOrNull()?.let { noteBpm ->
                if (isNoteBpmInvalid(noteBpm)) {
                    report(Message.Error.WrongNoteBpm)
                } else {
                    val note = createNote(noteExerciseName, noteBpm)
                    save(note)
                }
            } ?: report(Message.Error.WrongNoteBpm)
        }
    }

    /**
     * Initiate saving a note with the creation of an exercise with the name specified in the note.
     */
    fun acceptSaveWithExercise() {
        Timber.d("Accept saving a note with the creation of an exercise")
        Timber.d("noteToSaveWithExercise=$noteToSaveWithExercise")
        noteToSaveWithExercise?.let { saveWithExercise(it) }
        noteToSaveWithExercise = null
    }

    /**
     * Decline saving a note with the creation of an exercise.
     */
    fun declineSaveWithExercise() {
        Timber.d("Decline saving a note with the creation of an exercise")
        noteToSaveWithExercise = null
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun save(note: Note) = when (note) {
        initialNote ->
            setState(State.Finishing.Declined)
        else ->
            execute {
                setState(State.Waiting)
                tryToSave(note)
            }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun saveWithExercise(note: Note) = when {
        note.exerciseName.isBlank() ->
            report(Message.Error.MissingNoteExerciseName)
        isNoteBpmInvalid(note.bpm) ->
            report(Message.Error.WrongNoteBpm)
        note == initialNote ->
            setState(State.Finishing.Declined)
        else ->
            execute {
                setState(State.Waiting)
                tryToSaveWithExercise(note)
            }
    }

    private suspend fun tryToSave(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (noteRepository.saveIfExerciseNamePresentSuspending(note, exerciseRepository)) {
                setState(State.Finishing.Done)
            } else {
                noteToSaveWithExercise = note
                setState(State.Working)
                report(Message.SuggestSaveWithExercise(note.exerciseName))
            }
        } catch (e: Exception) {
            Timber.e(e)
            setState(State.Working)
            reportAbout(e)
        }
    }

    private suspend fun tryToSaveWithExercise(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            noteRepository.saveWithExerciseSuspending(note, exerciseRepository)
            setState(State.Finishing.Done)
        } catch (e: Exception) {
            Timber.e(e)
            setState(State.Working)
            reportAbout(e)
        }
    }

    private fun createNote(
        noteExerciseName: String,
        noteBpm: Int
    ): Note = initialNote?.copy(
        dateTime = noteDateTime,
        exerciseName = noteExerciseName,
        bpm = noteBpm
    ) ?: Note(
        dateTime = noteDateTime,
        exerciseName = noteExerciseName,
        bpm = noteBpm
    )

    private fun isNoteBpmInvalid(bpm: Int): Boolean {
        return bpm < NOTE_BPM_MIN || NOTE_BPM_MAX < bpm
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

    private fun setExercises(exercises: List<Exercise>) {
        Timber.d("Set exercises: exercises=$exercises")
        _exercisesAsLiveData.postValue(exercises)
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
