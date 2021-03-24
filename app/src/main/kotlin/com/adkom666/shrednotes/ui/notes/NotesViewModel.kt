package com.adkom666.shrednotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import androidx.paging.toLiveData
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.containsDifferentTrimmedTextIgnoreCaseThan
import com.adkom666.shrednotes.util.selection.ManageableSelection
import com.adkom666.shrednotes.util.selection.SelectableItems
import com.adkom666.shrednotes.util.selection.SelectionDashboard
import com.adkom666.shrednotes.util.selection.Selection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Notes section model.
 */
@ExperimentalCoroutinesApi
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private companion object {
        private const val PAGE_SIZE = 15
        private const val PAGED_LIST_INITIAL_LOAD_HINT = 30
        private const val PAGED_LIST_PREFETCH_DISTANCE = 15

        private const val NAVIGATION_CHANNEL_CAPACITY = 1
        private const val MESSAGE_CHANNEL_CAPACITY = 3
    }

    /**
     * Note list state.
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
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To the screen that allows you to add a new note.
         */
        object ToAddNoteScreen : NavDirection()

        /**
         * To the screen that allows you to update an existing note.
         *
         * @property note [Note] to update.
         */
        data class ToUpdateNoteScreen(val note: Note) : NavDirection()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about adding a note.
         */
        object Addition : Message()

        /**
         * Message about the note update.
         */
        object Update : Message()

        /**
         * Message about the count of deleted notes.
         *
         * @property count count of deleted notes.
         */
        data class Deletion(val count: Int) : Message()

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
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current list of notes in the UI thread.
     */
    val notePagedListAsLiveData: LiveData<PagedList<Note>>
        get() = noteSourceFactory.toLiveData(pagedListConfig)

    /**
     * Consume navigation directions from this channel in the UI thread.
     */
    val navigationChannel: ReceiveChannel<NavDirection>
        get() = _navigationChannel.openSubscription()

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

    /**
     * Notes to select.
     */
    val selectableNotes: SelectableItems
        get() = _manageableSelection

    /**
     * Tools for manipulating the selection.
     */
    val selectionDashboard: SelectionDashboard
        get() = _manageableSelection

    /**
     * Information about the presence of selected notes.
     */
    val selection: Selection
        get() = _manageableSelection

    /**
     * The text that must be contained in the names of the displayed notes' exercises
     * (case-insensitive).
     */
    var exerciseSubname: String? by Delegates.observable(null) { _, old, new ->
        Timber.d("Change exerciseSubname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            viewModelScope.launch {
                noteSourceFactory.exerciseSubname = new?.trim()
                val noteCount = noteRepository.countSuspending(new)
                _manageableSelection.reset(noteCount)
                invalidateNotes()
            }
        }
    }

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean by Delegates.observable(false) { _, old, new ->
        Timber.d("Change isSearchActive: old=$old, new=$new")
    }

    private val _manageableSelection: ManageableSelection = ManageableSelection()

    private val pagedListConfig: PagedList.Config = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(PAGE_SIZE)
        .setInitialLoadSizeHint(PAGED_LIST_INITIAL_LOAD_HINT)
        .setPrefetchDistance(PAGED_LIST_PREFETCH_DISTANCE)
        .build()

    private val noteSourceFactory: NoteSourceFactory = NoteSourceFactory(
        noteRepository,
        exerciseSubname
    )

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _navigationChannel: BroadcastChannel<NavDirection> =
        BroadcastChannel(NAVIGATION_CHANNEL_CAPACITY)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    init {
        Timber.d("Init")
        viewModelScope.launch {
            val noteInitialCount = noteRepository.countSuspending(exerciseSubname)
            Timber.d("noteInitialCount=$noteInitialCount")
            _manageableSelection.init(noteInitialCount)
            setState(State.Working)
            // Ignore initial value
            noteRepository.countFlow.drop(1).collect { noteCount ->
                Timber.d("Note list changed: noteCount=$noteCount")
                _manageableSelection.reset(noteCount)
                invalidateNotes()
            }
        }
    }

    /**
     * Start adding a new note.
     */
    fun addNote() {
        Timber.d("Add note")
        navigateTo(NavDirection.ToAddNoteScreen)
    }

    /**
     * Initiate the deletion of the selected notes.
     */
    fun deleteSelectedNotes() {
        Timber.d("Delete selected notes")
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val deletionCount = deleteSelectedNotes(_manageableSelection.state)
                setState(State.Working)
                report(Message.Deletion(deletionCount))
            } catch (e: Exception) {
                setState(State.Working)
                reportAbout(e)
            }
        }
    }

    /**
     * Handle the clicked note when the selection is inactive.
     */
    fun onNoteClick(note: Note) {
        Timber.d("onNoteClick: note=$note")
        navigateTo(NavDirection.ToUpdateNoteScreen(note))
    }

    /**
     * Call this method when the results of adding the note are available.
     *
     * @param isResultOk true if the note was added, false otherwise.
     */
    fun onAddNoteResult(isResultOk: Boolean) {
        Timber.d("onAddNoteResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Note has been added")
            report(Message.Addition)
        }
    }

    /**
     * Call this method when the results of the note update are available.
     *
     * @param isResultOk true if the note was updated, false otherwise.
     */
    fun onUpdateNoteResult(isResultOk: Boolean) {
        Timber.d("onUpdateNoteResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Note has been updated")
            report(Message.Update)
        }
    }

    private fun invalidateNotes() {
        setState(State.Waiting)
        noteSourceFactory.invalidate()
        setState(State.Working)
    }

    private suspend fun deleteSelectedNotes(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedNoteIdList = selectionState.selectedItemIdSet.toList()
            noteRepository.deleteSuspending(selectedNoteIdList, exerciseSubname)
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedNoteIdList = selectionState.unselectedItemIdSet.toList()
            noteRepository.deleteOtherSuspending(unselectedNoteIdList, exerciseSubname)
        }
        ManageableSelection.State.Inactive -> 0
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

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.offer(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }

    private class NoteSourceFactory(
        private val noteRepository: NoteRepository,
        var exerciseSubname: String?
    ) : DataSource.Factory<Int, Note>() {

        private var noteSource: NoteSource? = null

        override fun create(): DataSource<Int, Note> {
            val exerciseSource = NoteSource(noteRepository, exerciseSubname)
            this.noteSource = exerciseSource
            return exerciseSource
        }

        fun invalidate() {
            noteSource?.invalidate()
        }
    }

    private class NoteSource(
        private val noteRepository: NoteRepository,
        private var exerciseSubname: String?
    ) : PositionalDataSource<Note>() {

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<Note>
        ) {
            val (noteList, startPosition) = noteRepository.page(
                params.requestedLoadSize,
                params.requestedStartPosition,
                exerciseSubname
            )
            Timber.d("Load initial notes: noteList.size=${noteList.size}")
            Timber.d("Load initial notes: noteList=$noteList")
            callback.onResult(noteList, startPosition)
        }

        override fun loadRange(
            params: LoadRangeParams,
            callback: LoadRangeCallback<Note>
        ) {
            val noteList = noteRepository.list(
                params.loadSize,
                params.startPosition,
                exerciseSubname
            )
            Timber.d("Load next notes: noteList.size=${noteList.size}")
            callback.onResult(noteList)
        }
    }
}
