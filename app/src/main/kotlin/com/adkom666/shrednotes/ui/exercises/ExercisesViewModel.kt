package com.adkom666.shrednotes.ui.exercises

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
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.pref.ExerciseToolPreferences
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.paging.Page
import com.adkom666.shrednotes.util.selection.ManageableSelection
import com.adkom666.shrednotes.util.selection.OnActivenessChangeListener
import com.adkom666.shrednotes.util.selection.SelectableItems
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.selection.SelectionDashboard
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

/**
 * Exercises section model.
 *
 * @property exerciseRepository exercise storage management.
 * @property noteRepository note storage management.
 * @property exerciseToolPreferences to manage exercise search.
 */
@ExperimentalCoroutinesApi
class ExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val noteRepository: NoteRepository,
    private val exerciseToolPreferences: ExerciseToolPreferences
) : ViewModel() {

    private companion object {
        private const val PAGE_SIZE = 20

        private const val NAVIGATION_CHANNEL_CAPACITY = 1
        private const val MESSAGE_CHANNEL_CAPACITY = Channel.BUFFERED
        private const val SIGNAL_CHANNEL_CAPACITY = Channel.BUFFERED
    }

    /**
     * Exercise list state.
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
         * To the screen that allows you to add a new exercise.
         */
        object ToAddExerciseScreen : NavDirection()

        /**
         * To the screen that allows you to update an existing exercise.
         *
         * @property exercise [Exercise] to update.
         */
        data class ToUpdateExerciseScreen(val exercise: Exercise) : NavDirection()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about adding an exercise.
         */
        object Addition : Message()

        /**
         * Message about the exercise update.
         */
        object Update : Message()

        /**
         * Message about the count of notes associated with the selected exercises.
         *
         * @property noteCount count of notes associated with the selected exercises.
         */
        data class AssociatedNoteCount(val noteCount: Int) : Message()

        /**
         * Message about the count of deleted exercises.
         *
         * @property count count of deleted exercises.
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
     * Information signal.
     */
    sealed class Signal {

        /**
         * Indicates that at least one selected note appears or no more selected notes.
         */
        object SelectionChanged : Signal()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current state of the waiting for the new exercise in the UI thread.
     */
    val exerciseExpectationAsLiveData: LiveData<Boolean>
        get() = _exerciseExpectationAsLiveData

    /**
     * Subscribe to the current list of exercises in the UI thread.
     */
    val exercisePagingAsLiveData: LiveData<PagingData<Exercise>>
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
     * Exercises to select.
     */
    val selectableExercises: SelectableItems
        get() = _manageableSelection

    /**
     * Tools for manipulating the selection.
     */
    val selectionDashboard: SelectionDashboard
        get() = _manageableSelection

    /**
     * Information about the presence of selected exercises.
     */
    val selection: Selection
        get() = _manageableSelection

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean
        get() = exerciseToolPreferences.isExcerciseSearchActive
        set(value) {
            exerciseToolPreferences.isExcerciseSearchActive = value
        }

    /**
     * The text that must be contained in the names of the displayed exercises (case-insensitive).
     */
    var subname: String?
        get() = exerciseToolPreferences.exerciseSubname
        set(value) {
            exerciseToolPreferences.exerciseSubname = value
        }

    private var exerciseSource: ExerciseSource? = null

    private val pager: Pager<Int, Exercise> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false
        )
    ) {
        ExerciseSource(
            exerciseRepository = exerciseRepository,
            subname = subname
        ).also { exerciseSource = it }
    }

    private val onSelectionActivenessChangeListener: OnActivenessChangeListener =
        object : OnActivenessChangeListener {
            override fun onActivenessChange(isActive: Boolean) {
                give(Signal.SelectionChanged)
            }
        }

    private val _manageableSelection: ManageableSelection = ManageableSelection()
    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)
    private val _exerciseExpectationAsLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

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
            val exerciseInitialCount = exerciseRepository.countSuspending(subname)
            Timber.d("exerciseInitialCount=$exerciseInitialCount")
            _manageableSelection.init(exerciseInitialCount)
            setState(State.Working)
            // Ignore initial value
            exerciseRepository.countFlow.drop(1).collect { exerciseCount ->
                Timber.d("Exercise list changed: exerciseCount=$exerciseCount")
                resetExercises()
            }
        }
        viewModelScope.launch {
            exerciseToolPreferences.exerciseToolSignalChannel.consumeEach(::process)
        }
    }

    /**
     * Start adding a new exercise.
     */
    fun addExercise() {
        Timber.d("Add exercise")
        setExerciseExpectation(true)
        navigateTo(NavDirection.ToAddExerciseScreen)
    }

    /**
     * Request to generate the message [Message.AssociatedNoteCount].
     */
    fun requestAssociatedNoteCount() {
        Timber.d("Request associated notes count")
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val noteCount = requestAssociatedNoteCount(_manageableSelection.state)
                report(Message.AssociatedNoteCount(noteCount))
            } catch (e: Exception) {
                Timber.e(e)
                reportAbout(e)
            }
            setState(State.Working)
        }
    }

    /**
     * Initiate the deletion of the selected exercises.
     */
    fun deleteSelectedExercises() {
        Timber.d("Delete selected exercises")
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val deletionCount = deleteSelectedExercises(_manageableSelection.state)
                report(Message.Deletion(deletionCount))
            } catch (e: Exception) {
                Timber.e(e)
                reportAbout(e)
            }
            setState(State.Working)
        }
    }

    /**
     * Handle the clicked exercise when the selection is inactive.
     */
    fun onExerciseClick(exercise: Exercise) {
        Timber.d("onExerciseClick: exercise=$exercise")
        navigateTo(NavDirection.ToUpdateExerciseScreen(exercise))
    }

    /**
     * Call this method when the results of adding the exercise are available.
     *
     * @param isResultOk true if the exercise was added, false otherwise.
     */
    fun onAddExerciseResult(isResultOk: Boolean) {
        Timber.d("onAddExerciseResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Exercise has been added")
            report(Message.Addition)
        } else {
            setExerciseExpectation(false)
        }
    }

    /**
     * Call this method when the results of the exercise update are available.
     *
     * @param isResultOk true if the exercise was updated, false otherwise.
     */
    fun onUpdateExerciseResult(isResultOk: Boolean) {
        Timber.d("onUpdateExerciseResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Exercise has been updated")
            report(Message.Update)
        }
    }

    private fun resetExercises() {
        setState(State.Waiting)
        viewModelScope.launch {
            val exerciseCount = exerciseRepository.countSuspending(subname)
            _manageableSelection.reset(exerciseCount)
            exerciseSource?.invalidate()
            setState(State.Working)
        }
    }

    private suspend fun requestAssociatedNoteCount(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedExerciseIdList = selectionState.selectedItemIdSet.toList()
            noteRepository.countByExerciseIdsSuspending(selectedExerciseIdList)
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedExerciseIdList = selectionState.unselectedItemIdSet.toList()
            noteRepository.countOtherByExerciseIdsSuspending(unselectedExerciseIdList)
        }
        ManageableSelection.State.Inactive -> 0
    }

    private suspend fun deleteSelectedExercises(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedExerciseIdList = selectionState.selectedItemIdSet.toList()
            exerciseRepository.deleteSuspending(selectedExerciseIdList, subname)
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedExerciseIdList = selectionState.unselectedItemIdSet.toList()
            exerciseRepository.deleteOtherSuspending(unselectedExerciseIdList, subname)
        }
        ManageableSelection.State.Inactive -> 0
    }

    private fun process(signal: ExerciseToolPreferences.Signal) = when (signal) {
        ExerciseToolPreferences.Signal.ExcerciseSearchActivenessChanged ->
            Unit
        ExerciseToolPreferences.Signal.ExerciseSubnameChanged ->
            resetExercises()
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

    private fun setExerciseExpectation(isWait: Boolean) {
        Timber.d("Set exercise expectation: isWait=$isWait")
        _exerciseExpectationAsLiveData.postValue(isWait)
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

    private class ExerciseSource(
        private val exerciseRepository: ExerciseRepository,
        private val subname: String?
    ) : PagingSource<Int, Exercise>() {

        override suspend fun load(
            params: LoadParams<Int>
        ): LoadResult<Int, Exercise> = withContext(Dispatchers.IO) {
            val key = params.key ?: 0
            val (exerciseList, startPosition) = if (params.loadSize != PAGE_SIZE) {
                // Load initial
                exerciseRepository.page(
                    size = params.loadSize,
                    requestedStartPosition = key,
                    subname = subname
                )
            } else {
                val exerciseList = exerciseRepository.list(
                    size = params.loadSize,
                    startPosition = key,
                    subname = subname
                )
                Page(exerciseList, key)
            }
            val probableKey = if (exerciseList.size == params.loadSize) {
                startPosition + exerciseList.size
            } else {
                null
            }
            return@withContext LoadResult.Page(
                data = exerciseList,
                prevKey = null,
                nextKey = if (probableKey != key) probableKey else null
            )
        }

        override fun getRefreshKey(state: PagingState<Int, Exercise>): Int? {
            return null
        }
    }
}
