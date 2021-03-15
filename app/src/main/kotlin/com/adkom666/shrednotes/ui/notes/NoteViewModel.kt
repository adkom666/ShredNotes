package com.adkom666.shrednotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.TruncatedToMinutesDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Note screen model.
 *
 * @property noteRepository note storage management.
 */
@ExperimentalCoroutinesApi
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    companion object {
        private const val MESSAGE_CHANNEL_CAPACITY = 3
    }

    /**
     * Note state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         */
        object Waiting : State()

        /**
         * Initiate the necessary objects before interacting with the user.
         *
         * @property exerciseList [List] of all exercises.
         * @property noteDateTime date and time of training.
         * @property noteExerciseName training exercise name.
         * @property noteBpmString training BPM.
         */
        data class Init(
            val exerciseList: List<Exercise>,
            val noteDateTime: TruncatedToMinutesDate,
            val noteExerciseName: String?,
            val noteBpmString: String?
        ) : State()

        /**
         * Interacting with the user.
         */
        object Normal : State()

        /**
         * The date and time of the training have changed.
         *
         * @property noteDateTime new date and time of training.
         */
        data class NoteDateTimeChanged(val noteDateTime: TruncatedToMinutesDate) : State()

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
     * Date and time of training.
     */
    var noteDateTime: TruncatedToMinutesDate
        get() = requireNotNull(_noteDateTime)
        set(value) {
            if (value != _noteDateTime) {
                Timber.d("Set noteDateTime=$value")
                _noteDateTime = value
                setState(State.NoteDateTimeChanged(noteDateTime = value))
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

    private val initialNote: Note?
        get() = _initialNote

    private var _initialNote: Note? = null
    private var _noteDateTime: TruncatedToMinutesDate? = null
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    /**
     * All the necessary information to create a note along with the exercise.
     */
    private var noteToSaveWithExercise: Note? = null

    /**
     * Prepare for working with the [note].
     *
     * @param note initial note.
     */
    fun prepare(note: Note?) {
        Timber.d("Prepare: note=$note")
        _initialNote = note
        _noteDateTime = note?.dateTime ?: TruncatedToMinutesDate()
    }

    /**
     * Start working.
     */
    fun start() {
        Timber.d("Start")
        setState(State.Waiting)
        viewModelScope.launch {
            val exerciseList = exerciseRepository.allExercisesSuspending()
            setState(
                State.Init(
                    exerciseList,
                    noteDateTime,
                    initialNote?.exerciseName,
                    initialNote?.bpm?.toString()
                )
            )
        }
    }

    /**
     * Tell the model that all the required objects are initialized.
     */
    fun initiated() {
        Timber.d("Initiated")
        setState(State.Normal)
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
            noteExerciseName.isBlank() -> report(Message.Error.MissingNoteExerciseName)
            noteBpmString.isBlank() -> report(Message.Error.MissingNoteBpm)
            else -> try {
                val noteBpm = noteBpmString.toInt()
                if (isNoteBpmInvalid(noteBpm)) {
                    report(Message.Error.WrongNoteBpm)
                } else {
                    val note = createNote(noteExerciseName, noteBpm)
                    save(note)
                }
            } catch (e: NumberFormatException) {
                report(Message.Error.WrongNoteBpm)
            }
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
        initialNote -> setState(State.Declined)
        else -> {
            setState(State.Waiting)
            viewModelScope.launch {
                tryToSave(note)
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun saveWithExercise(note: Note) = when {
        note.exerciseName.isBlank() -> report(Message.Error.MissingNoteExerciseName)
        isNoteBpmInvalid(note.bpm) -> report(Message.Error.WrongNoteBpm)
        note == initialNote -> setState(State.Declined)
        else -> {
            setState(State.Waiting)
            viewModelScope.launch {
                tryToSaveWithExercise(note)
            }
        }
    }

    private suspend fun tryToSave(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            if (noteRepository.saveIfExerciseNamePresentSuspending(note, exerciseRepository)) {
                setState(State.Done)
            } else {
                noteToSaveWithExercise = note
                setState(State.Normal)
                report(Message.SuggestSaveWithExercise(note.exerciseName))
            }
        } catch (e: Exception) {
            setState(State.Normal)
            reportAbout(e)
        }
    }

    private suspend fun tryToSaveWithExercise(note: Note) {
        @Suppress("TooGenericExceptionCaught")
        try {
            noteRepository.saveWithExerciseSuspending(note, exerciseRepository)
            setState(State.Done)
        } catch (e: Exception) {
            setState(State.Normal)
            reportAbout(e)
        }
    }

    private fun createNote(noteExerciseName: String, noteBpm: Int): Note {
        return initialNote?.copy(
            dateTime = noteDateTime,
            exerciseName = noteExerciseName,
            bpm = noteBpm
        ) ?: Note(
            dateTime = noteDateTime,
            exerciseName = noteExerciseName,
            bpm = noteBpm
        )
    }

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

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }
}
