package com.adkom666.shrednotes.ui.notes

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.liveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.data.pref.NoteToolPreferences
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.getNullableDays
import com.adkom666.shrednotes.util.getNullableInt
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.putNullableDays
import com.adkom666.shrednotes.util.putNullableInt
import com.adkom666.shrednotes.util.selection.ManageableSelection
import com.adkom666.shrednotes.util.selection.OnActivenessChangeListener
import com.adkom666.shrednotes.util.selection.SelectableItems
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.selection.SelectionDashboard
import com.adkom666.shrednotes.util.time.Days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates.observable

/**
 * Notes section model.
 *
 * @property noteRepository note storage management.
 * @property noteToolPreferences to manage note filter and search.
 * @property preferences project's [SharedPreferences].
 */
@ExperimentalCoroutinesApi
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val noteToolPreferences: NoteToolPreferences,
    private val preferences: SharedPreferences
) : ViewModel() {

    private companion object {
        private const val PAGE_SIZE = 20

        private const val NAVIGATION_CHANNEL_CAPACITY = 1
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED

        private const val KEY_DOES_FILTER_HAVE_DATE_FROM = "notes.does_filter_have_date_from"
        private const val KEY_FILTER_DATE_FROM_INCLUSIVE = "notes.filter_date_from_inclusive"
        private const val KEY_DOES_FILTER_HAVE_DATE_TO = "notes.does_filter_have_date_to"
        private const val KEY_FILTER_DATE_TO_EXCLUSIVE = "notes.filter_date_to_exclusive"
        private const val KEY_DOES_FILTER_HAVE_BPM_FROM = "notes.does_filter_have_bpm_from"
        private const val KEY_FILTER_BPM_FROM_INCLUSIVE = "notes.filter_bpm_from_inclusive"
        private const val KEY_DOES_FILTER_HAVE_BPM_TO = "notes.does_filter_have_bpm_to"
        private const val KEY_FILTER_BPM_TO_INCLUSIVE = "notes.filter_bpm_to_inclusive"
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

        /**
         * To the screen that allows you to configure the notes filter.
         *
         * @property filter current filter.
         * @property isFilterEnabled whether the [filter] is currently applied.
         */
        data class ToConfigFilterScreen(
            val filter: NoteFilter,
            val isFilterEnabled: Boolean
        ) : NavDirection()
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
         * Message stating that, as a result of filter configuration, none of its parameters have
         * been defined.
         */
        object FilterUndefined : Message()

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
     * Information signal.
     */
    sealed class Signal {

        /**
         * Indicates that the filter is turned on or turned off.
         */
        object FilterEnablingChanged : Signal()

        /**
         * Indicates that at least one selected note appears or no more selected notes.
         */
        object SelectionChanged : Signal()

        /**
         * Indicates that at least one note has been changed.
         */
        object NoteChanged : Signal()
    }

    /**
     * How the filter configuration ended.
     */
    enum class ConfigFilterStatus {
        APPLY,
        DISABLE,
        CANCEL
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current state of the waiting for the new note in the UI thread.
     */
    val noteExpectationAsLiveData: LiveData<Boolean>
        get() = _noteExpectationAsLiveData

    /**
     * Subscribe to the current list of notes in the UI thread.
     */
    val notePagingAsLiveData: LiveData<PagingData<Note>>
        get() = pager.liveData

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
     * Consume information signals from this channel in the UI thread.
     */
    val signalChannel: ReceiveChannel<Signal>
        get() = _signalChannel.openSubscription()

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
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean
        get() = noteToolPreferences.isNoteSearchActive
        set(value) {
            noteToolPreferences.isNoteSearchActive = value
        }

    /**
     * The text that must be contained in the names of the displayed notes' exercises
     * (case-insensitive).
     */
    var exerciseSubname: String?
        get() = noteToolPreferences.noteExerciseSubname
        set(value) {
            noteToolPreferences.noteExerciseSubname = value
        }

    /**
     * Indicates whether the filter is enabled.
     */
    val isFilterEnabled: Boolean
        get() = noteToolPreferences.isNoteFilterEnabled

    private var filter: NoteFilter by observable(
        preferences.getNoteFilter()
    ) { _, old, new ->
        Timber.d("Change filter: old=$old, new=$new")
        if (new != old) {
            preferences.edit {
                putNoteFilter(new)
            }
            if (noteToolPreferences.isNoteFilterEnabled) {
                resetNotes()
            }
        }
    }

    private val filterOrNull: NoteFilter?
        get() = if (noteToolPreferences.isNoteFilterEnabled) filter else null

    private var noteSource: NoteSource? = null

    private val pager: Pager<Int, Note> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false
        )
    ) {
        NoteSource(
            noteRepository = noteRepository,
            exerciseSubname = exerciseSubname,
            filter = filterOrNull
        ).also { noteSource = it }
    }

    private val onSelectionActivenessChangeListener: OnActivenessChangeListener =
        object : OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                give(Signal.SelectionChanged)
            }
        }

    private val _manageableSelection: ManageableSelection = ManageableSelection()
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _noteExpectationAsLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

    private val _navigationChannel: BroadcastChannel<NavDirection> =
        BroadcastChannel(NAVIGATION_CHANNEL_CAPACITY)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    private val _signalChannel: BroadcastChannel<Signal> =
        BroadcastChannel(SIGNAL_CHANNEL_CAPACITY)

    init {
        Timber.d("Init")
        _manageableSelection.addOnActivenessChangeListener(onSelectionActivenessChangeListener)
        setState(State.Waiting)
        viewModelScope.launch {
            val noteInitialCount = noteRepository.countSuspending(exerciseSubname, filterOrNull)
            Timber.d("noteInitialCount=$noteInitialCount")
            _manageableSelection.init(noteInitialCount)
            setState(State.Working)
            // Ignore initial value
            noteRepository.countFlow.drop(1).collect { noteCount ->
                Timber.d("Note list changed: noteCount=$noteCount")
                resetNotes()
            }
        }
        viewModelScope.launch {
            noteToolPreferences.noteToolSignalChannel.consumeEach(::process)
        }
    }

    /**
     * Start adding a new note.
     */
    fun addNote() {
        Timber.d("Add note")
        setNoteExpectation(true)
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
                report(Message.Deletion(deletionCount))
            } catch (e: Exception) {
                Timber.e(e)
                reportAbout(e)
            }
            setState(State.Working)
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
        } else {
            setNoteExpectation(false)
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
            give(Signal.NoteChanged)
            report(Message.Update)
        }
    }

    /**
     * Request the configuration screen of the current notes filter.
     */
    fun requestFilter() {
        Timber.d("Filter requested")
        navigateTo(
            NavDirection.ToConfigFilterScreen(
                filter,
                noteToolPreferences.isNoteFilterEnabled
            )
        )
    }

    /**
     * Call this method when the results of the filter configuration are available.
     *
     * @param status how the [filter] configuration ended.
     * @param filter notes filter after configuration or null to use current filter.
     */
    fun onConfigFilterResult(status: ConfigFilterStatus, filter: NoteFilter? = null) {
        Timber.d("onConfigFilterResult: status=$status, filter=$filter")
        when (status) {
            ConfigFilterStatus.APPLY -> filter?.let { safeFilter ->
                this.filter = safeFilter
                val isFilterDefined = safeFilter.isDefined
                Timber.d("isFilterDefined=$isFilterDefined")
                noteToolPreferences.isNoteFilterEnabled = isFilterDefined
                if (isFilterDefined.not()) {
                    report(Message.FilterUndefined)
                }
            }
            ConfigFilterStatus.DISABLE -> {
                noteToolPreferences.isNoteFilterEnabled = false
                filter?.let { this.filter = it }
            }
            ConfigFilterStatus.CANCEL -> Unit
        }
    }

    private fun resetNotes() {
        setState(State.Waiting)
        viewModelScope.launch {
            val noteCount = noteRepository.countSuspending(
                exerciseSubname,
                filterOrNull
            )
            _manageableSelection.reset(noteCount)
            noteSource?.invalidate()
            setState(State.Working)
        }
    }

    private suspend fun deleteSelectedNotes(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedNoteIdList = selectionState.selectedItemIdSet.toList()
            noteRepository.deleteSuspending(
                selectedNoteIdList,
                exerciseSubname,
                filterOrNull
            )
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedNoteIdList = selectionState.unselectedItemIdSet.toList()
            noteRepository.deleteOtherSuspending(
                unselectedNoteIdList,
                exerciseSubname,
                filterOrNull
            )
        }
        ManageableSelection.State.Inactive -> 0
    }

    private fun process(signal: NoteToolPreferences.Signal) = when (signal) {
        NoteToolPreferences.Signal.NoteSearchActivenessChanged ->
            Unit
        NoteToolPreferences.Signal.NoteExerciseSubnameChanged ->
            resetNotes()
        NoteToolPreferences.Signal.NoteFilterEnablementChanged -> {
            resetNotes()
            give(Signal.FilterEnablingChanged)
        }
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

    private fun setNoteExpectation(isWait: Boolean) {
        Timber.d("Set note expectation: isWait=$isWait")
        _noteExpectationAsLiveData.postValue(isWait)
    }

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.offer(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
    }

    private fun give(signal: Signal) {
        Timber.d("Give: signal=$signal")
        _signalChannel.offer(signal)
    }

    private fun SharedPreferences.getNoteFilter(): NoteFilter {
        val defaultDays = Days()
        val dateFromInclusive = getNullableDays(
            key = KEY_FILTER_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_DATE_FROM,
            defaultValue = defaultDays
        )
        val dateToExclusive = getNullableDays(
            key = KEY_FILTER_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_DATE_TO,
            defaultValue = defaultDays
        )
        val bpmFromInclusive = getNullableInt(
            key = KEY_FILTER_BPM_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_BPM_FROM,
            defaultValue = NOTE_BPM_MIN
        )
        val bpmToInclusive = getNullableInt(
            key = KEY_FILTER_BPM_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_BPM_TO,
            defaultValue = NOTE_BPM_MAX
        )
        return NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive,
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
    }

    private fun SharedPreferences.Editor.putNoteFilter(filter: NoteFilter) {
        putNullableDays(
            key = KEY_FILTER_DATE_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_DATE_FROM,
            value = filter.dateFromInclusive
        )
        putNullableDays(
            key = KEY_FILTER_DATE_TO_EXCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_DATE_TO,
            value = filter.dateToExclusive
        )
        putNullableInt(
            key = KEY_FILTER_BPM_FROM_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_BPM_FROM,
            value = filter.bpmFromInclusive
        )
        putNullableInt(
            key = KEY_FILTER_BPM_TO_INCLUSIVE,
            presenceAttributeKey = KEY_DOES_FILTER_HAVE_BPM_TO,
            value = filter.bpmToInclusive
        )
    }

    private class NoteSource(
        private val noteRepository: NoteRepository,
        private val exerciseSubname: String?,
        private val filter: NoteFilter?
    ) : PagingSource<Int, Note>() {

        override suspend fun load(
            params: LoadParams<Int>
        ): LoadResult<Int, Note> = withContext(Dispatchers.IO) {
            val key = params.key ?: 0
            val (noteList, startPosition) = if (params.loadSize != PAGE_SIZE) {
                // Load initial
                noteRepository.page(
                    size = params.loadSize,
                    requestedStartPosition = key,
                    exerciseSubname = exerciseSubname,
                    filter = filter
                )
            } else {
                val noteList = noteRepository.list(
                    size = params.loadSize,
                    startPosition = key,
                    exerciseSubname = exerciseSubname,
                    filter = filter
                )
                Page(noteList, key)
            }
            val probableKey = if (noteList.size == params.loadSize) {
                startPosition + noteList.size
            } else {
                null
            }
            return@withContext LoadResult.Page(
                data = noteList,
                prevKey = null,
                nextKey = if (probableKey != key) probableKey else null
            )
        }

        override fun getRefreshKey(state: PagingState<Int, Note>): Int? {
            return null
        }
    }
}
