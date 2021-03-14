package com.adkom666.shrednotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
         * The note list is ready for interaction.
         */
        object Ready : State()

        /**
         * Error when working with the list.
         */
        sealed class Error : State() {

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
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about the count of deleted notes.
         *
         * @param count count of deleted notes.
         */
        data class Deleted(val count: Int) : Message()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = _stateAsLiveData

    /**
     * Subscribe to the current list of notes in the UI thread.
     */
    val notePagedListAsLiveData: LiveData<PagedList<Note>>
        get() = noteSourceFactory.toLiveData(pagedListConfig)

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
    var isSearchActive: Boolean = false

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

    private val _messageChannel: BroadcastChannel<Message> = BroadcastChannel(
        MESSAGE_CHANNEL_CAPACITY
    )

    init {
        viewModelScope.launch {
            val noteInitialCount = noteRepository.countSuspending(exerciseSubname)
            Timber.d("noteInitialCount=$noteInitialCount")
            _manageableSelection.init(noteInitialCount)
            setState(State.Ready)
            // Ignore initial value
            noteRepository.countFlow.drop(1).collect { noteCount ->
                Timber.d("Note list changed: noteCount=$noteCount")
                _manageableSelection.reset(noteCount)
                invalidateNotes()
            }
        }
    }

    /**
     * Initiate the deletion of the selected notes.
     */
    fun deleteSelectedNotes() {
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val deletionCount = deleteSelectedNotes(_manageableSelection.state)
                setState(State.Ready)
                _messageChannel.offer(Message.Deleted(deletionCount))
            } catch (e: Exception) {
                e.localizedMessage?.let {
                    setState(State.Error.Clarified(it))
                } ?: setState(State.Error.Unknown)
            }
        }
    }

    private fun invalidateNotes() {
        setState(State.Waiting)
        noteSourceFactory.invalidate()
        setState(State.Ready)
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

    private fun setState(state: State) {
        _stateAsLiveData.value = state
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
